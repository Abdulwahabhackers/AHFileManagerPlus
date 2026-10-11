package com.ah.filemanagerplus.data.model

data class CategorySize(
    val type: CategoryType,
    val totalSize: Long,
    val fileCount: Int
)

data class AnalysisResult(
    val drive: Drive,
    val usedBytes: Long,
    val freeBytes: Long,
    val totalBytes: Long,
    val categories: List<CategorySize>,
    val largestFolders: List<FileItem>,
    val largestFiles: List<FileItem>,
    val isComplete: Boolean = false
)