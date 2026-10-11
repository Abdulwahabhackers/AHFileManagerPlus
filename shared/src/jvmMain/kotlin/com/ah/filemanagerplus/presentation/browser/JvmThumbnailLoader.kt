package com.ah.filemanagerplus.presentation.browser

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.mpatric.mp3agic.Mp3File
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

class JvmThumbnailLoader : ThumbnailLoader {

    private val imageExts = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "ico"
    )

    // ✅ الصيغ التي لا يدعمها Java — نتجاهلها
    private val unsupportedExts = setOf("heic", "heif", "avif")

    private val maxThumbSize = 240

    private val cache = object : LinkedHashMap<String, ImageBitmap?>(60, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, ImageBitmap?>?
        ): Boolean = size > 150
    }

    private val coverCache = object : LinkedHashMap<String, ImageBitmap?>(20, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, ImageBitmap?>?
        ): Boolean = size > 50
    }

    private val folderImageCache = mutableMapOf<String, String?>()

    override fun loadImage(path: String): ImageBitmap? {
        cache[path]?.let { return it }

        // ✅ تجاهل HEIC/HEIF/AVIF — Java لا يدعمها
        val ext = path.substringAfterLast('.', "").lowercase()
        if (ext in unsupportedExts) {
            cache[path] = null
            return null
        }

        val bmp = try {
            val f = File(path)
            if (!f.exists() || !f.isFile) null
            else if (f.length() > 12L * 1024 * 1024) null
            else {
                val original = ImageIO.read(f) ?: return null
                val thumb = scaleToFit(original, maxThumbSize)
                original.flush()
                thumb.toComposeImageBitmap().also { thumb.flush() }
            }
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: Exception) { null }
        cache[path] = bmp
        return bmp
    }

    override fun loadCoverArt(audioPath: String): ImageBitmap? {
        coverCache[audioPath]?.let { return it }
        val bmp = try {
            val f = File(audioPath)
            if (!f.exists() || f.extension.lowercase() != "mp3") {
                null
            } else {
                val mp3 = Mp3File(f)
                val imageData: ByteArray? = when {
                    // ✅ بدون hasAlbumImage() — نتحقق مباشرة
                    mp3.hasId3v2Tag() && mp3.id3v2Tag.albumImage != null ->
                        mp3.id3v2Tag.albumImage
                    else -> null
                }
                if (imageData == null) null
                else {
                    val img = ImageIO.read(ByteArrayInputStream(imageData))
                    if (img == null) null
                    else {
                        val scaled = scaleToFit(img, 512)
                        img.flush()
                        scaled.toComposeImageBitmap().also { scaled.flush() }
                    }
                }
            }
        } catch (e: Exception) { null }
        coverCache[audioPath] = bmp
        return bmp
    }

    override fun findFirstImageInFolder(folderPath: String): String? {
        folderImageCache[folderPath]?.let { return it }
        val result = try {
            val folder = File(folderPath)
            if (!folder.isDirectory) null
            else folder.listFiles()?.firstOrNull {
                it.isFile &&
                it.extension.lowercase() in imageExts &&
                it.length() < 12L * 1024 * 1024
            }?.absolutePath
        } catch (e: Exception) { null }
        folderImageCache[folderPath] = result
        return result
    }

    private fun scaleToFit(src: BufferedImage, maxSize: Int): BufferedImage {
        val srcW = src.width
        val srcH = src.height
        if (srcW <= 0 || srcH <= 0) return src
        if (srcW <= maxSize && srcH <= maxSize) return src

        val ratio = if (srcW > srcH) maxSize.toDouble() / srcW
                    else maxSize.toDouble() / srcH
        val newW = (srcW * ratio).toInt().coerceAtLeast(1)
        val newH = (srcH * ratio).toInt().coerceAtLeast(1)

        val dst = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
        val g = dst.createGraphics()
        try {
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            )
            g.drawImage(src, 0, 0, newW, newH, null)
        } finally { g.dispose() }
        return dst
    }
}

actual fun createThumbnailLoader(): ThumbnailLoader = JvmThumbnailLoader()