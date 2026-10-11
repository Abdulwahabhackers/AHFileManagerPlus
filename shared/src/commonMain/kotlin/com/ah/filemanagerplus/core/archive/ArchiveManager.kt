package com.ah.filemanagerplus.core.archive

data class ArchiveEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val lastModified: Long
)

enum class CompressionLevel(val label: String, val value: Int) {
    NONE("بدون ضغط", 0),
    FASTEST("الأسرع", 1),
    FAST("سريع", 3),
    NORMAL("عادي", 5),
    MAXIMUM("الأقصى", 7),
    ULTRA("الفائق", 9)
}

enum class ArchiveFormat(val label: String, val extension: String) {
    ZIP("zip", ".zip"),
    SEVEN_ZIP("7z", ".7z"),
    TAR("tar", ".tar"),
    TAR_GZ("tar.gz", ".tar.gz"),
    TAR_BZ2("tar.bz2", ".tar.bz2"),
    TAR_XZ("tar.xz", ".tar.xz")
}

data class ArchiveProgress(
    val currentFile: String = "",
    val filesProcessed: Int = 0,
    val totalFiles: Int = 0,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Int = 0
)

interface ArchiveManager {
    suspend fun listContents(archivePath: String, password: String? = null): List<ArchiveEntry>

    suspend fun extract(
        archivePath: String,
        destination: String,
        password: String? = null,
        onProgress: (ArchiveProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean

    suspend fun extractSelected(
        archivePath: String,
        entryNames: List<String>,
        destination: String,
        password: String? = null,
        onProgress: (ArchiveProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean

    suspend fun compress(
        sources: List<String>,
        destinationArchive: String,
        format: ArchiveFormat,
        level: CompressionLevel,
        password: String? = null,
        onProgress: (ArchiveProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean

    suspend fun needsPassword(archivePath: String): Boolean
}

expect fun createArchiveManager(): ArchiveManager