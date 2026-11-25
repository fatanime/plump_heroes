package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import com.example.model.BattlePhase
import com.example.model.BattleState
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.tanh

enum class AudioTrackMode {
    MENU_TAVERN,
    BATTLE_NORMAL,
    BATTLE_BOSS,
    SFX_VICTORY,
    SFX_DEFEAT,
    SILENT
}

fun resolveDesiredAudioMode(battleState: BattleState?): AudioTrackMode {
    if (battleState == null) return AudioTrackMode.MENU_TAVERN
    return when (battleState.phase) {
        BattlePhase.VICTORY -> AudioTrackMode.SFX_VICTORY
        BattlePhase.DEFEAT -> AudioTrackMode.SFX_DEFEAT
        else -> if (battleState.isBoss) AudioTrackMode.BATTLE_BOSS else AudioTrackMode.BATTLE_NORMAL
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
    private var isWorkerRunning: Boolean = false

    private var workerThread: Thread? = null

    private fun isRobolectricEnvironment(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    fun getCurrentLogicalMode(): AudioTrackMode = logicalMode

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

                val startCursor = synchronized(lock) { sampleCursor }
                val finished = synth.renderChunk(activeMode, startCursor, pcmBuffer)
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
        if (midi <= 0) return Triple(0, 0.0, false)
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
    fun renderChunk(mode: AudioTrackMode, startSample: Long, out: ShortArray): Boolean {
        var finished = false
        for (i in out.indices) {
            val s = startSample + i
            val t = s * dt
            val raw = when (mode) {
                AudioTrackMode.MENU_TAVERN -> renderMenuTravelOverworld(t)
                AudioTrackMode.BATTLE_NORMAL -> renderBattleQuestMarch(t)
                AudioTrackMode.BATTLE_BOSS -> renderBattleDungeonBoss(t)
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
    // THEME 1 (MENU): Calm & Hopeful 8-bit Hyrule Journey / Village Overworld (G Major, ~115 BPM)
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
        // 16 groups of 4-step gentle broken chord notes (1 note per step)
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
        val stepSec = 0.26
        val totalSteps = 64
        val loopT = t % (totalSteps * stepSec)
        val stepIdx = (loopT / stepSec).toInt().coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec

        // 1. Lead: warm 50% hollow NES square (calm Zelda village/camp travel flute tone)
        val (leadMidi, noteT, nearEnd) = readGridNote(menuLeadGrid, stepIdx, stepT, stepSec)
        val leadVoice = if (leadMidi > 0) {
            leadPhase = advancePhase(leadPhase, midiHz[leadMidi])
            val atk = (noteT / 0.012).coerceIn(0.0, 1.0)
            val env = atk * (0.45 + 0.55 * exp(-noteT * 2.4)) * (if (nearEnd) 0.55 else 1.0)
            pulseWave(leadPhase, 0.50) * env * 0.24
        } else 0.0

        // 2. Harmony: soft 25% pulse broken travel chords
        val arpMidi = menuArpNotes[stepIdx]
        harmPhase = advancePhase(harmPhase, midiHz[arpMidi])
        val harmEnv = (stepT / 0.008).coerceIn(0.0, 1.0) * exp(-stepT * 7.2)
        val harmVoice = pulseWave(harmPhase, 0.25) * harmEnv * 0.13

        // 3. Bass: clean NES triangle (half-bar root -> fifth walking travel bass)
        val barPairIdx = (stepIdx / 4).coerceIn(0, 15)
        val bassMidi = if (stepIdx % 4 == 2) menuBassRoots[barPairIdx] + 7 else menuBassRoots[barPairIdx]
        bassPhase = advancePhase(bassPhase, midiHz[bassMidi])
        val bassEnv = if (stepIdx % 2 == 0) exp(-stepT * 2.8) else exp(-stepT * 4.5)
        val bassVoice = nesTriangleWave(bassPhase) * bassEnv * 0.29

        // 4. Light retro travel shaker on offbeats
        val tick = if (stepIdx % 2 == 1) {
            steppedNoise(6) * exp(-stepT * 48.0) * 0.045
        } else 0.0

        return leadVoice + harmVoice + bassVoice + tick
    }

    // =========================================================================================
    // THEME 2 (BATTLE): Heroic 8-bit Zelda Overworld Adventure March (A Minor / C Major, ~166 BPM)
    // =========================================================================================
    private val battleLeadGrid = intArrayOf(
        // Bar 1 (Am): A4 - A4 E5 | Bar 2: D5 C5 B4 A4
        69, -1, 69, 76, 74, 72, 71, 69,
        // Bar 3 (F): F5 - E5 D5 | Bar 4 (G): E5 - D5 B4
        77, -1, 76, 74, 76, -1, 74, 71,
        // Bar 5 (Am): C5 D5 E5 G5 | Bar 6 (E7): F5 E5 D5 B4
        72, 74, 76, 79, 77, 76, 74, 71,
        // Bar 7 (Am): A4 - E5 C5 | Bar 8 (E): B4 - - -
        69, -1, 76, 72, 71, -2, -2, -1,
        // B-Section (C -> G -> Am -> Em -> F -> G -> E7 -> Am heroic adventure theme)
        // Bar 9 (C): G5 - E5 C5 | Bar 10 (G): D5 E5 F5 D5
        79, -1, 76, 72, 74, 76, 77, 74,
        // Bar 11 (Am): E5 - C5 A4 | Bar 12 (Em): B4 C5 D5 B4
        76, -1, 72, 69, 71, 72, 74, 71,
        // Bar 13 (F): C5 - F5 E5 | Bar 14 (G): D5 - G5 F5
        72, -1, 77, 76, 74, -1, 79, 77,
        // Bar 15 (E7): E5 D5 C5 B4 | Bar 16 (Am): A4 - - -
        76, 74, 72, 71, 69, -2, -2, -1
    )

    private val battleBarRoots = intArrayOf(
        // 16 four-step half-bar chunks
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
        val stepSec = 0.18
        val totalSteps = 64
        val loopT = t % (totalSteps * stepSec)
        val stepIdx = (loopT / stepSec).toInt().coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec
        val barChunk = (stepIdx / 4).coerceIn(0, 15)

        // 1. Heroic 25% pulse lead (bright classic Zelda NES adventure fanfare tone)
        val (leadMidi, noteT, nearEnd) = readGridNote(battleLeadGrid, stepIdx, stepT, stepSec)
        val leadVoice = if (leadMidi > 0) {
            leadPhase = advancePhase(leadPhase, midiHz[leadMidi])
            val env = (noteT / 0.006).coerceIn(0.0, 1.0) *
                (0.40 + 0.60 * exp(-noteT * 3.8)) *
                (if (nearEnd) 0.50 else 1.0)
            pulseWave(leadPhase, 0.25) * env * 0.25
        } else 0.0

        // 2. Driving 12.5% pulse chord arpeggio (root-third-fifth-octave bounce)
        val arpMidi = when (stepIdx % 4) {
            0 -> battleBarRoots[barChunk] + 12
            1 -> battleBarThirds[barChunk] + 12
            2 -> battleBarFifths[barChunk] + 12
            else -> battleBarRoots[barChunk] + 24
        }
        harmPhase = advancePhase(harmPhase, midiHz[arpMidi])
        val harmEnv = exp(-stepT * 10.5)
        val harmVoice = pulseWave(harmPhase, 0.125) * harmEnv * 0.12

        // 3. Punchy NES triangle gallop bass (root on 0,2; fifth on 1,3)
        val bassMidi = if (stepIdx % 2 == 0) battleBarRoots[barChunk] else battleBarFifths[barChunk]
        bassPhase = advancePhase(bassPhase, midiHz[bassMidi])
        val bassEnv = exp(-stepT * 4.5)
        val bassVoice = nesTriangleWave(bassPhase) * bassEnv * 0.30

        // 4. Crisp NES percussion (triangle kick pitch-drop on beat 0; noise snare on beat 2; hat on 1,3)
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
        // Urgent 8-bit castle theme: Dm -> Bb -> Gm -> A7
        74, -1, 77, 76, 74, -1, 73, 74,
        70, -1, 74, 72, 70, -1, 69, -1,
        67, 70, 74, 72, 70, 69, 67, 65,
        69, -1, 67, 64, 69, -2, 73, -1,
        // High climax octave phrase
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

        // 1. Intense 25% pulse dark-castle lead
        val (leadMidi, noteT, nearEnd) = readGridNote(bossLeadGrid, stepIdx, stepT, stepSec)
        val leadVoice = if (leadMidi > 0) {
            leadPhase = advancePhase(leadPhase, midiHz[leadMidi])
            val env = (noteT / 0.005).coerceIn(0.0, 1.0) *
                (0.45 + 0.55 * exp(-noteT * 3.2)) *
                (if (nearEnd) 0.55 else 1.0)
            pulseWave(leadPhase, 0.25) * env * 0.25
        } else 0.0

        // 2. Rapid 16th-note baroque 8-bit arpeggio (50% pulse)
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

        // 3. Relentless octave-jumping NES triangle bass
        val bassMidi = if ((stepIdx * 2 + subIdx) % 2 == 0) bossRoots[barChunk] else bossFifths[barChunk]
        bassPhase = advancePhase(bassPhase, midiHz[bassMidi])
        val bassVoice = nesTriangleWave(bassPhase) * exp(-subT * 5.0) * 0.30

        // 4. Driving boss snare & kick
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

        val harmNote = if (t < 0.88) stepNote - 12 else 76 // E5 harmony on final chord
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
            t < 0.30 -> 67 to t                 // G4
            t < 0.60 -> 66 to (t - 0.30)        // F#4
            t < 0.90 -> 65 to (t - 0.60)        // F4
            else -> 64 to (t - 0.90)            // E4
        }
        val hz = if (t >= 0.90) {
            // Smooth continuous-phase retro pitch sag on final note
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
