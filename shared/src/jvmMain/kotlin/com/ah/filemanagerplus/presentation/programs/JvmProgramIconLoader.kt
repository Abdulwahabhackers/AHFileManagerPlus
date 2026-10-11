package com.ah.filemanagerplus.presentation.programs

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView

class JvmProgramIconLoader : ProgramIconLoader {

    private val fsv: FileSystemView = FileSystemView.getFileSystemView()
    private val cache = object : LinkedHashMap<String, ImageBitmap?>(80, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, ImageBitmap?>?
        ): Boolean = size > 250
    }

    override fun loadIcon(path: String?): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        cache[path]?.let { return it }

        val bitmap = try {
            val cleanPath = path.substringBefore(',').trim().trim('"')
            val f = File(cleanPath)
            if (!f.exists()) {
                cache[path] = null
                return null
            }

            val icon: Icon = fsv.getSystemIcon(f) ?: run {
                cache[path] = null
                return null
            }

            val w = icon.iconWidth.coerceAtLeast(1)
            val h = icon.iconHeight.coerceAtLeast(1)
            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            icon.paintIcon(null, g, 0, 0)
            g.dispose()

            img.toComposeImageBitmap().also { img.flush() }
        } catch (e: Exception) {
            null
        }
        cache[path] = bitmap
        return bitmap
    }
}

actual fun createProgramIconLoader(): ProgramIconLoader = JvmProgramIconLoader()