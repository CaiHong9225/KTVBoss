package com.thunder.ktvboss.audio

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

object DemoBgmPlayer {

    private const val FILE_NAME = "ktv_boss_demo_bgm.wav"

    private var player: MediaPlayer? = null

    fun start(context: Context) {
        if (player != null) return

        val file = ensureWavFile(context)
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.isLooping = true
        mediaPlayer.setVolume(0.22f, 0.22f)
        mediaPlayer.prepare()
        mediaPlayer.start()
        player = mediaPlayer
    }

    fun stop() {
        val current = player ?: return
        player = null
        try {
            current.stop()
        } catch (_: Exception) {
        }
        current.release()
    }

    private fun ensureWavFile(context: Context): File {
        val file = File(context.cacheDir, FILE_NAME)
        if (file.exists() && file.length() > 44L) return file
        val bytes = synthWavBytes()
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun synthWavBytes(): ByteArray {
        val sampleRate = 22050
        val durationMs = 6500
        val samples = (sampleRate * durationMs / 1000.0).toInt()
        val pcm = ByteArray(samples * 2)

        val bpm = 120.0
        val beatSeconds = 60.0 / bpm
        val baseA = 110.0
        val baseB = 220.0

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate.toDouble()
            val beatPhase = (t % beatSeconds) / beatSeconds
            val kickEnv = kotlin.math.exp(-beatPhase * 10.0)
            val kick = sin(2.0 * PI * 70.0 * t) * kickEnv * 0.55

            val bassEnv = 0.18 + 0.82 * (0.5 + 0.5 * sin(2.0 * PI * (1.0 / beatSeconds) * t))
            val bass =
                (sin(2.0 * PI * baseA * t) * 0.28 + sin(2.0 * PI * baseB * t) * 0.14) * bassEnv

            val shimmer = sin(2.0 * PI * 880.0 * t) * 0.035

            val value = (kick + bass + shimmer).coerceIn(-1.0, 1.0)
            val s = (value * 32767.0).toInt().toShort()
            val idx = i * 2
            pcm[idx] = (s.toInt() and 0xFF).toByte()
            pcm[idx + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
        }

        val header = wavHeader(
            sampleRate = sampleRate,
            channels = 1,
            bitsPerSample = 16,
            pcmDataSize = pcm.size
        )
        return header + pcm
    }

    private fun wavHeader(
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        pcmDataSize: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalDataLen = pcmDataSize + 36

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16)
        writeShortLE(header, 20, 1)
        writeShortLE(header, 22, channels.toShort())
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, blockAlign.toShort())
        writeShortLE(header, 34, bitsPerSample.toShort())
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, pcmDataSize)
        return header
    }

    private fun writeIntLE(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buffer: ByteArray, offset: Int, value: Short) {
        buffer[offset] = (value.toInt() and 0xFF).toByte()
        buffer[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }
}

