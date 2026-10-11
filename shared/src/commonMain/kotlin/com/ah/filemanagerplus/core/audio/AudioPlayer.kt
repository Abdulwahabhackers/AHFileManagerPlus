package com.ah.filemanagerplus.core.audio

interface AudioPlayer {
    /** يبدأ تشغيل ملف. يعيد true لو نجح */
    fun play(path: String): Boolean

    fun pause()
    fun resume()
    fun stop()

    /** ينتقل لموضع معين (بالميلي ثانية) */
    fun seekTo(millis: Long)

    /** الموضع الحالي (ms) */
    fun getPositionMs(): Long

    /** مدة الملف (ms) */
    fun getDurationMs(): Long

    /** هل يشتغل الآن؟ */
    fun isPlaying(): Boolean

    /** يستدعى عند انتهاء الملف */
    fun setOnComplete(callback: () -> Unit)

    /** تنظيف */
    fun release()
}

expect fun createAudioPlayer(): AudioPlayer