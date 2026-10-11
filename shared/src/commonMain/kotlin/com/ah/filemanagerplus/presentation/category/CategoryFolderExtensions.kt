package com.ah.filemanagerplus.presentation.category

import com.ah.filemanagerplus.core.filesystem.CategoryFolder
import com.ah.filemanagerplus.data.model.FileItem

fun CategoryFolder.toFileItem(): FileItem = FileItem(
    name = name,
    path = path,
    isDirectory = true,
    size = totalSize,
    lastModified = 0L,
    isHidden = false,
    itemCount = fileCount
)