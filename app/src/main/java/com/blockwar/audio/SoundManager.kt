package com.blockwar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(private val context: Context) {
    @Volatile var isEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopBgm()
        }

    private val sampleRate = 22050
    private var bgmJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val audioLifecycle = AudioPlaybackLifecycle()

    @Volatile
    private var currentBgmId: String? = null

    // Pre-calculated note frequencies in Hz
    private val notes = mapOf(
        "C3" to 130.81f, "D3" to 146.83f, "E3" to 164.81f, "F3" to 174.61f, "G3" to 196.00f, "A3" to 220.00f, "B3" to 246.94f,
        "C4" to 261.63f, "D4" to 293.66f, "E4" to 329.63f, "F4" to 349.23f, "G4" to 392.00f, "A4" to 440.00f, "B4" to 493.88f,
        "C5" to 523.25f, "D5" to 587.33f, "E5" to 659.25f, "F5" to 698.46f, "G5" to 783.99f, "A5" to 880.00f, "B5" to 987.77f,
        "C6" to 1046.50f, "REST" to 0f
    )

    @Synchronized fun startBgm(bgmId: String) {
        if (!isEnabled) return
        if (currentBgmId == bgmId && bgmJob?.isActive == true) return

        stopBgm()
        currentBgmId = bgmId

        bgmJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(sampleRate / 4)

                audioLifecycle.play(
                    create = {
                    AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                    },
                    stop = { it.stop() },
                    release = { it.release() }
                ) { audioTrack ->
                currentCoroutineContext().ensureActive()

                audioTrack.play()

                while (isActive && currentBgmId == bgmId && isEnabled) {
                    when (bgmId) {
                        "musica_synthwave_intenso", "musica_orquestra_epica", "musica_batida_eletronica", "musica_rock_pesado", "musica_ambiente_espacial" -> playCosmeticLoop(audioTrack, bgmId)
                        "rush" -> playRushLoop(audioTrack)
                        "synth" -> playSynthLoop(audioTrack)
                        else -> playArcadeLoop(audioTrack)
                    }
                }

                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("SoundManager", "Error in BGM track: ${e.message}")
            }
        }
        bgmJob?.start()
    }

    @Synchronized fun stopBgm() {
        currentBgmId = null
        bgmJob?.cancel()
        bgmJob = null
    }

    // =========================================================================
    // SFX Synthesizers
    // =========================================================================

    fun playMove() {
        if (!isEnabled) return
        playPcmTone(freqStart = 400f, freqEnd = 500f, durationMs = 25, volume = 0.25f, waveType = WaveType.SQUARE)
    }

    fun playRotate() {
        if (!isEnabled) return
        playPcmTone(freqStart = 500f, freqEnd = 800f, durationMs = 35, volume = 0.3f, waveType = WaveType.SQUARE)
    }

    fun playDrop() {
        if (!isEnabled) return
        playPcmTone(freqStart = 260f, freqEnd = 90f, durationMs = 60, volume = 0.4f, waveType = WaveType.TRIANGLE)
    }

    fun playLineClear(lines: Int) {
        if (!isEnabled) return
        scope.launch {
            try {
                val baseDuration = 55
                when (lines) {
                    1 -> {
                        playPcmTone(523.25f, 523.25f, baseDuration, 0.35f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(659.25f, 659.25f, baseDuration, 0.4f, WaveType.SQUARE)
                    }
                    2 -> {
                        playPcmTone(523.25f, 523.25f, baseDuration, 0.35f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(659.25f, 659.25f, baseDuration, 0.4f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(783.99f, 783.99f, baseDuration, 0.45f, WaveType.SQUARE)
                    }
                    3 -> {
                        playPcmTone(440f, 440f, baseDuration, 0.35f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(554f, 554f, baseDuration, 0.4f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(659f, 659f, baseDuration, 0.45f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(880f, 880f, 90, 0.5f, WaveType.SQUARE)
                    }
                    else -> { // Tetris 4 lines
                        playPcmTone(523.25f, 523.25f, baseDuration, 0.4f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(659.25f, 659.25f, baseDuration, 0.4f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(783.99f, 783.99f, baseDuration, 0.45f, WaveType.SQUARE)
                        kotlinx.coroutines.delay(baseDuration.toLong())
                        playPcmTone(1046.50f, 1046.50f, 150, 0.55f, WaveType.SQUARE)
                    }
                }
            } catch (e: Throwable) {
                // Ignore audio playback error
            }
        }
    }

    fun playGarbageIncoming() {
        if (!isEnabled) return
        scope.launch {
            try {
                playPcmTone(180f, 180f, 60, 0.45f, WaveType.SAWTOOTH)
                kotlinx.coroutines.delay(65)
                playPcmTone(140f, 140f, 90, 0.5f, WaveType.SAWTOOTH)
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    fun playVictory() {
        if (!isEnabled) return
        scope.launch {
            try {
                val speed = 80
                playPcmTone(523.25f, 523.25f, speed, 0.4f, WaveType.SQUARE)
                kotlinx.coroutines.delay(speed.toLong())
                playPcmTone(659.25f, 659.25f, speed, 0.4f, WaveType.SQUARE)
                kotlinx.coroutines.delay(speed.toLong())
                playPcmTone(783.99f, 783.99f, speed, 0.45f, WaveType.SQUARE)
                kotlinx.coroutines.delay(speed.toLong())
                playPcmTone(1046.50f, 1046.50f, speed * 2, 0.55f, WaveType.SQUARE)
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    fun playGameOver() {
        if (!isEnabled) return
        scope.launch {
            try {
                val speed = 100
                playPcmTone(440f, 440f, speed, 0.4f, WaveType.SQUARE)
                kotlinx.coroutines.delay(speed.toLong())
                playPcmTone(415f, 415f, speed, 0.4f, WaveType.SQUARE)
                kotlinx.coroutines.delay(speed.toLong())
                playPcmTone(392f, 392f, speed, 0.4f, WaveType.SQUARE)
                kotlinx.coroutines.delay(speed.toLong())
                playPcmTone(349f, 349f, speed * 2, 0.45f, WaveType.SAWTOOTH)
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    // =========================================================================
    // Synthesized Melodies
    // =========================================================================

    private suspend fun playArcadeLoop(track: AudioTrack) {
        // Classic 8-bit upbeat chiptune sequence (A-Theme retro style)
        val melody = listOf(
            Pair("E5", 220), Pair("B4", 110), Pair("C5", 110), Pair("D5", 220),
            Pair("C5", 110), Pair("B4", 110), Pair("A4", 220), Pair("A4", 110),
            Pair("C5", 110), Pair("E5", 220), Pair("D5", 110), Pair("C5", 110),
            Pair("B4", 330), Pair("C5", 110), Pair("D5", 220), Pair("E5", 220),
            Pair("C5", 220), Pair("A4", 220), Pair("A4", 220), Pair("REST", 110)
        )
        for ((note, dur) in melody) {
            currentCoroutineContext().ensureActive()
            if (!isEnabled) return
            val freq = notes[note] ?: 0f
            writeToneToTrack(track, freq, dur, volume = 0.22f, wave = WaveType.SQUARE)
        }
    }

    private suspend fun playRushLoop(track: AudioTrack) {
        // Battle Rush: high-tempo arpeggiated 16th-note fast chiptune
        val pattern = listOf(
            Pair("A4", 90), Pair("C5", 90), Pair("E5", 90), Pair("A5", 90),
            Pair("G4", 90), Pair("B4", 90), Pair("D5", 90), Pair("G5", 90),
            Pair("F4", 90), Pair("A4", 90), Pair("C5", 90), Pair("F5", 90),
            Pair("E4", 90), Pair("G4", 90), Pair("B4", 90), Pair("E5", 90)
        )
        for ((note, dur) in pattern) {
            currentCoroutineContext().ensureActive()
            if (!isEnabled) return
            val freq = notes[note] ?: 0f
            writeToneToTrack(track, freq, dur, volume = 0.24f, wave = WaveType.SAWTOOTH)
        }
    }

    private suspend fun playSynthLoop(track: AudioTrack) {
        // Dark Synth Retro: synthwave atmospheric bassline & lead
        val pattern = listOf(
            Pair("A3", 260), Pair("A3", 130), Pair("C4", 130),
            Pair("D4", 260), Pair("E4", 260),
            Pair("F3", 260), Pair("F3", 130), Pair("A3", 130),
            Pair("G3", 260), Pair("E3", 260),
            Pair("D3", 260), Pair("F3", 260),
            Pair("E3", 520)
        )
        for ((note, dur) in pattern) {
            currentCoroutineContext().ensureActive()
            if (!isEnabled) return
            val freq = notes[note] ?: 0f
            writeToneToTrack(track, freq, dur, volume = 0.26f, wave = WaveType.TRIANGLE)
        }
    }

    // Original synthesized arrangements: no dependency on the empty MP3 placeholders.
    private suspend fun playCosmeticLoop(track: AudioTrack, id: String) {
        val pattern = when(id) {
            "musica_synthwave_intenso" -> listOf("A3","E4","A4","C5","G4","E4","C4","E4")
            "musica_orquestra_epica" -> listOf("C4","G4","E4","C5","B4","G4","A4","D5")
            "musica_batida_eletronica" -> listOf("C3","C4","REST","G4","C3","E4","REST","G4")
            "musica_rock_pesado" -> listOf("E3","E3","G3","A3","E3","D3","G3","E3")
            else -> listOf("C4","E4","G4","B4","A4","G4","E4","REST")
        }
        val duration = when(id) { "musica_ambiente_espacial" -> 560; "musica_orquestra_epica" -> 310; "musica_rock_pesado" -> 150; else -> 190 }
        val wave = when(id) { "musica_rock_pesado" -> WaveType.SAWTOOTH; "musica_batida_eletronica" -> WaveType.SQUARE; "musica_ambiente_espacial" -> WaveType.SINE; else -> WaveType.TRIANGLE }
        for (note in pattern) {
            currentCoroutineContext().ensureActive()
            if (currentBgmId != id || !isEnabled) return
            writeToneToTrack(track, notes[note] ?: 0f, duration, .19f, wave)
        }
    }

    private suspend fun writeToneToTrack(
        track: AudioTrack,
        freq: Float,
        durationMs: Int,
        volume: Float,
        wave: WaveType
    ) {
        currentCoroutineContext().ensureActive()
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)

        if (freq <= 0f) {
            // Silence/Rest
            writeCancellable(track, buffer)
            return
        }

        val period = sampleRate / freq
        for (i in 0 until numSamples) {
            val sampleVal: Float = when (wave) {
                WaveType.SQUARE -> if ((i % period) < (period / 2)) 1f else -1f
                WaveType.TRIANGLE -> {
                    val pos = (i % period) / period
                    if (pos < 0.5f) (pos * 4f - 1f) else (3f - pos * 4f)
                }
                WaveType.SAWTOOTH -> {
                    val pos = (i % period) / period
                    pos * 2f - 1f
                }
                WaveType.SINE -> sin(2.0 * PI * i.toDouble() * freq.toDouble() / sampleRate).toFloat()
            }
            // Apply slight envelope (attack & decay) to avoid clicking
            val envelope = when {
                i < 100 -> i / 100f
                i > numSamples - 150 -> (numSamples - i) / 150f
                else -> 1.0f
            }
            buffer[i] = (sampleVal * volume * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        writeCancellable(track, buffer)
    }

    // Short writes make long notes and rests observe cancellation promptly.
    private suspend fun writeCancellable(track: AudioTrack, buffer: ShortArray) {
        var offset = 0
        val chunk = sampleRate / 50 // 20 ms
        while (offset < buffer.size) {
            currentCoroutineContext().ensureActive()
            if (!isEnabled) return
            val count = minOf(chunk, buffer.size - offset)
            val written = track.write(buffer, offset, count)
            check(written > 0) { "AudioTrack write failed: $written" }
            offset += written
        }
    }

    private fun playPcmTone(
        freqStart: Float,
        freqEnd: Float,
        durationMs: Int,
        volume: Float,
        waveType: WaveType
    ) {
        scope.launch {
            try {
                val numSamples = (sampleRate * durationMs / 1000)
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples.toFloat()
                    val currentFreq = freqStart + (freqEnd - freqStart) * progress
                    val period = sampleRate / currentFreq.coerceAtLeast(20f)
                    val sampleVal: Float = when (waveType) {
                        WaveType.SQUARE -> if ((i % period) < (period / 2)) 1f else -1f
                        WaveType.TRIANGLE -> {
                            val pos = (i % period) / period
                            if (pos < 0.5f) (pos * 4f - 1f) else (3f - pos * 4f)
                        }
                        WaveType.SAWTOOTH -> ((i % period) / period) * 2f - 1f
                        WaveType.SINE -> sin(2.0 * PI * i.toDouble() * currentFreq.toDouble() / sampleRate).toFloat()
                    }
                    val envelope = 1.0f - progress
                    buffer[i] = (sampleVal * volume * envelope * Short.MAX_VALUE).toInt().toShort()
                }

                useAudioResource(
                    create = {
                    AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                    },
                    stop = { it.stop() },
                    release = { it.release() }
                ) { track ->
                currentCoroutineContext().ensureActive()
                if (!isEnabled) return@useAudioResource

                check(track.write(buffer, 0, buffer.size) == buffer.size) { "Incomplete sound effect buffer" }
                track.play()
                // Let it play out then release
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ignore audio playback error
            }
        }
    }

    enum class WaveType {
        SQUARE, TRIANGLE, SAWTOOTH, SINE
    }
}
