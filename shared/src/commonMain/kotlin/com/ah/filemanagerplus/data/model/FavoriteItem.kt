package com.ah.filemanagerplus.data.model

data class FavoriteItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val addedAt: Long
)