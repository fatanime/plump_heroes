package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import com.example.model.BattlePhase
import com.example.model.BattleState
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
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
 * Self-contained procedural game music and SFX engine backed by Android [AudioTrack].
 * Synthesizes 3 looping themes (calm medieval tavern, battle, serious boss battle)
 * and 2 one-shot outcome sounds (victory fanfare, defeat fail) with zero extra APK bloat.
 */
object GameAudioManager {
    private const val SAMPLE_RATE = 22050
    private const val CHUNK_SAMPLES = 1024

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
        val synth = FantasyAudioSynthesizer(SAMPLE_RATE)
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
                        synth.resetFilters()
                        lastRenderedMode = AudioTrackMode.SILENT
                    }
                    Thread.sleep(60L)
                    continue
                }

                if (activeMode != lastRenderedMode) {
                    synth.resetFilters()
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
                    if (logicalMode == activeMode || (activeMode == getEffectivePlaybackMode())) {
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

private class FantasyAudioSynthesizer(private val sampleRate: Int) {
    private var lpState = 0.0
    private var lpBossState = 0.0
    private val reverbDelayA = DoubleArray((sampleRate * 0.089).toInt())
    private val reverbDelayB = DoubleArray((sampleRate * 0.137).toInt())
    private var revIdxA = 0
    private var revIdxB = 0
    private var noiseSeed = 0x13579BDF

    fun resetFilters() {
        lpState = 0.0
        lpBossState = 0.0
        reverbDelayA.fill(0.0)
        reverbDelayB.fill(0.0)
        revIdxA = 0
        revIdxB = 0
    }

    private fun nextNoise(): Double {
        noiseSeed = noiseSeed * 1664525 + 1013904223
        return ((noiseSeed shr 16) and 0xFFFF) / 32768.0 - 1.0
    }

    private fun midiToHz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    private fun applyRoomReverb(dry: Double, wetAmount: Double): Double {
        val tapA = reverbDelayA[revIdxA]
        val tapB = reverbDelayB[revIdxB]
        reverbDelayA[revIdxA] = dry + tapB * 0.24
        reverbDelayB[revIdxB] = dry + tapA * 0.20
        revIdxA = (revIdxA + 1) % reverbDelayA.size
        revIdxB = (revIdxB + 1) % reverbDelayB.size
        return dry * (1.0 - wetAmount * 0.35) + (tapA + tapB) * 0.5 * wetAmount
    }

    /** Returns true if a one-shot SFX finished within this chunk. */
    fun renderChunk(mode: AudioTrackMode, startSample: Long, out: ShortArray): Boolean {
        var finished = false
        for (i in out.indices) {
            val s = startSample + i
            val t = s.toDouble() / sampleRate.toDouble()
            val sampleVal = when (mode) {
                AudioTrackMode.MENU_TAVERN -> synthesizeTavernSample(t)
                AudioTrackMode.BATTLE_NORMAL -> synthesizeBattleNormalSample(t)
                AudioTrackMode.BATTLE_BOSS -> synthesizeBattleBossSample(t)
                AudioTrackMode.SFX_VICTORY -> {
                    if (t >= 2.85) finished = true
                    synthesizeVictoryFanfare(t)
                }
                AudioTrackMode.SFX_DEFEAT -> {
                    if (t >= 2.55) finished = true
                    synthesizeDefeatFail(t)
                }
                AudioTrackMode.SILENT -> 0.0
            }
            val clipped = tanh(sampleVal * 0.85)
            out[i] = (clipped * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return finished
    }

    // THEME 1: Calm Medieval Tavern (D Dorian, gentle 3/4 lute + wooden flute + warm drone)
    private fun synthesizeTavernSample(t: Double): Double {
        val stepSec = 0.26
        val totalSteps = 48
        val loopDur = totalSteps * stepSec
        val loopT = t % loopDur
        val stepIdx = ((loopT / stepSec).toInt()).coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec

        // 4-bar chord roots in D Dorian: Dm (0..11), C (12..23), Gm (24..35), Am -> Dm (36..47)
        val chordRoots = intArrayOf(50, 50, 48, 48, 43, 43, 45, 50) // D3, D3, C3, C3, G2, G2, A2, D3
        val chordThirds = intArrayOf(53, 53, 52, 52, 46, 46, 48, 53)
        val chordFifths = intArrayOf(57, 57, 55, 55, 50, 50, 52, 57)
        val phraseIdx = (stepIdx / 6).coerceIn(0, 7)

        // Plucked lute arpeggio (3/4 pattern: root - 3rd - 5th - octave - 5th - 3rd)
        val arpMidi = when (stepIdx % 6) {
            0 -> chordRoots[phraseIdx]
            1 -> chordThirds[phraseIdx]
            2 -> chordFifths[phraseIdx]
            3 -> chordRoots[phraseIdx] + 12
            4 -> chordFifths[phraseIdx]
            else -> chordThirds[phraseIdx]
        }
        val luteHz = midiToHz(arpMidi)
        val lutePhase = 2.0 * PI * luteHz * stepT
        val luteEnv = (1.0 - exp(-stepT * 110.0)) * exp(-stepT * 4.8)
        val luteWave = (
            sin(lutePhase) +
                0.42 * exp(-stepT * 7.5) * sin(2.0 * lutePhase) +
                0.18 * exp(-stepT * 13.0) * sin(3.0 * lutePhase)
            ) * luteEnv * 0.28

        // Gentle wooden flute melody over 24 two-step notes (-1 = rest)
        val fluteMelody = intArrayOf(
            74, 77, 76, 74, 72, 74,
            69, -1, 72, 74, 76, 72,
            74, 72, 69, -1, 67, 69,
            70, 69, 67, 65, 69, -1,
            74, 76, 77, 79, 77, 76,
            74, 72, 74, -1, 76, 77,
            79, 77, 76, 74, 72, 69,
            72, 74, 74, -1, -1, -1
        )
        val fluteMidi = fluteMelody[stepIdx]
        val fluteVoice = if (fluteMidi > 0) {
            val prevSame = stepIdx > 0 && fluteMelody[stepIdx - 1] == fluteMidi
            val nextSame = stepIdx + 1 < totalSteps && fluteMelody[stepIdx + 1] == fluteMidi
            val fHz = midiToHz(fluteMidi)
            val vib = 1.0 + 0.004 * sin(2.0 * PI * 4.8 * t)
            val fPhase = 2.0 * PI * fHz * vib * t
            val atk = if (prevSame) 1.0 else (1.0 - exp(-stepT * 28.0))
            val rel = if (nextSame) 1.0 else exp(-maxOf(0.0, stepT - stepSec * 0.45) * 9.0)
            (sin(fPhase) + 0.14 * sin(2.0 * fPhase)) * atk * rel * 0.20
        } else 0.0

        // Warm tavern fifth drone (D2 + A2)
        val drone = (sin(2.0 * PI * midiToHz(38) * t) + 0.55 * sin(2.0 * PI * midiToHz(45) * t)) * 0.065

        // Soft warm low-pass + tavern hall reverb
        val raw = luteWave + fluteVoice + drone
        lpState += 0.34 * (raw - lpState)
        return applyRoomReverb(lpState, 0.32)
    }

    // THEME 2: Energetic Fantasy Battle (E Minor, driving 4/4 horns/strings + percussion)
    private fun synthesizeBattleNormalSample(t: Double): Double {
        val stepSec = 0.19
        val totalSteps = 64
        val loopDur = totalSteps * stepSec
        val loopT = t % loopDur
        val stepIdx = ((loopT / stepSec).toInt()).coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec

        // 8-bar progression (8 eighths per bar): Em, Em, C, D, Em, G, D, Bm
        val bassNotes = intArrayOf(40, 40, 36, 38, 40, 43, 38, 35)
        val barIdx = (stepIdx / 8).coerceIn(0, 7)
        val ostinatoHz = midiToHz(bassNotes[barIdx])
        val ostEnv = (1.0 - exp(-stepT * 140.0)) * exp(-stepT * 7.5)
        val ostPhase = 2.0 * PI * ostinatoHz * stepT
        val ostinato = (
            sin(ostPhase) +
                0.55 * sin(2.0 * ostPhase) +
                0.35 * sin(1.5 * ostPhase) // Fifth power-chord drive
            ) * ostEnv * 0.22

        val leadMelody = intArrayOf(
            76, 76, 79, 78, 76, 74, 71, -1,
            76, 79, 81, 79, 78, 76, 74, -1,
            72, 74, 76, 79, 78, 74, 76, 72,
            74, 74, 76, 78, 79, 78, 74, -1,
            76, 79, 83, 81, 79, 78, 76, 74,
            79, 79, 83, 81, 79, 76, 74, -1,
            74, 76, 78, 79, 81, 79, 78, 74,
            76, -1, 74, 71, 76, 76, -1, -1
        )
        val leadMidi = leadMelody[stepIdx]
        val leadVoice = if (leadMidi > 0) {
            val lHz = midiToHz(leadMidi)
            val lPhase = 2.0 * PI * lHz * t
            val atk = 1.0 - exp(-stepT * 42.0)
            val rel = exp(-maxOf(0.0, stepT - stepSec * 0.35) * 6.5)
            val horn = sin(lPhase) + 0.45 * sin(2.0 * lPhase) + 0.22 * sin(3.0 * lPhase)
            horn * atk * rel * 0.22
        } else 0.0

        // Battle percussion: Timpani on steps 0, 4; Snare on steps 2, 6 (+ pickup on 7)
        val barStep = stepIdx % 8
        val timpani = if (barStep == 0 || barStep == 4) {
            val pitchDrop = 95.0 - 45.0 * (1.0 - exp(-stepT * 28.0))
            sin(2.0 * PI * pitchDrop * stepT) * exp(-stepT * 11.0) * 0.32
        } else 0.0
        val snare = if (barStep == 2 || barStep == 6 || (barStep == 7 && barIdx % 2 == 1)) {
            nextNoise() * exp(-stepT * 24.0) * 0.14
        } else 0.0

        val raw = ostinato + leadVoice + timpani + snare
        lpState += 0.42 * (raw - lpState)
        return applyRoomReverb(lpState, 0.24)
    }

    // THEME 3: Serious & Powerful Boss Battle (D Harmonic Minor, pipe organ + dark brass + heavy war drums)
    private fun synthesizeBattleBossSample(t: Double): Double {
        val stepSec = 0.215
        val totalSteps = 64
        val loopDur = totalSteps * stepSec
        val loopT = t % loopDur
        val stepIdx = ((loopT / stepSec).toInt()).coerceIn(0, totalSteps - 1)
        val stepT = loopT - stepIdx * stepSec

        // Boss progression: Dm -> Bb -> Gm -> A (with C# leading tone)
        val roots = intArrayOf(38, 38, 34, 34, 31, 31, 33, 33)
        val barIdx = (stepIdx / 8).coerceIn(0, 7)
        val subStepT = stepT % (stepSec * 0.5)
        val bassHz = midiToHz(roots[barIdx])
        val bassEnv = (1.0 - exp(-subStepT * 160.0)) * exp(-subStepT * 12.0)
        val bassOsc = sin(2.0 * PI * bassHz * subStepT) +
            0.65 * sin(2.0 * PI * bassHz * 2.0 * subStepT) +
            0.35 * sin(2.0 * PI * bassHz * 3.0 * subStepT)
        val celloOstinato = bassOsc * bassEnv * 0.25

        // Cathedral pipe organ + dark brass lead
        val bossLead = intArrayOf(
            62, -1, 65, 64, 61, -1, 62, -1,
            62, 65, 69, 68, 69, -1, 65, 64,
            70, -1, 69, 67, 65, 64, 62, -1,
            67, 65, 64, 61, 62, -1, 57, -1,
            74, -1, 77, 76, 73, -1, 74, -1,
            70, 72, 74, 76, 77, -1, 76, 74,
            79, -1, 77, 74, 76, 74, 73, 69,
            74, -1, 73, 69, 74, 74, -1, -1
        )
        val bMidi = bossLead[stepIdx]
        val brassChoir = if (bMidi > 0) {
            val f = midiToHz(bMidi)
            val atk = 1.0 - exp(-stepT * 30.0)
            val rel = exp(-maxOf(0.0, stepT - stepSec * 0.45) * 4.5)
            // Unison octave + pipe organ 3rd/4th harmonics
            val wave = sin(2.0 * PI * f * t) +
                0.70 * sin(2.0 * PI * (f * 0.5) * t) +
                0.45 * sin(2.0 * PI * (f * 2.0) * t) +
                0.22 * sin(2.0 * PI * (f * 3.0) * t)
            wave * atk * rel * 0.20
        } else 0.0

        // Heavy war taiko double strikes & crash on phrase downbeat
        val barStep = stepIdx % 8
        val warDrum = if (barStep == 0 || barStep == 1 || barStep == 4 || barStep == 6) {
            val pitchDrop = 82.0 - 44.0 * (1.0 - exp(-stepT * 22.0))
            sin(2.0 * PI * pitchDrop * stepT) * exp(-stepT * 8.5) * 0.36
        } else 0.0
        val cymbalCrash = if (stepIdx % 16 == 0) {
            nextNoise() * exp(-loopT.rem(16 * stepSec) * 3.2) * 0.12
        } else 0.0

        val raw = celloOstinato + brassChoir + warDrum + cymbalCrash
        lpBossState += 0.36 * (raw - lpBossState)
        return applyRoomReverb(lpBossState, 0.34)
    }

    // SOUND 1: One-Shot Victory Fanfare (~2.8s, then silence)
    private fun synthesizeVictoryFanfare(t: Double): Double {
        if (t > 2.80) return 0.0

        fun brassNote(noteT: Double, hz: Double, dur: Double): Double {
            if (noteT < 0.0 || noteT > dur) return 0.0
            val env = (1.0 - exp(-noteT * 75.0)) * exp(-maxOf(0.0, noteT - dur * 0.45) * 5.2)
            val p = 2.0 * PI * hz * noteT
            return (sin(p) + 0.55 * sin(2.0 * p) + 0.30 * sin(3.0 * p) + 0.14 * sin(4.0 * p)) * env
        }

        val sig = when {
            t < 0.14 -> brassNote(t, midiToHz(67), 0.14) * 0.30 // G4
            t < 0.28 -> brassNote(t - 0.14, midiToHz(72), 0.14) * 0.32 // C5
            t < 0.42 -> brassNote(t - 0.28, midiToHz(76), 0.14) * 0.34 // E5
            t < 0.78 -> brassNote(t - 0.42, midiToHz(79), 0.36) * 0.36 // G5
            t < 0.96 -> brassNote(t - 0.78, midiToHz(76), 0.18) * 0.34 // E5
            else -> {
                val chordT = t - 0.96
                val chordDur = 1.84
                val env = (1.0 - exp(-chordT * 50.0)) * exp(-chordT * 1.8)
                val c4 = brassNote(chordT, midiToHz(60), chordDur)
                val g4 = brassNote(chordT, midiToHz(67), chordDur)
                val c5 = brassNote(chordT, midiToHz(72), chordDur)
                val e5 = brassNote(chordT, midiToHz(76), chordDur)
                val g5 = brassNote(chordT, midiToHz(79), chordDur)
                val bellC6 = sin(2.0 * PI * midiToHz(84) * chordT) * exp(-chordT * 2.5) * 0.18
                val cymbal = nextNoise() * exp(-chordT * 3.5) * 0.12
                (c4 + g4 + c5 + e5 + g5) * 0.13 * env + bellC6 + cymbal
            }
        }
        val snareRoll = if (t < 0.42) nextNoise() * 0.08 * (1.0 - t / 0.45) else 0.0
        val fadeTail = if (t > 2.55) ((2.80 - t) / 0.25).coerceIn(0.0, 1.0) else 1.0
        lpState += 0.48 * ((sig + snareRoll) - lpState)
        return applyRoomReverb(lpState, 0.35) * fadeTail
    }

    // SOUND 2: One-Shot Defeat / Fail Motif (~2.5s, then silence)
    private fun synthesizeDefeatFail(t: Double): Double {
        if (t > 2.50) return 0.0

        fun mournfulHorn(noteT: Double, baseHz: Double, dur: Double, bendDown: Boolean = false): Double {
            if (noteT < 0.0 || noteT > dur) return 0.0
            val bendFactor = if (bendDown) {
                1.0 - 0.08 * (noteT / dur) + 0.012 * sin(2.0 * PI * 4.5 * noteT)
            } else 1.0
            val f = baseHz * bendFactor
            val env = (1.0 - exp(-noteT * 40.0)) * exp(-maxOf(0.0, noteT - dur * 0.35) * 3.2)
            val p = 2.0 * PI * f * noteT
            return (sin(p) + 0.48 * sin(2.0 * p) + 0.22 * sin(3.0 * p)) * env
        }

        val melody = when {
            t < 0.36 -> mournfulHorn(t, midiToHz(55), 0.36) * 0.36 // G3
            t < 0.72 -> mournfulHorn(t - 0.36, midiToHz(54), 0.36) * 0.36 // F#3
            t < 1.08 -> mournfulHorn(t - 0.72, midiToHz(53), 0.36) * 0.36 // F3
            else -> mournfulHorn(t - 1.08, midiToHz(52), 1.42, bendDown = true) * 0.40 // E3 -> sag down
        }
        val lowThud = if (t < 0.5 || (t in 1.08..1.8)) {
            val thudT = if (t < 0.5) t else t - 1.08
            val pitch = 68.0 - 28.0 * (1.0 - exp(-thudT * 18.0))
            sin(2.0 * PI * pitch * thudT) * exp(-thudT * 5.5) * 0.24
        } else 0.0

        val fadeTail = if (t > 2.25) ((2.50 - t) / 0.25).coerceIn(0.0, 1.0) else 1.0
        lpState += 0.32 * ((melody + lowThud) - lpState)
        return applyRoomReverb(lpState, 0.28) * fadeTail
    }
}
