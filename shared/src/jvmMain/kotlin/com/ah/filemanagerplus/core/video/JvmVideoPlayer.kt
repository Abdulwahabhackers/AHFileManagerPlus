package com.ah.filemanagerplus.core.video

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import javafx.application.Platform
import javafx.scene.SnapshotParameters
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.media.Media
import javafx.scene.media.MediaException
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ImageInfo
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume

object JavaFxToolkit {
    @Volatile private var initialized = false

    @Synchronized
    fun ensureInitialized() {
        if (initialized) return
        try {
            val latch = CountDownLatch(1)
            Platform.startup { latch.countDown() }
            latch.await()
        } catch (e: IllegalStateException) {
            // Already initialized
        }
        initialized = true
    }
}

class JvmVideoPlayer : VideoPlayer {

    companion object {
        /** دقة العرض القصوى — تحدّد أداء التقاط الإطارات */
        private const val MAX_WIDTH = 1280.0
        private const val MAX_HEIGHT = 720.0
    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaView: MediaView? = null
    private var onComplete: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    @Volatile private var lastErrorMessage: String? = null

    private var reuseImage: WritableImage? = null
    private var reuseWidth: Int = 0
    private var reuseHeight: Int = 0
    private var pixelsBuffer: IntArray = IntArray(0)
    private var bytesBuffer: ByteArray = ByteArray(0)
    private val snapshotParams = SnapshotParameters()

    init {
        JavaFxToolkit.ensureInitialized()
    }

    override suspend fun load(path: String): Boolean {
        return suspendCancellableCoroutine { cont ->
            Platform.runLater {
                try {
                    releaseInternal()
                    lastErrorMessage = null

                    val f = File(path)
                    if (!f.exists() || !f.isFile) {
                        lastErrorMessage = "الملف غير موجود"
                        cont.resume(false)
                        return@runLater
                    }

                    val uri = f.toURI().toString()
                    val media: Media = try {
                        Media(uri)
                    } catch (e: MediaException) {
                        lastErrorMessage = "الصيغة غير مدعومة (${f.extension.uppercase()})"
                        onError?.invoke(lastErrorMessage!!)
                        cont.resume(false)
                        return@runLater
                    } catch (e: Exception) {
                        lastErrorMessage = "فشل تحميل الفيديو: ${e.message ?: "خطأ غير معروف"}"
                        onError?.invoke(lastErrorMessage!!)
                        cont.resume(false)
                        return@runLater
                    }

                    val player = MediaPlayer(media)
                    val view = MediaView(player)
                    view.isPreserveRatio = true

                    player.setOnReady {
                        val vw = media.width
                        val vh = media.height
                        if (vw > 0.0 && vh > 0.0) {
                            // حد أقصى 1280x720 — يحسّن الأداء 6x للفيديو 4K
                            val scale = minOf(MAX_WIDTH / vw, MAX_HEIGHT / vh, 1.0)
                            view.fitWidth = vw * scale
                            view.fitHeight = vh * scale
                        }
                    }

                    player.setOnEndOfMedia {
                        onComplete?.invoke()
                    }

                    player.setOnError {
                        val msg = try {
                            player.error?.message ?: "خطأ غير معروف"
                        } catch (e: Exception) { "خطأ غير معروف" }
                        lastErrorMessage = "تعذّر تشغيل الفيديو: $msg"
                        onError?.invoke(lastErrorMessage!!)
                    }

                    mediaPlayer = player
                    mediaView = view

                    cont.resume(true)
                } catch (e: Exception) {
                    lastErrorMessage = "خطأ غير متوقع: ${e.message ?: e.toString()}"
                    onError?.invoke(lastErrorMessage!!)
                    cont.resume(false)
                }
            }
        }
    }

    override fun play() {
        Platform.runLater { mediaPlayer?.play() }
    }

    override fun pause() {
        Platform.runLater { mediaPlayer?.pause() }
    }

    override fun stop() {
        Platform.runLater { mediaPlayer?.stop() }
    }

    override fun seekTo(millis: Long) {
        Platform.runLater {
            mediaPlayer?.seek(Duration.millis(millis.toDouble()))
        }
    }

    override fun getPositionMs(): Long {
        return try {
            mediaPlayer?.currentTime?.toMillis()?.toLong() ?: 0L
        } catch (e: Exception) { 0L }
    }

    override fun getDurationMs(): Long {
        return try {
            val d = mediaPlayer?.totalDuration ?: return 0L
            if (d.isUnknown) 0L else d.toMillis().toLong()
        } catch (e: Exception) { 0L }
    }

    override fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.status == MediaPlayer.Status.PLAYING
        } catch (e: Exception) { false }
    }

    override fun setVolume(volume: Double) {
        Platform.runLater {
            mediaPlayer?.volume = volume.coerceIn(0.0, 1.0)
        }
    }

    override fun getVolume(): Double {
        return try { mediaPlayer?.volume ?: 1.0 } catch (e: Exception) { 1.0 }
    }

    override fun setRate(rate: Double) {
        Platform.runLater {
            try { mediaPlayer?.rate = rate.coerceIn(0.25, 4.0) } catch (e: Exception) {}
        }
    }

    override fun getRate(): Double {
        return try { mediaPlayer?.rate ?: 1.0 } catch (e: Exception) { 1.0 }
    }

    override fun getLastError(): String? = lastErrorMessage

    override suspend fun captureFrame(width: Int, height: Int): ImageBitmap? {
        val view = mediaView ?: return null
        val player = mediaPlayer ?: return null
        if (lastErrorMessage != null) return null
        if (player.status == MediaPlayer.Status.HALTED ||
            player.status == MediaPlayer.Status.UNKNOWN
        ) return null

        // استخدم fit size (المحدود بـ 1280x720) بدل دقة الفيديو الأصلية
        val w: Int = try {
            view.fitWidth.toInt().coerceAtLeast(1)
        } catch (e: Exception) { MAX_WIDTH.toInt() }

        val h: Int = try {
            view.fitHeight.toInt().coerceAtLeast(1)
        } catch (e: Exception) { MAX_HEIGHT.toInt() }

        if (w <= 1 || h <= 1) return null

        return suspendCancellableCoroutine { cont ->
            Platform.runLater {
                try {
                    val image = if (reuseImage == null || reuseWidth != w || reuseHeight != h) {
                        val newImage = WritableImage(w, h)
                        reuseImage = newImage
                        reuseWidth = w
                        reuseHeight = h
                        pixelsBuffer = IntArray(w * h)
                        bytesBuffer = ByteArray(w * h * 4)
                        newImage
                    } else {
                        reuseImage!!
                    }

                    view.snapshot(snapshotParams, image)

                    val reader = image.pixelReader ?: run {
                        cont.resume(null)
                        return@runLater
                    }

                    reader.getPixels(
                        0, 0, w, h,
                        PixelFormat.getIntArgbPreInstance(),
                        pixelsBuffer, 0, w
                    )

                    val byteBuffer = ByteBuffer.wrap(bytesBuffer).order(ByteOrder.nativeOrder())
                    byteBuffer.asIntBuffer().put(pixelsBuffer)

                    val info = ImageInfo.makeN32Premul(w, h)
                    val skiaBitmap = Bitmap()
                    skiaBitmap.allocPixels(info)
                    skiaBitmap.installPixels(info, bytesBuffer, w * 4)

                    cont.resume(skiaBitmap.asComposeImageBitmap())
                } catch (e: Exception) {
                    cont.resume(null)
                }
            }
        }
    }

    override fun setOnComplete(callback: () -> Unit) {
        onComplete = callback
    }

    override fun setOnError(callback: (String) -> Unit) {
        onError = callback
    }

    override fun release() {
        Platform.runLater { releaseInternal() }
    }

    private fun releaseInternal() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
        } catch (e: Exception) {}
        mediaPlayer = null
        mediaView = null
        reuseImage = null
        reuseWidth = 0
        reuseHeight = 0
        pixelsBuffer = IntArray(0)
        bytesBuffer = ByteArray(0)
        lastErrorMessage = null
    }
}

actual fun createVideoPlayer(): VideoPlayer = JvmVideoPlayer()