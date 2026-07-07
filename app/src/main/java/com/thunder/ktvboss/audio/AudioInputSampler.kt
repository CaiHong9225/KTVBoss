package com.thunder.ktvboss.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min

class AudioInputSampler(
    private val context: Context,
    private val onLevelChanged: (Int) -> Unit
) {

    @Volatile
    private var running = false

    private val mainHandler = Handler(Looper.getMainLooper())

    fun start() {
        if (running) return
        running = true

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            running = false
            return
        }

        thread(name = "boss-audio-sampler") {
            val sampleRate = 16_000
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufferSize <= 0) {
                running = false
                return@thread
            }

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
            )
            val buffer = ShortArray(1024)

            try {
                record.startRecording()
                while (running) {
                    val readSize = record.read(buffer, 0, buffer.size)
                    if (readSize <= 0) continue

                    var peak = 0
                    for (index in 0 until readSize) {
                        peak = maxOf(peak, abs(buffer[index].toInt()))
                    }
                    val normalized = min(100, peak * 100 / 32767)
                    mainHandler.post { onLevelChanged(normalized) }
                }
            } catch (_: Exception) {
                mainHandler.post { onLevelChanged(0) }
            } finally {
                try {
                    record.stop()
                } catch (_: Exception) {
                }
                record.release()
            }
        }
    }

    fun stop() {
        running = false
    }
}
