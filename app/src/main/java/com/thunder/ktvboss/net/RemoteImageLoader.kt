package com.thunder.ktvboss.net

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object RemoteImageLoader {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024L / 16L).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun load(url: String, imageView: ImageView) {
        imageView.tag = url
        val cached = cache.get(url)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        thread(name = "boss-img") {
            val bitmap = download(url) ?: return@thread
            cache.put(url, bitmap)
            mainHandler.post {
                if (imageView.tag == url) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun download(url: String): Bitmap? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6500
                readTimeout = 6500
                instanceFollowRedirects = true
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return null
            }
            val stream = BufferedInputStream(connection.inputStream)
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            connection.disconnect()
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}

