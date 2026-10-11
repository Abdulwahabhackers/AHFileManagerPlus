package com.ah.filemanagerplus.data.model

data class DriveAnalysisItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val itemCount: Int,
    val percent: Double
)