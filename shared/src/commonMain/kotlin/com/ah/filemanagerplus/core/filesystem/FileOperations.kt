package com.ah.filemanagerplus.core.filesystem

enum class OperationType { COPY, MOVE, DELETE, EXTRACT, COMPRESS }

data class OperationProgress(
    val currentFile: String = "",
    val filesProcessed: Int = 0,
    val totalFiles: Int = 0,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val elapsedMillis: Long = 0L,
    val isPreparing: Boolean = false
) {
    val percent: Int
        get() = when {
            totalBytes > 0 -> ((bytesProcessed * 100) / totalBytes).toInt().coerceIn(0, 100)
            totalFiles > 0 -> (filesProcessed * 100 / totalFiles).coerceIn(0, 100)
            else -> 0
        }
}

data class OperationResult(
    val success: Boolean,
    val cancelled: Boolean = false,
    val errorMessage: String? = null
)

data class ConflictInfo(
    val sourcePath: String,
    val targetPath: String,
    val sourceName: String,
    val targetName: String,
    val isDirectory: Boolean,
    val sourceSize: Long,
    val targetSize: Long,
    val sourceModified: Long,
    val targetModified: Long
)

enum class ConflictAction {
    REPLACE,
    RENAME,
    SKIP,
    MERGE,
    CANCEL
}

data class ConflictDecision(
    val action: ConflictAction,
    val applyToAll: Boolean = false
)