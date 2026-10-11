package com.ah.filemanagerplus.core.video

import androidx.compose.ui.graphics.ImageBitmap

interface VideoPlayer {

    suspend fun load(path: String): Boolean

    fun play()
    fun pause()
    fun stop()

    fun seekTo(millis: Long)

    fun getPositionMs(): Long
    fun getDurationMs(): Long
    fun isPlaying(): Boolean

    fun setVolume(volume: Double)
    fun getVolume(): Double

    fun setRate(rate: Double)
    fun getRate(): Double

    /** رسالة آخر خطأ حصل — null إذا لا يوجد */
    fun getLastError(): String?

    suspend fun captureFrame(width: Int, height: Int): ImageBitmap?

    fun setOnComplete(callback: () -> Unit)
    fun setOnError(callback: (String) -> Unit)

    fun release()
}

expect fun createVideoPlayer(): VideoPlayer