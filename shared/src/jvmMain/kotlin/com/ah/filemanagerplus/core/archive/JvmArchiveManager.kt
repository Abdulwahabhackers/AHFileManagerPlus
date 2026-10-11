package com.ah.filemanagerplus.core.archive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class JvmArchiveManager : ArchiveManager {

    override suspend fun listContents(archivePath: String, password: String?): List<ArchiveEntry> =
        withContext(Dispatchers.IO) {
            val f = File(archivePath)
            if (!f.exists()) return@withContext emptyList()

            val ext = f.name.lowercase()
            return@withContext when {
                ext.endsWith(".zip") -> listZip(archivePath, password)
                ext.endsWith(".7z") -> list7z(archivePath)
                ext.endsWith(".tar") -> listTar(archivePath, null)
                ext.endsWith(".tar.gz") || ext.endsWith(".tgz") -> listTar(archivePath, "gz")
                ext.endsWith(".tar.bz2") || ext.endsWith(".tbz2") -> listTar(archivePath, "bz2")
                ext.endsWith(".tar.xz") -> listTar(archivePath, "xz")
                else -> emptyList()
            }
        }

    private fun listZip(path: String, password: String?): List<ArchiveEntry> {
        val zf = if (password.isNullOrBlank()) ZipFile(path)
                 else ZipFile(path, password.toCharArray())
        return zf.fileHeaders.map { h: FileHeader ->
            ArchiveEntry(
                name = h.fileName.substringAfterLast('/').ifBlank { h.fileName },
                path = h.fileName,
                isDirectory = h.isDirectory,
                size = h.uncompressedSize,
                compressedSize = h.compressedSize,
                lastModified = try { h.lastModifiedTime } catch (e: Exception) { 0L }
            )
        }
    }

    private fun list7z(path: String): List<ArchiveEntry> {
        return try {
            SevenZFile(File(path)).use { sz ->
                val list = mutableListOf<ArchiveEntry>()
                var entry = sz.nextEntry
                while (entry != null) {
                    list.add(
                        ArchiveEntry(
                            name = entry.name.substringAfterLast('/').ifBlank { entry.name },
                            path = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            compressedSize = entry.size,
                            lastModified = entry.lastModifiedDate?.time ?: 0L
                        )
                    )
                    entry = sz.nextEntry
                }
                list
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun listTar(path: String, compression: String?): List<ArchiveEntry> {
        return try {
            val fis = FileInputStream(path)
            val buff = BufferedInputStream(fis)
            val stream = when (compression) {
                "gz" -> GzipCompressorInputStream(buff)
                "bz2" -> BZip2CompressorInputStream(buff)
                "xz" -> XZCompressorInputStream(buff)
                else -> buff
            }
            val list = mutableListOf<ArchiveEntry>()
            TarArchiveInputStream(stream).use { tar ->
                var e: TarArchiveEntry? = tar.nextEntry
                while (e != null) {
                    list.add(
                        ArchiveEntry(
                            name = e.name.substringAfterLast('/').ifBlank { e.name },
                            path = e.name,
                            isDirectory = e.isDirectory,
                            size = e.size,
                            compressedSize = e.size,
                            lastModified = e.modTime.time
                        )
                    )
                    e = tar.nextEntry
                }
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun extract(
        archivePath: String,
        destination: String,
        password: String?,
        onProgress: (ArchiveProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val destDir = File(destination)
            if (!destDir.exists()) destDir.mkdirs()

            val contents = listContents(archivePath, password)
            if (contents.isEmpty()) return@withContext false

            val totalFiles = contents.count { !it.isDirectory }
            val totalBytes = contents.sumOf { if (it.isDirectory) 0L else it.size }
            var processed = 0
            var bytesDone = 0L
            val startTime = System.currentTimeMillis()

            val f = File(archivePath)
            val ext = f.name.lowercase()

            if (ext.endsWith(".zip")) {
                val zf = if (password.isNullOrBlank()) ZipFile(archivePath)
                         else ZipFile(archivePath, password.toCharArray())
                for (h in zf.fileHeaders) {
                    if (isCancelled()) return@withContext false
                    val target = File(destDir, h.fileName)
                    if (h.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        zf.getInputStream(h).use { input ->
                            target.outputStream().use { out -> input.copyTo(out, 128 * 1024) }
                        }
                        processed++
                        bytesDone += h.uncompressedSize
                        onProgress(progressOf(h.fileName, processed, totalFiles, bytesDone, totalBytes, startTime))
                    }
                }
            } else if (ext.endsWith(".7z")) {
                SevenZFile(File(archivePath)).use { sz ->
                    var entry = sz.nextEntry
                    while (entry != null) {
                        if (isCancelled()) return@withContext false
                        val target = File(destDir, entry.name)
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { out ->
                                val buf = ByteArray(128 * 1024)
                                var r: Int
                                while (sz.read(buf).also { r = it } > 0) {
                                    out.write(buf, 0, r)
                                    bytesDone += r
                                }
                            }
                            processed++
                            onProgress(progressOf(entry.name, processed, totalFiles, bytesDone, totalBytes, startTime))
                        }
                        entry = sz.nextEntry
                    }
                }
            } else {
                val fis = FileInputStream(archivePath)
                val buff = BufferedInputStream(fis)
                val stream = when {
                    ext.endsWith(".tar.gz") || ext.endsWith(".tgz") -> GzipCompressorInputStream(buff)
                    ext.endsWith(".tar.bz2") || ext.endsWith(".tbz2") -> BZip2CompressorInputStream(buff)
                    ext.endsWith(".tar.xz") -> XZCompressorInputStream(buff)
                    else -> buff
                }
                TarArchiveInputStream(stream).use { tar ->
                    var e: TarArchiveEntry? = tar.nextEntry
                    while (e != null) {
                        if (isCancelled()) return@withContext false
                        val target = File(destDir, e.name)
                        if (e.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { out ->
                                val buf = ByteArray(128 * 1024)
                                var r: Int
                                while (tar.read(buf).also { r = it } > 0) {
                                    out.write(buf, 0, r)
                                    bytesDone += r
                                }
                            }
                            processed++
                            onProgress(progressOf(e.name, processed, totalFiles, bytesDone, totalBytes, startTime))
                        }
                        e = tar.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            println("[Archive] extract error: ${e.message}")
            false
        }
    }

    override suspend fun extractSelected(
        archivePath: String,
        entryNames: List<String>,
        destination: String,
        password: String?,
        onProgress: (ArchiveProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val destDir = File(destination)
            if (!destDir.exists()) destDir.mkdirs()

            val f = File(archivePath)
            val ext = f.name.lowercase()
            val wanted = entryNames.toSet()

            if (ext.endsWith(".zip")) {
                val zf = if (password.isNullOrBlank()) ZipFile(archivePath)
                         else ZipFile(archivePath, password.toCharArray())

                val toExtract = zf.fileHeaders.filter { h ->
                    wanted.any { w -> h.fileName == w || h.fileName.startsWith("$w/") }
                }

                val totalFiles = toExtract.count { !it.isDirectory }
                val totalBytes = toExtract.sumOf { if (it.isDirectory) 0L else it.uncompressedSize }
                var processed = 0
                var bytesDone = 0L
                val startTime = System.currentTimeMillis()

                onProgress(progressOf("جارٍ التحضير...", 0, totalFiles, 0, totalBytes, startTime))

                for (h in toExtract) {
                    if (isCancelled()) return@withContext false
                    val target = File(destDir, h.fileName)
                    if (h.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        zf.getInputStream(h).use { input ->
                            target.outputStream().use { out ->
                                input.copyTo(out, 128 * 1024)
                            }
                        }
                        processed++
                        bytesDone += h.uncompressedSize
                        onProgress(progressOf(h.fileName, processed, totalFiles, bytesDone, totalBytes, startTime))
                    }
                }
                return@withContext true
            }

            if (ext.endsWith(".7z")) {
                val matched = mutableListOf<Pair<String, Long>>()
                SevenZFile(File(archivePath)).use { sz ->
                    var entry = sz.nextEntry
                    while (entry != null) {
                        val match = wanted.any { w -> entry.name == w || entry.name.startsWith("$w/") }
                        if (match && !entry.isDirectory) matched.add(entry.name to entry.size)
                        entry = sz.nextEntry
                    }
                }
                val totalFiles = matched.size
                val totalBytes = matched.sumOf { it.second }
                var processed = 0
                var bytesDone = 0L
                val startTime = System.currentTimeMillis()

                onProgress(progressOf("جارٍ التحضير...", 0, totalFiles, 0, totalBytes, startTime))

                SevenZFile(File(archivePath)).use { sz ->
                    var entry = sz.nextEntry
                    while (entry != null) {
                        if (isCancelled()) return@withContext false
                        val match = wanted.any { w -> entry.name == w || entry.name.startsWith("$w/") }
                        if (match) {
                            val target = File(destDir, entry.name)
                            if (entry.isDirectory) {
                                target.mkdirs()
                            } else {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { out ->
                                    val buf = ByteArray(128 * 1024)
                                    var r: Int
                                    while (sz.read(buf).also { r = it } > 0) out.write(buf, 0, r)
                                }
                                processed++
                                bytesDone += entry.size
                                onProgress(progressOf(entry.name, processed, totalFiles, bytesDone, totalBytes, startTime))
                            }
                        }
                        entry = sz.nextEntry
                    }
                }
                return@withContext true
            }

            val fis = FileInputStream(archivePath)
            val buff = BufferedInputStream(fis)
            val stream = when {
                ext.endsWith(".tar.gz") || ext.endsWith(".tgz") -> GzipCompressorInputStream(buff)
                ext.endsWith(".tar.bz2") || ext.endsWith(".tbz2") -> BZip2CompressorInputStream(buff)
                ext.endsWith(".tar.xz") -> XZCompressorInputStream(buff)
                else -> buff
            }
            var totalFiles = 0
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()

            TarArchiveInputStream(stream).use { tar ->
                var e: TarArchiveEntry? = tar.nextEntry
                while (e != null) {
                    if (isCancelled()) return@withContext false
                    val match = wanted.any { w -> e.name == w || e.name.startsWith("$w/") }
                    if (match) {
                        val target = File(destDir, e.name)
                        if (e.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { out ->
                                val buf = ByteArray(128 * 1024)
                                var r: Int
                                while (tar.read(buf).also { r = it } > 0) out.write(buf, 0, r)
                            }
                            totalFiles++
                            totalBytes += e.size
                            onProgress(progressOf(e.name, totalFiles, totalFiles, totalBytes, totalBytes, startTime))
                        }
                    }
                    e = tar.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            println("[Archive] extractSelected error: ${e.message}")
            false
        }
    }

    override suspend fun compress(
        sources: List<String>,
        destinationArchive: String,
        format: ArchiveFormat,
        level: CompressionLevel,
        password: String?,
        onProgress: (ArchiveProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            compressZip(sources, destinationArchive, level, password, onProgress, isCancelled)
        } catch (e: Exception) {
            println("[Archive] compress error: ${e.message}")
            false
        }
    }

    private fun compressZip(
        sources: List<String>,
        destPath: String,
        level: CompressionLevel,
        password: String?,
        onProgress: (ArchiveProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        data class Entry(val file: File, val relPath: String)
        val entries = mutableListOf<Entry>()

        for (src in sources) {
            val f = File(src)
            if (f.isDirectory) {
                val baseName = f.name
                f.walkTopDown().forEach { child ->
                    if (child.isFile) {
                        val rel = "$baseName/" + child.relativeTo(f).path.replace('\\', '/')
                        entries.add(Entry(child, rel))
                    }
                }
            } else {
                entries.add(Entry(f, f.name))
            }
        }

        val totalFiles = entries.size
        val totalBytes = entries.sumOf { it.file.length() }
        var processed = 0
        var bytesDone = 0L
        val startTime = System.currentTimeMillis()

        onProgress(progressOf("جارٍ التحضير...", 0, totalFiles, 0, totalBytes, startTime))

        if (entries.isEmpty()) {
            return false
        }

        val zf = if (password.isNullOrBlank()) ZipFile(destPath)
                 else ZipFile(destPath, password.toCharArray())

        val params = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            compressionLevel = mapLevel(level)
            if (!password.isNullOrBlank()) {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
        }

        for (entry in entries) {
            if (isCancelled()) return false
            params.fileNameInZip = entry.relPath
            zf.addFile(entry.file, params)
            processed++
            bytesDone += entry.file.length()
            onProgress(progressOf(entry.relPath, processed, totalFiles, bytesDone, totalBytes, startTime))
        }

        return true
    }

    private fun mapLevel(level: CompressionLevel): net.lingala.zip4j.model.enums.CompressionLevel {
        return when (level) {
            CompressionLevel.NONE -> net.lingala.zip4j.model.enums.CompressionLevel.NO_COMPRESSION
            CompressionLevel.FASTEST -> net.lingala.zip4j.model.enums.CompressionLevel.FASTEST
            CompressionLevel.FAST -> net.lingala.zip4j.model.enums.CompressionLevel.FAST
            CompressionLevel.NORMAL -> net.lingala.zip4j.model.enums.CompressionLevel.NORMAL
            CompressionLevel.MAXIMUM -> net.lingala.zip4j.model.enums.CompressionLevel.MAXIMUM
            CompressionLevel.ULTRA -> net.lingala.zip4j.model.enums.CompressionLevel.MAXIMUM
        }
    }

    override suspend fun needsPassword(archivePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val f = File(archivePath)
            if (!f.name.lowercase().endsWith(".zip")) return@withContext false
            ZipFile(archivePath).isEncrypted
        } catch (e: Exception) { false }
    }

    private fun progressOf(
        currentFile: String,
        filesProcessed: Int,
        totalFiles: Int,
        bytesProcessed: Long,
        totalBytes: Long,
        startTime: Long
    ): ArchiveProgress {
        val percent = when {
            totalBytes > 0 -> ((bytesProcessed * 100) / totalBytes).toInt()
            totalFiles > 0 -> (filesProcessed * 100 / totalFiles)
            else -> 0
        }.coerceIn(0, 100)
        return ArchiveProgress(
            currentFile = currentFile,
            filesProcessed = filesProcessed,
            totalFiles = totalFiles,
            bytesProcessed = bytesProcessed,
            totalBytes = totalBytes,
            percent = percent
        )
    }
}

actual fun createArchiveManager(): ArchiveManager = JvmArchiveManager()