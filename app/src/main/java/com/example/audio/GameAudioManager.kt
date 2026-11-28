package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import com.example.model.BattlePhase
import com.example.model.BattleState
import com.example.model.TOWER_ROOF_FLOOR
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.tanh

enum class AudioTrackMode {
    MENU_TAVERN,
    BATTLE_NORMAL,
    BATTLE_BOSS,
    BATTLE_FINAL_BOSS,
    SFX_VICTORY,
    SFX_DEFEAT,
    SILENT
}

const val CAREER_FINAL_BOSS_STAGE_ID = 406

fun isFinalBossBattle(battleState: BattleState): Boolean {
    return battleState.campaignStageId == CAREER_FINAL_BOSS_STAGE_ID ||
        (battleState.isTower && battleState.towerFloor == TOWER_ROOF_FLOOR)
}

fun resolveDesiredAudioMode(battleState: BattleState?): AudioTrackMode {
    if (battleState == null) return AudioTrackMode.MENU_TAVERN
    return when (battleState.phase) {
        BattlePhase.VICTORY -> AudioTrackMode.SFX_VICTORY
        BattlePhase.DEFEAT -> AudioTrackMode.SFX_DEFEAT
        else -> when {
            isFinalBossBattle(battleState) -> AudioTrackMode.BATTLE_FINAL_BOSS
            battleState.isBoss -> AudioTrackMode.BATTLE_BOSS
            else -> AudioTrackMode.BATTLE_NORMAL
        }
    }
}

fun isOneShotAudioMode(mode: AudioTrackMode): Boolean {
    return mode == AudioTrackMode.SFX_VICTORY || mode == AudioTrackMode.SFX_DEFEAT
}

/**
 * Retro 8-bit chiptune audio engine (NES 2A03-style Pulse, Triangle, and Stepped Noise)
 * powered by continuous phase accumulators so melodies remain crisp, melodic, and pitch-locked.
 */
object GameAudioManager {
    private const val SAMPLE_RATE = 44100
    private const val CHUNK_SAMPLES = 2048

    private val lock = Any()

    @Volatile
    private var logicalMode: AudioTrackMode = AudioTrackMode.MENU_TAVERN

    @Volatile
    private var isMutedState: Boolean = false

    @Volatile
    private var isForegroundState: Boolean = true

    @Volatile
    private var oneShotFinished: Boolean = false

    @Volatile
    private var sampleCursor: Long = 0L

    @Volatile
    private var battleNormalRotationOffset: Int = 0

    @Volatile
    private var battleNormalEntryCount: Int = 0

    @Volatile
    private var isWorkerRunning: Boolean = false

    private var workerThread: Thread? = null

    private fun isRobolectricEnvironment(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    fun getCurrentLogicalMode(): AudioTrackMode = logicalMode

    fun getBattleNormalRotationOffset(): Int = battleNormalRotationOffset

    fun isMuted(): Boolean = isMutedState

    fun isOneShotCompleted(): Boolean = oneShotFinished

    fun getEffectivePlaybackMode(): AudioTrackMode {
        if (isMutedState || !isForegroundState) return AudioTrackMode.SILENT
        val mode = logicalMode
        if (isOneShotAudioMode(mode) && oneShotFinished) return AudioTrackMode.SILENT
        return mode
    }

    fun setMuted(muted: Boolean) {
        synchronized(lock) {
            if (isMutedState == muted) return
            isMutedState = muted
            if (!muted) {
                ensureWorkerStartedIfNeeded()
            }
        }
    }

    fun setAppForeground(inForeground: Boolean) {
        synchronized(lock) {
            if (isForegroundState == inForeground) return
            isForegroundState = inForeground
            if (inForeground) {
                ensureWorkerStartedIfNeeded()
            }
        }
    }

    fun setMode(mode: AudioTrackMode) {
        synchronized(lock) {
            if (logicalMode == mode) {
                if (!isMutedState && isForegroundState && !isOneShotAudioMode(mode)) {
                    ensureWorkerStartedIfNeeded()
                }
                return
            }
            if (mode == AudioTrackMode.BATTLE_NORMAL) {
                battleNormalRotationOffset = battleNormalEntryCount % 3
                battleNormalEntryCount++
            }
            logicalMode = mode
            sampleCursor = 0L
            oneShotFinished = false
            ensureWorkerStartedIfNeeded()
        }
    }

    /** Test helper to reset internal state cleanly between unit test cases. */
    internal fun resetForTesting(muted: Boolean = false) {
        synchronized(lock) {
            logicalMode = AudioTrackMode.MENU_TAVERN
            isMutedState = muted
            isForegroundState = true
            oneShotFinished = false
            sampleCursor = 0L
            battleNormalRotationOffset = 0
            battleNormalEntryCount = 0
        }
    }

    private fun ensureWorkerStartedIfNeeded() {
        if (isRobolectricEnvironment()) return
        if (isWorkerRunning) return
        if (getEffectivePlaybackMode() == AudioTrackMode.SILENT) return

        isWorkerRunning = true
        workerThread = Thread({
            runAudioLoop()
        }, "PlumpHeroesAudioThread").apply {
            isDaemon = true
            start()
        }
    }

    private fun runAudioLoop() {
        var audioTrack: AudioTrack? = null
        val pcmBuffer = ShortArray(CHUNK_SAMPLES)
        val synth = ChiptuneAudioSynthesizer(SAMPLE_RATE)
        var lastRenderedMode = AudioTrackMode.SILENT

        try {
            val minBufBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(CHUNK_SAMPLES * 4)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBufBytes)
                .build()

            var isPlaying = false

            while (isWorkerRunning) {
                val activeMode = getEffectivePlaybackMode()
                if (activeMode == AudioTrackMode.SILENT) {
                    if (isPlaying) {
                        runCatching {
                            audioTrack.pause()
                            audioTrack.flush()
                        }
                        isPlaying = false
                        synth.reset()
                        lastRenderedMode = AudioTrackMode.SILENT
                    }
                    Thread.sleep(60L)
                    continue
                }

                if (activeMode != lastRenderedMode) {
                    synth.reset()
                    lastRenderedMode = activeMode
                    if (isPlaying) {
                        runCatching { audioTrack.flush() }
                    }
                }

                if (!isPlaying) {
                    audioTrack.play()
                    isPlaying = true
                }

                val (startCursor, normalOffset) = synchronized(lock) {
                    sampleCursor to battleNormalRotationOffset
                }
                val finished = synth.renderChunk(activeMode, startCursor, pcmBuffer, normalOffset)
                synchronized(lock) {
                    if (logicalMode == activeMode || activeMode == getEffectivePlaybackMode()) {
                        sampleCursor = startCursor + CHUNK_SAMPLES
                        if (finished && isOneShotAudioMode(activeMode)) {
                            oneShotFinished = true
                        }
                    }
                }

                val written = audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                if (written < 0) {
                    Thread.sleep(50L)
                }
            }
        } catch (_: Throwable) {
            // Audio device unavailable; degrade silently without interrupting gameplay.
        } finally {
            isWorkerRunning = false
            runCatching {
                audioTrack?.stop()
                audioTrack?.release()
            }
        }
    }
}

/**
 * Pure 8-bit chiptune synthesizer with per-channel continuous phase accumulators.
 * Models NES 2A03 hardware channels: 2 Pulse channels (lead & harmony/arp),
 * 1 4-bit quantized Triangle bass channel, and 1 stepped LFSR-style Noise channel.
 */
internal class ChiptuneAudioSynthesizer(private val sampleRate: Int) {
    companion object {
        const val MENU_STEP_SEC = 0.26
        const val MENU_STEPS = 64
        const val MENU_LOOP_SEC = MENU_STEPS * MENU_STEP_SEC // 16.64s

        const val BATTLE_NORMAL_STEP_SEC = 0.18
        const val BATTLE_NORMAL_STEPS = 64
        const val BATTLE_NORMAL_LOOP_SEC = BATTLE_NORMAL_STEPS * BATTLE_NORMAL_STEP_SEC // 11.52s
    }

    private val dt = 1.0 / sampleRate.toDouble()

    // Continuous phase accumulators (eliminates frequency-modulation pitch drift / theremin artifacts)
    private var leadPhase = 0.0
    private var harmPhase = 0.0
    private var bassPhase = 0.0
    private var sfxExtraPhase = 0.0

    // Stepped 8-bit noise generator
    private var lfsr = 0x7FFF
    private var noiseSample = 0.0
    private var noiseTickCounter = 0

    // Gentle output smoothing to tame harsh Nyquist aliasing on 8-bit square edges
    private var toneLpState = 0.0
    private var dcPrevIn = 0.0
    private var dcPrevOut = 0.0

    // Precomputed MIDI -> Hz table for crisp integer pitches
    private val midiHz = DoubleArray(128) { midi ->
        440.0 * Math.pow(2.0, (midi - 69) / 12.0)
    }

    fun reset() {
        leadPhase = 0.0
        harmPhase = 0.0
        bassPhase = 0.0
        sfxExtraPhase = 0.0
        lfsr = 0x7FFF
        noiseSample = 0.0
        noiseTickCounter = 0
        toneLpState = 0.0
        dcPrevIn = 0.0
        dcPrevOut = 0.0
    }

    /**
     * Plays Menu Theme 1 for 5 consecutive loops (0..4), then switches to Menu Theme 2
     * on the 6th loop (after 5 consecutive plays), and alternates in blocks of 5 loops thereafter.
     */
    fun resolveMenuTrackVariant(t: Double): Int {
        val loopIndex = (t / MENU_LOOP_SEC).toInt().coerceAtLeast(0)
        return (loopIndex / 5) % 2
    }

    /**
     * Periodically cycles between 3 normal battle melodies (0, 1, 2) on each full song loop.
     */
    fun resolveBattleNormalTrackVariant(t: Double, rotationOffset: Int = 0): Int {
        val loopIndex = (t / BATTLE_NORMAL_LOOP_SEC).toInt().coerceAtLeast(0)
        val safeOffset = ((rotationOffset % 3) + 3) % 3
        return (loopIndex + safeOffset) % 3
    }

    private fun advancePhase(phase: Double, hz: Double): Double {
        var next = phase + hz * dt
        if (next >= 1.0) next -= next.toInt().toDouble()
        return next
    }

    private fun pulseWave(phase: Double, duty: Double): Double {
        return if (phase < duty) 1.0 else -1.0
    }

    /** NES 4-bit quantized triangle wave (16 crisp stepped levels, iconic retro bass warmth). */
    private fun nesTriangleWave(phase: Double): Double {
        val raw = 1.0 - 4.0 * abs(phase - 0.5)
        return ((raw * 7.5).roundToInt() / 7.5)
    }

    /** Clocked 15-bit LFSR noise (authentic 8-bit snare/hat grit). */
    private fun steppedNoise(dividerSamples: Int): Double {
        noiseTickCounter++
        if (noiseTickCounter >= dividerSamples) {
            noiseTickCounter = 0
            val bit = ((lfsr shr 0) xor (lfsr shr 1)) and 1
            lfsr = (lfsr shr 1) or (bit shl 14)
            if (lfsr == 0) lfsr = 0x4A2B
            noiseSample = if ((lfsr and 1) == 1) 1.0 else -1.0
        }
        return noiseSample
    }

    /** Note sustain helper across tied/repeated notes in a step grid. */
    private fun readGridNote(
        grid: IntArray,
        stepIdx: Int,
        stepT: Double,
        stepSec: Double
    ): Triple<Int, Double, Boolean> {
        val midi = grid[stepIdx]
        if (midi <= 0 && midi != -2) return Triple(0, 0.0, false)
        // -2 marks sustain continuation of previous note
        var origin = stepIdx
        while (origin > 0 && grid[origin] == -2) {
            origin--
        }
        val activeMidi = grid[origin]
        if (activeMidi <= 0) return Triple(0, 0.0, false)

        var totalSteps = 1
        var scan = origin + 1
        while (scan < grid.size && grid[scan] == -2) {
            totalSteps++
            scan++
        }
        val elapsedInNote = (stepIdx - origin) * stepSec + stepT
        val noteDur = totalSteps * stepSec
        val isNearEnd = elapsedInNote > (noteDur - 0.018)
        return Triple(activeMidi, elapsedInNote, isNearEnd)
    }

    /** Returns true if a one-shot SFX finished within this chunk. */
    fun renderChunk(
        mode: AudioTrackMode,
        startSample: Long,
        out: ShortArray,
        battleNormalRotationOffset: Int = 0
    ): Boolean {
        var finished = false
        for (i in out.indices) {
            val s = startSample + i
            val t = s * dt
            val raw = when (mode) {
                AudioTrackMode.MENU_TAVERN -> {
                    when (resolveMenuTrackVariant(t)) {
                        0 -> renderMenuTravelOverworld(t)
                        else -> renderMenuMoonlitSanctuary(t)
                    }
                }
                AudioTrackMode.BATTLE_NORMAL -> {
                    when (resolveBattleNormalTrackVariant(t, battleNormalRotationOffset)) {
                        0 -> renderBattleQuestMarch(t)
                        1 -> renderBattleCrimsonCharge(t)
                        else -> renderBattleStarlitVanguard(t)
                    }
                }
                AudioTrackMode.BATTLE_BOSS -> renderBattleDungeonBoss(t)
                AudioTrackMode.BATTLE_FINAL_BOSS -> renderBattleFinalBoss(t)
                AudioTrackMode.SFX_VICTORY -> {
                    if (t >= 2.35) finished = true
                    renderVictoryFanfare8Bit(t)
                }
                AudioTrackMode.SFX_DEFEAT -> {
                    if (t >= 2.15) finished = true
                    renderDefeatMotif8Bit(t)
                }
                AudioTrackMode.SILENT -> 0.0
            }
            // Warm analog-console low-pass (~5.8 kHz at 44.1k) + DC blocker
            toneLpState += 0.56 * (raw - toneLpState)
            val dcFree = toneLpState - dcPrevIn + 0.996 * dcPrevOut
            dcPrevIn = toneLpState
            dcPrevOut = dcFree

            val limited = tanh(dcFree * 0.92)
            out[i] = (limited * 24500.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return finished
    }

    // =========================================================================================
    // THEME 1A (MENU): Calm & Hopeful 8-bit Hyrule Journey / Village Overworld (G Major, ~115 BPM)
    // -2 = hold previous note, -1 = rest
    // =========================================================================================
    private val menuLeadGrid = intArrayOf(
        // Bar 1 (G): B4.. D5 G5 | Bar 2 (D/F#): F#5. D5 A4..
        71, -2, 74, 79, 78, -2, 74, 69,
        // Bar 3 (Em): G4.. B4 E5 | Bar 4 (Bm): D5.. B4. -
        67, -2, 71, 76, 74, -2, 71, -1,
        // Bar 5 (C): C5.. E5 G5 | Bar 6 (G): D5.. B4 G4
        72, -2, 76, 79, 74, -2, 71, 67,
        // Bar 7 (Am): A4.. C5 E5 | Bar 8 (D7): D5.. - -
        69, -2, 72, 76, 74, -2, -2, -1,
        // Bar 9 (G): B4. D5 G5 A5 | Bar 10 (D): F#5.. D5. -
        71, -2, 74, 79, 81, -2, 78, 74,
        // Bar 11 (Em): G5.. E5 B4 | Bar 12 (C): C5. E5 G5. -
        79, -2, 76, 71, 72, -2, 76, 79,
        // Bar 13 (Am): A5. G5 F#5 E5 | Bar 14 (D): D5. A4 B4 C5
        81, 79, 78, 76, 74, -2, 69, 72,
        // Bar 15 (G): B4.. G4 D4 | Bar 16 (G->D): G4.. - -
        71, -2, 67, 62, 67, -2, -2, -1
    )

    private val menuArpNotes = intArrayOf(
        55, 59, 62, 59,  54, 57, 62, 57,  52, 55, 59, 55,  59, 62, 66, 62,
        48, 52, 55, 52,  55, 59, 62, 59,  57, 60, 64, 60,  50, 54, 57, 60,
        55, 59, 62, 59,  54, 57, 62, 57,  52, 55, 59, 55,  48, 52, 55, 52,
        57, 60, 64, 60,  50, 54, 57, 54,  55, 59, 62, 59,  55, 59, 57, 54
    )

    private val menuBassRoots = intArrayOf(
        43, 42, 40, 47, 48, 43, 45, 38,
        43, 42, 40, 48, 45, 38, 43, 43
    )

    private fun renderMenuTravelOverworld(t: Double): Double {
        val stepSec = MENU_STEP_SEC
        val totalSteps = MENU_STEPS
        val loopT = t % (totalSteps * stepSec)
        val stepIdx = (loopT / stepSec).toInt().coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec

        val (leadMidi, noteT, nearEnd) = readGridNote(menuLeadGrid, stepIdx, stepT, stepSec)
        val leadVoice = if (leadMidi > 0) {
            leadPhase = advancePhase(leadPhase, midiHz[leadMidi])
            val atk = (noteT / 0.012).coerceIn(0.0, 1.0)
            val env = atk * (0.45 + 0.55 * exp(-noteT * 2.4)) * (if (nearEnd) 0.55 else 1.0)
            pulseWave(leadPhase, 0.50) * env * 0.24
        } else 0.0

        val arpMidi = menuArpNotes[stepIdx]
        harmPhase = advancePhase(harmPhase, midiHz[arpMidi])
        val harmEnv = (stepT / 0.008).coerceIn(0.0, 1.0) * exp(-stepT * 7.2)
        val harmVoice = pulseWave(harmPhase, 0.25) * harmEnv * 0.13

        val barPairIdx = (stepIdx / 4).coerceIn(0, 15)
        val bassMidi = if (stepIdx % 4 == 2) menuBassRoots[barPairIdx] + 7 else menuBassRoots[barPairIdx]
        bassPhase = advancePhase(bassPhase, midiHz[bassMidi])
        val bassEnv = if (stepIdx % 2 == 0) exp(-stepT * 2.8) else exp(-stepT * 4.5)
        val bassVoice = nesTriangleWave(bassPhase) * bassEnv * 0.29

        val tick = if (stepIdx % 2 == 1) {
            steppedNoise(6) * exp(-stepT * 48.0) * 0.045
        } else 0.0

        return leadVoice + harmVoice + bassVoice + tick
    }

    // =========================================================================================
    // THEME 1B (MENU ALT after 5 plays): Gentle 8-bit Moonlit Sanctuary / Tavern Lute (F Major)
    // =========================================================================================
    private val menuAltLeadGrid = intArrayOf(
        // F -> C/E -> Dm -> Am
        69, -2, 72, 77, 76, -2, 72, 67,
        74, -2, 72, 69, 72, -2, -2, -1,
        // Bb -> F/A -> Gm -> C7
        70, -2, 74, 77, 76, -2, 72, 69,
        67, -2, 69, 70, 72, -2, -2, -1,
        // High gentle chorus: F -> Dm -> Bb -> C
        77, -2, 76, 74, 72, -2, 69, -1,
        74, -2, 72, 70, 69, -2, 67, -1,
        // Gm -> C -> F -> F
        70, 72, 74, 76, 77, -2, 76, 72,
        69, -2, 67, 65, 65, -2, -2, -1
    )

    private val menuAltArpNotes = intArrayOf(
        53, 57, 60, 57,  52, 55, 60, 55,  50, 53, 57, 53,  48, 53, 57, 53,
        46, 50, 53, 50,  53, 57, 60, 57,  55, 58, 62, 58,  48, 52, 55, 58,
        53, 57, 60, 57,  50, 53, 57, 53,  46, 50, 53, 50,  48, 52, 55, 52,
        55, 58, 62, 58,  48, 52, 55, 58,  53, 57, 60, 57,  53, 57, 60, 53
    )

    private val menuAltBassRoots = intArrayOf(
        41, 40, 38, 45, 46, 41, 43, 36,
        41, 38, 46, 36, 43, 36, 41, 41
    )

    private fun renderMenuMoonlitSanctuary(t: Double): Double {
        val stepSec = MENU_STEP_SEC
        val totalSteps = MENU_STEPS
        val loopT = t % (totalSteps * stepSec)
        val stepIdx = (loopT / stepSec).toInt().coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec

        val (leadMidi, noteT, nearEnd) = readGridNote(menuAltLeadGrid, stepIdx, stepT, stepSec)
        val leadVoice = if (leadMidi > 0) {
            leadPhase = advancePhase(leadPhase, midiHz[leadMidi])
            val atk = (noteT / 0.015).coerceIn(0.0, 1.0)
            val env = atk * (0.42 + 0.58 * exp(-noteT * 2.2)) * (if (nearEnd) 0.55 else 1.0)
            pulseWave(leadPhase, 0.25) * env * 0.22
        } else 0.0

        val arpMidi = menuAltArpNotes[stepIdx]
        harmPhase = advancePhase(harmPhase, midiHz[arpMidi])
        val harmEnv = (stepT / 0.010).coerceIn(0.0, 1.0) * exp(-stepT * 6.5)
        val harmVoice = pulseWave(harmPhase, 0.50) * harmEnv * 0.12

        val barPairIdx = (stepIdx / 4).coerceIn(0, 15)
        val bassMidi = if (stepIdx % 4 == 2) menuAltBassRoots[barPairIdx] + 7 else menuAltBassRoots[barPairIdx]
        bassPhase = advancePhase(bassPhase, midiHz[bassMidi])
        val bassEnv = if (stepIdx % 2 == 0) exp(-stepT * 2.5) else exp(-stepT * 4.2)
        val bassVoice = nesTriangleWave(bassPhase) * bassEnv * 0.29

        val brush = if (stepIdx % 4 == 3) {
            steppedNoise(5) * exp(-stepT * 42.0) * 0.04
        } else 0.0

        return leadVoice + harmVoice + bassVoice + brush
    }

    // =========================================================================================
    // THEME 2A (BATTLE NORMAL 1): Heroic 8-bit Adventure March (A Minor / C Major, ~166 BPM)
    // =========================================================================================
    private val battleLeadGrid = intArrayOf(
        69, -1, 69, 76, 74, 72, 71, 69,
        77, -1, 76, 74, 76, -1, 74, 71,
        72, 74, 76, 79, 77, 76, 74, 71,
        69, -1, 76, 72, 71, -2, -2, -1,
        79, -1, 76, 72, 74, 76, 77, 74,
        76, -1, 72, 69, 71, 72, 74, 71,
        72, -1, 77, 76, 74, -1, 79, 77,
        76, 74, 72, 71, 69, -2, -2, -1
    )

    private val battleBarRoots = intArrayOf(
        45, 45, 41, 43, 45, 40, 45, 40,
        48, 43, 45, 40, 41, 43, 40, 45
    )

    private val battleBarFifths = intArrayOf(
        52, 52, 48, 50, 52, 47, 52, 47,
        55, 50, 52, 47, 48, 50, 47, 52
    )

    private val battleBarThirds = intArrayOf(
        48, 48, 45, 47, 48, 44, 48, 44,
        52, 47, 48, 43, 45, 47, 44, 48
    )

    private fun renderBattleQuestMarch(t: Double): Double {
        return renderStandardBattlePattern(
            t = t,
            leadGrid = battleLeadGrid,
            roots = battleBarRoots,
            thirds = battleBarThirds,
            fifths = battleBarFifths,
            leadDuty = 0.25,
            arpDuty = 0.125
        )
    }

    // =========================================================================================
    // THEME 2B (BATTLE NORMAL 2): Energetic 8-bit Crimson Charge (E Minor / G Major, ~166 BPM)
    // =========================================================================================
    private val battleCrimsonLeadGrid = intArrayOf(
        76, -1, 79, 78, 76, 74, 71, -1,
        72, 74, 76, 79, 78, -2, 74, -1,
        76, -1, 79, 83, 81, 79, 78, 76,
        74, 72, 71, 69, 71, -2, -2, -1,
        // B-Section (G -> D -> Em -> Bm -> C -> G -> B7 -> Em)
        79, -1, 78, 76, 74, -1, 78, 79,
        76, -1, 74, 72, 71, -1, 74, 76,
        72, 74, 76, 79, 78, 76, 74, 72,
        71, 74, 78, 75, 76, -2, -2, -1
    )

    private val battleCrimsonRoots = intArrayOf(
        40, 40, 48, 50, 40, 45, 47, 47,
        43, 50, 40, 47, 48, 43, 47, 40
    )

    private val battleCrimsonThirds = intArrayOf(
        43, 43, 52, 54, 43, 48, 51, 51,
        47, 54, 43, 50, 52, 47, 51, 43
    )

    private val battleCrimsonFifths = intArrayOf(
        47, 47, 55, 57, 47, 52, 54, 54,
        50, 57, 47, 54, 55, 50, 54, 47
    )

    private fun renderBattleCrimsonCharge(t: Double): Double {
        return renderStandardBattlePattern(
            t = t,
            leadGrid = battleCrimsonLeadGrid,
            roots = battleCrimsonRoots,
            thirds = battleCrimsonThirds,
            fifths = battleCrimsonFifths,
            leadDuty = 0.50,
            arpDuty = 0.25
        )
    }

    // =========================================================================================
    // THEME 2C (BATTLE NORMAL 3): Heroic 8-bit Starlit Vanguard (C Minor / Eb Major, ~166 BPM)
    // =========================================================================================
    private val battleStarlitLeadGrid = intArrayOf(
        72, 75, 79, -1, 77, 75, 74, 72,
        70, 74, 77, -1, 75, 74, 72, 70,
        68, 72, 75, 79, 77, -1, 75, 74,
        72, 74, 75, 77, 74, -2, -2, -1,
        // B-Section (Eb -> Bb -> Cm -> Gm -> Ab -> Bb -> G7 -> Cm)
        79, -1, 77, 75, 74, 75, 77, -1,
        75, -1, 74, 72, 70, 72, 74, -1,
        68, 72, 75, 79, 77, 75, 74, 72,
        74, 72, 71, 74, 72, -2, -2, -1
    )

    private val battleStarlitRoots = intArrayOf(
        48, 48, 46, 46, 44, 46, 48, 43,
        51, 46, 48, 43, 44, 46, 43, 48
    )

    private val battleStarlitThirds = intArrayOf(
        51, 51, 50, 50, 48, 50, 51, 47,
        55, 50, 51, 46, 48, 50, 47, 51
    )

    private val battleStarlitFifths = intArrayOf(
        55, 55, 53, 53, 51, 53, 55, 50,
        58, 53, 55, 50, 51, 53, 50, 55
    )

    private fun renderBattleStarlitVanguard(t: Double): Double {
        return renderStandardBattlePattern(
            t = t,
            leadGrid = battleStarlitLeadGrid,
            roots = battleStarlitRoots,
            thirds = battleStarlitThirds,
            fifths = battleStarlitFifths,
            leadDuty = 0.25,
            arpDuty = 0.125
        )
    }

    private fun renderStandardBattlePattern(
        t: Double,
        leadGrid: IntArray,
        roots: IntArray,
        thirds: IntArray,
        fifths: IntArray,
        leadDuty: Double,
        arpDuty: Double
    ): Double {
        val stepSec = BATTLE_NORMAL_STEP_SEC
        val totalSteps = BATTLE_NORMAL_STEPS
        val loopT = t % (totalSteps * stepSec)
        val stepIdx = (loopT / stepSec).toInt().coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec
        val barChunk = (stepIdx / 4).coerceIn(0, 15)

        val (leadMidi, noteT, nearEnd) = readGridNote(leadGrid, stepIdx, stepT, stepSec)
        val leadVoice = if (leadMidi > 0) {
            leadPhase = advancePhase(leadPhase, midiHz[leadMidi])
            val env = (noteT / 0.006).coerceIn(0.0, 1.0) *
                (0.40 + 0.60 * exp(-noteT * 3.8)) *
                (if (nearEnd) 0.50 else 1.0)
            pulseWave(leadPhase, leadDuty) * env * 0.25
        } else 0.0

        val arpMidi = when (stepIdx % 4) {
            0 -> roots[barChunk] + 12
            1 -> thirds[barChunk] + 12
            2 -> fifths[barChunk] + 12
            else -> roots[barChunk] + 24
        }
        harmPhase = advancePhase(harmPhase, midiHz[arpMidi])
        val harmEnv = exp(-stepT * 10.5)
        val harmVoice = pulseWave(harmPhase, arpDuty) * harmEnv * 0.12

        val bassMidi = if (stepIdx % 2 == 0) roots[barChunk] else fifths[barChunk]
        bassPhase = advancePhase(bassPhase, midiHz[bassMidi])
        val bassEnv = exp(-stepT * 4.5)
        val bassVoice = nesTriangleWave(bassPhase) * bassEnv * 0.30

        val beatPos = stepIdx % 4
        val kick = if (beatPos == 0) {
            val kHz = 55.0 + 95.0 * exp(-stepT * 45.0)
            sfxExtraPhase = advancePhase(sfxExtraPhase, kHz)
            nesTriangleWave(sfxExtraPhase) * exp(-stepT * 22.0) * 0.16
        } else 0.0
        val snare = if (beatPos == 2 || (stepIdx % 8 == 7)) {
            steppedNoise(10) * exp(-stepT * 28.0) * 0.11
        } else if (beatPos == 1 || beatPos == 3) {
            steppedNoise(3) * exp(-stepT * 55.0) * 0.045
        } else 0.0

        return leadVoice + harmVoice + bassVoice + kick + snare
    }

    // =========================================================================================
    // THEME 3 (BOSS BATTLE): Dramatic 8-bit Dungeon Citadel Showdown (D Minor, ~176 BPM)
    // =========================================================================================
    private val bossLeadGrid = intArrayOf(
        74, -1, 77, 76, 74, -1, 73, 74,
        70, -1, 74, 72, 70, -1, 69, -1,
        67, 70, 74, 72, 70, 69, 67, 65,
        69, -1, 67, 64, 69, -2, 73, -1,
        86, -1, 84, 82, 81, -1, 79, 77,
        79, -1, 77, 76, 74, -1, 72, 70,
        74, 76, 77, 79, 81, 79, 77, 76,
        74, -1, 73, 69, 74, -2, -2, -1
    )

    private val bossRoots = intArrayOf(
        38, 38, 34, 34, 31, 31, 33, 33,
        38, 34, 36, 34, 31, 33, 38, 38
    )

    private val bossFifths = intArrayOf(
        45, 45, 41, 41, 38, 38, 40, 40,
        45, 41, 43, 41, 38, 40, 45, 45
    )

    private fun renderBattleDungeonBoss(t: Double): Double {
        val stepSec = 0.17
        val totalSteps = 64
        val loopT = t % (totalSteps * stepSec)
        val stepIdx = (loopT / stepSec).toInt().coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec
        val barChunk = (stepIdx / 4).coerceIn(0, 15)

        val (leadMidi, noteT, nearEnd) = readGridNote(bossLeadGrid, stepIdx, stepT, stepSec)
        val leadVoice = if (leadMidi > 0) {
            leadPhase = advancePhase(leadPhase, midiHz[leadMidi])
            val env = (noteT / 0.005).coerceIn(0.0, 1.0) *
                (0.45 + 0.55 * exp(-noteT * 3.2)) *
                (if (nearEnd) 0.55 else 1.0)
            pulseWave(leadPhase, 0.25) * env * 0.25
        } else 0.0

        val subIdx = ((stepT / (stepSec * 0.5)).toInt()).coerceIn(0, 1)
        val subT = stepT - subIdx * (stepSec * 0.5)
        val root = bossRoots[barChunk] + 12
        val arpMidi = when ((stepIdx * 2 + subIdx) % 4) {
            0 -> root
            1 -> root + 3
            2 -> root + 7
            else -> root + 12
        }
        harmPhase = advancePhase(harmPhase, midiHz[arpMidi])
        val harmVoice = pulseWave(harmPhase, 0.50) * exp(-subT * 14.0) * 0.12

        val bassMidi = if ((stepIdx * 2 + subIdx) % 2 == 0) bossRoots[barChunk] else bossFifths[barChunk]
        bassPhase = advancePhase(bassPhase, midiHz[bassMidi])
        val bassVoice = nesTriangleWave(bassPhase) * exp(-subT * 5.0) * 0.30

        val beatPos = stepIdx % 4
        val kick = if (beatPos == 0 || beatPos == 3) {
            val kHz = 48.0 + 105.0 * exp(-stepT * 52.0)
            sfxExtraPhase = advancePhase(sfxExtraPhase, kHz)
            nesTriangleWave(sfxExtraPhase) * exp(-stepT * 24.0) * 0.18
        } else 0.0
        val snare = if (beatPos == 2) {
            steppedNoise(12) * exp(-stepT * 24.0) * 0.13
        } else {
            steppedNoise(4) * exp(-stepT * 60.0) * 0.045
        }

        return leadVoice + harmVoice + bassVoice + kick + snare
    }

    // =========================================================================================
    // THEME 4 (FINAL BOSS - Career 4-6 & Tower Roof): Epic 8-bit Apocalypse Showdown (F# Minor, ~200 BPM)
    // =========================================================================================
    private val finalBossLeadGrid = intArrayOf(
        // Urgent chromatic F#m -> D -> Bm -> C#7 climax theme
        78, -1, 81, 80, 78, 76, 74, 73,
        74, -1, 78, 76, 74, 73, 71, -1,
        73, 74, 76, 78, 80, 81, 83, 85,
        84, -2, 80, 77, 73, -2, -2, -1,
        // High soaring heroic counter-melody
        90, -1, 88, 86, 85, -1, 83, 81,
        83, -1, 81, 80, 78, -1, 76, 74,
        78, 80, 81, 83, 85, 86, 88, 90,
        89, -1, 85, 80, 78, -2, -2, -1
    )

    private val finalBossRoots = intArrayOf(
        42, 42, 38, 38, 35, 35, 37, 37,
        42, 38, 40, 38, 35, 37, 42, 42
    )

    private val finalBossFifths = intArrayOf(
        49, 49, 45, 45, 42, 42, 44, 44,
        49, 45, 47, 45, 42, 44, 49, 49
    )

    private fun renderBattleFinalBoss(t: Double): Double {
        val stepSec = 0.15
        val totalSteps = 64
        val loopT = t % (totalSteps * stepSec)
        val stepIdx = (loopT / stepSec).toInt().coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec
        val barChunk = (stepIdx / 4).coerceIn(0, 15)

        // 1. Fierce dual-duty 12.5% pulse lead for high-stakes final boss intensity
        val (leadMidi, noteT, nearEnd) = readGridNote(finalBossLeadGrid, stepIdx, stepT, stepSec)
        val leadVoice = if (leadMidi > 0) {
            leadPhase = advancePhase(leadPhase, midiHz[leadMidi])
            val env = (noteT / 0.004).coerceIn(0.0, 1.0) *
                (0.48 + 0.52 * exp(-noteT * 3.0)) *
                (if (nearEnd) 0.55 else 1.0)
            pulseWave(leadPhase, 0.125) * env * 0.25
        } else 0.0

        // 2. Relentless 16th baroque harmonic arpeggios
        val subIdx = ((stepT / (stepSec * 0.5)).toInt()).coerceIn(0, 1)
        val subT = stepT - subIdx * (stepSec * 0.5)
        val root = finalBossRoots[barChunk] + 12
        val arpMidi = when ((stepIdx * 2 + subIdx) % 4) {
            0 -> root
            1 -> root + 3
            2 -> root + 7
            else -> root + 12
        }
        harmPhase = advancePhase(harmPhase, midiHz[arpMidi])
        val harmVoice = pulseWave(harmPhase, 0.25) * exp(-subT * 13.0) * 0.13

        // 3. Driving octave-hammering NES triangle bass
        val bassMidi = if ((stepIdx * 2 + subIdx) % 2 == 0) finalBossRoots[barChunk] else finalBossFifths[barChunk]
        bassPhase = advancePhase(bassPhase, midiHz[bassMidi])
        val bassVoice = nesTriangleWave(bassPhase) * exp(-subT * 4.5) * 0.31

        // 4. Heavy double-kick and dramatic snare
        val beatPos = stepIdx % 4
        val kick = if (beatPos == 0 || beatPos == 2 || (stepIdx % 8 == 7)) {
            val kHz = 45.0 + 115.0 * exp(-stepT * 55.0)
            sfxExtraPhase = advancePhase(sfxExtraPhase, kHz)
            nesTriangleWave(sfxExtraPhase) * exp(-stepT * 22.0) * 0.18
        } else 0.0
        val snare = if (beatPos == 1 || beatPos == 3) {
            steppedNoise(11) * exp(-stepT * 24.0) * 0.13
        } else {
            steppedNoise(4) * exp(-stepT * 58.0) * 0.045
        }

        return leadVoice + harmVoice + bassVoice + kick + snare
    }

    // =========================================================================================
    // SOUND 1: Classic 8-bit Hero Victory Fanfare (~2.3s one-shot)
    // =========================================================================================
    private fun renderVictoryFanfare8Bit(t: Double): Double {
        if (t > 2.30) return 0.0

        val stepNote = when {
            t < 0.13 -> 72 // C5
            t < 0.26 -> 76 // E5
            t < 0.39 -> 79 // G5
            t < 0.72 -> 84 // C6
            t < 0.88 -> 79 // G5
            else -> 84     // C6 hold
        }
        val noteStart = when {
            t < 0.13 -> 0.0
            t < 0.26 -> 0.13
            t < 0.39 -> 0.26
            t < 0.72 -> 0.39
            t < 0.88 -> 0.72
            else -> 0.88
        }
        val noteElapsed = t - noteStart

        leadPhase = advancePhase(leadPhase, midiHz[stepNote])
        val leadEnv = if (t < 0.88) {
            exp(-noteElapsed * 4.5)
        } else {
            exp(-noteElapsed * 2.1)
        }
        val lead = pulseWave(leadPhase, 0.25) * leadEnv * 0.28

        val harmNote = if (t < 0.88) stepNote - 12 else 76
        harmPhase = advancePhase(harmPhase, midiHz[harmNote])
        val harm = pulseWave(harmPhase, 0.50) * leadEnv * 0.15

        val bassNote = if (t < 0.39) 48 else if (t < 0.88) 55 else 48
        bassPhase = advancePhase(bassPhase, midiHz[bassNote])
        val bass = nesTriangleWave(bassPhase) * leadEnv * 0.24

        val snareRoll = if (t < 0.38) steppedNoise(9) * 0.07 else if (t in 0.88..1.25) {
            steppedNoise(10) * exp(-(t - 0.88) * 10.0) * 0.11
        } else 0.0

        val tailFade = if (t > 2.05) ((2.30 - t) / 0.25).coerceIn(0.0, 1.0) else 1.0
        return (lead + harm + bass + snareRoll) * tailFade
    }

    // =========================================================================================
    // SOUND 2: Classic 8-bit Defeat / Game Over Motif (~2.1s one-shot)
    // =========================================================================================
    private fun renderDefeatMotif8Bit(t: Double): Double {
        if (t > 2.10) return 0.0

        val (noteMidi, noteElapsed) = when {
            t < 0.30 -> 67 to t
            t < 0.60 -> 66 to (t - 0.30)
            t < 0.90 -> 65 to (t - 0.60)
            else -> 64 to (t - 0.90)
        }
        val hz = if (t >= 0.90) {
            val sag = (1.0 - 0.06 * ((t - 0.90) / 1.20)).coerceIn(0.90, 1.0)
            midiHz[noteMidi] * sag
        } else midiHz[noteMidi]

        leadPhase = advancePhase(leadPhase, hz)
        val env = exp(-noteElapsed * if (t >= 0.90) 2.2 else 4.2)
        val lead = pulseWave(leadPhase, 0.50) * env * 0.28

        bassPhase = advancePhase(bassPhase, hz * 0.5)
        val bass = nesTriangleWave(bassPhase) * env * 0.24

        val tailFade = if (t > 1.85) ((2.10 - t) / 0.25).coerceIn(0.0, 1.0) else 1.0
        return (lead + bass) * tailFade
    }
}
