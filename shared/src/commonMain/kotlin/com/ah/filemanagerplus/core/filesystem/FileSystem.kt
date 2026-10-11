package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.model.CategoryData
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.data.model.FileItem

interface FileSystem {
    fun listDrives(): List<Drive>
    fun listFiles(path: String): List<FileItem>
    fun exists(path: String): Boolean
    fun parentOf(path: String): String?

    /** يعيد مسار مجلد التصنيف القياسي (Downloads, Pictures, ...) أو null */
    fun categoryFolder(type: CategoryType): String?

    /** يمسح مجلد التصنيف ويحسب العدد والحجم */
    fun scanCategory(type: CategoryType): CategoryData

    /** يعيد مسار التنزيلات الافتراضي */
    fun defaultDownloadsFolder(): String?

    suspend fun copy(
        sources: List<String>,
        destination: String,
        onProgress: (OperationProgress) -> Unit,
        onConflict: suspend (ConflictInfo) -> ConflictDecision,
        isCancelled: () -> Boolean
    ): OperationResult

    suspend fun move(
        sources: List<String>,
        destination: String,
        onProgress: (OperationProgress) -> Unit,
        onConflict: suspend (ConflictInfo) -> ConflictDecision,
        isCancelled: () -> Boolean
    ): OperationResult

    suspend fun delete(
        sources: List<String>,
        permanently: Boolean,
        onProgress: (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): OperationResult

    suspend fun rename(
        oldPath: String,
        newName: String
    ): OperationResult
}

expect fun createFileSystem(): FileSystem