package com.ah.filemanagerplus.presentation.browser

import androidx.compose.ui.graphics.ImageBitmap

interface ThumbnailLoader {
    fun loadImage(path: String): ImageBitmap?
    fun findFirstImageInFolder(folderPath: String): String?

    /** يحمّل غلاف ملف صوتي (ID3 APIC) — null لو ما فيه */
    fun loadCoverArt(audioPath: String): ImageBitmap?
}

expect fun createThumbnailLoader(): ThumbnailLoader