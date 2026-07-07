package com.thunder.ktvboss.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

object DemoBgmPlayer {

    private var track: AudioTrack? = null

    fun start(context: Context) {
        if (track != null) return

        val sampleRate = 44100
        val durationMs = 6500
        val pcmShorts = synthPcmShorts(sampleRate, durationMs)
        val minSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcmShorts.size * 2)

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minSize,
            AudioTrack.MODE_STATIC
        )

        audioTrack.write(pcmShorts, 0, pcmShorts.size)
        audioTrack.setLoopPoints(0, pcmShorts.size, -1)
        audioTrack.setStereoVolume(0.85f, 0.85f)
        audioTrack.play()
        track = audioTrack
    }

    fun stop() {
        val current = track ?: return
        track = null
        try {
            current.pause()
        } catch (_: Exception) {
        }
        current.release()
    }

    private fun synthPcmShorts(sampleRate: Int, durationMs: Int): ShortArray {
        val samples = (sampleRate * durationMs / 1000.0).toInt()
        val pcm = ShortArray(samples)

        val bpm = 120.0
        val beatSeconds = 60.0 / bpm
        val baseA = 110.0
        val baseB = 220.0

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate.toDouble()
            val beatPhase = (t % beatSeconds) / beatSeconds
            val kickEnv = kotlin.math.exp(-beatPhase * 10.0)
            val kick = sin(2.0 * PI * 70.0 * t) * kickEnv * 0.7

            val bassEnv = 0.18 + 0.82 * (0.5 + 0.5 * sin(2.0 * PI * (1.0 / beatSeconds) * t))
            val bass =
                (sin(2.0 * PI * baseA * t) * 0.34 + sin(2.0 * PI * baseB * t) * 0.18) * bassEnv

            val shimmer = sin(2.0 * PI * 880.0 * t) * 0.05

            val value = (kick + bass + shimmer).coerceIn(-1.0, 1.0)
            pcm[i] = (value * 32767.0).toInt().toShort()
        }
        return pcm
    }
}
