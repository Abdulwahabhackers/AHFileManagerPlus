package com.ah.filemanagerplus.data.model

enum class CategoryType {
    IMAGES,
    VIDEOS,
    AUDIO,
    DOCUMENTS,
    DOWNLOADS
}

data class CategoryData(
    val type: CategoryType,
    val folderPath: String?,
    val fileCount: Int,
    val totalSize: Long
) {
    val arabicName: String
        get() = when (type) {
            CategoryType.IMAGES -> "صور"
            CategoryType.VIDEOS -> "فيديو"
            CategoryType.AUDIO -> "صوتي"
            CategoryType.DOCUMENTS -> "وثائق"
            CategoryType.DOWNLOADS -> "تنزيلات"
        }

    val available: Boolean get() = folderPath != null
}