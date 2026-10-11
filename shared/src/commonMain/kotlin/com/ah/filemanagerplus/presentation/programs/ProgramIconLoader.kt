package com.ah.filemanagerplus.presentation.programs

import androidx.compose.ui.graphics.ImageBitmap

interface ProgramIconLoader {
    fun loadIcon(path: String?): ImageBitmap?
}

expect fun createProgramIconLoader(): ProgramIconLoader