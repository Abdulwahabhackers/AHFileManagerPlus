package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.model.CategoryData
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.data.model.DriveType
import com.ah.filemanagerplus.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import javax.swing.filechooser.FileSystemView

class JvmFileSystem : FileSystem {

    private val fsv: FileSystemView = FileSystemView.getFileSystemView()

    private data class CopyJob(val source: File, var target: File, val isDir: Boolean)
    private data class MoveTarget(val source: File, val target: File, val isDir: Boolean)
    private data class ResolvedConflict(val action: ConflictAction, val target: File)

    // ==================== LIST DRIVES ====================

    override fun listDrives(): List<Drive> {
        val roots = File.listRoots() ?: return emptyList()
        return roots.map { root ->
            Drive(
                rootPath = root.absolutePath,
                label = safeLabel(root),
                totalBytes = safeTotal(root),
                freeBytes = safeFree(root),
                type = detectType(root)
            )
        }
    }

    private fun safeLabel(root: File): String = try {
        fsv.getSystemDisplayName(root)?.trim().orEmpty()
    } catch (e: Exception) { "" }

    private fun safeTotal(root: File): Long = try { root.totalSpace } catch (e: Exception) { 0L }
    private fun safeFree(root: File): Long = try { root.usableSpace } catch (e: Exception) { 0L }

    private fun detectType(root: File): DriveType = try {
        when {
            fsv.isComputerNode(root) -> DriveType.FIXED
            fsv.isFloppyDrive(root) -> DriveType.REMOVABLE
            fsv.isDrive(root) -> DriveType.FIXED
            else -> DriveType.UNKNOWN
        }
    } catch (e: Exception) { DriveType.UNKNOWN }

    // ==================== LIST FILES ====================

    override fun listFiles(path: String): List<FileItem> {
        val dir = File(path)
        if (!dir.isDirectory) return emptyList()
        val children = dir.listFiles() ?: return emptyList()

        return children.asSequence().map { f ->
            val isDir = f.isDirectory
            FileItem(
                name = f.name.ifBlank { f.absolutePath },
                path = f.absolutePath,
                isDirectory = isDir,
                size = if (!isDir) f.length() else 0L,
                lastModified = f.lastModified(),
                isHidden = f.isHidden,
                itemCount = if (isDir) (f.list()?.size ?: 0) else 0
            )
        }.sortedWith(
            compareByDescending<FileItem> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        ).toList()
    }

    override fun exists(path: String): Boolean = File(path).exists()
    override fun parentOf(path: String): String? = File(path).parentFile?.absolutePath

    // ==================== CATEGORIES ====================

    override fun categoryFolder(type: CategoryType): String? {
        val home = System.getProperty("user.home") ?: return null
        val name = when (type) {
            CategoryType.IMAGES -> "Pictures"
            CategoryType.VIDEOS -> "Videos"
            CategoryType.AUDIO -> "Music"
            CategoryType.DOCUMENTS -> "Documents"
            CategoryType.DOWNLOADS -> "Downloads"
        }
        val folder = File(home, name)
        return if (folder.exists() && folder.isDirectory) folder.absolutePath else null
    }

    override fun scanCategory(type: CategoryType): CategoryData {
        var fileCount = 0
        var totalSize = 0L
        var firstFolder: String? = null

        kotlinx.coroutines.runBlocking {
            CategoryScanner.findFoldersWithTypeFlow(type).collect { folders ->
                fileCount = folders.sumOf { it.fileCount }
                totalSize = folders.sumOf { it.totalSize }
                firstFolder = folders.firstOrNull()?.path
            }
        }

        return CategoryData(
            type = type,
            folderPath = firstFolder,
            fileCount = fileCount,
            totalSize = totalSize
        )
    }

    override fun defaultDownloadsFolder(): String? = categoryFolder(CategoryType.DOWNLOADS)

    // ==================== HELPERS ====================

    private fun isSameOrInside(source: File, dest: File): Boolean {
        val s = source.canonicalFile.absolutePath
        val d = dest.canonicalFile.absolutePath
        return s.equals(d, ignoreCase = true) || s.startsWith(d + File.separator, ignoreCase = true)
    }

    // ==================== COPY ====================

    override suspend fun copy(
        sources: List<String>,
        destination: String,
        onProgress: (OperationProgress) -> Unit,
        onConflict: suspend (ConflictInfo) -> ConflictDecision,
        isCancelled: () -> Boolean
    ): OperationResult = withContext(Dispatchers.IO) {
        try {
            onProgress(OperationProgress(isPreparing = true))

            val destDir = File(destination)
            if (!destDir.exists()) destDir.mkdirs()

            val applyAllActions = mutableMapOf<Boolean, ConflictAction>()
            val jobs = mutableListOf<CopyJob>()
            var totalBytes = 0L

            for (srcPath in sources) {
                val src = File(srcPath)
                if (!src.exists()) continue

                val target = File(destDir, src.name)

                if (src.canonicalPath.equals(target.canonicalPath, ignoreCase = true)) continue
                if (src.isDirectory && isSameOrInside(destDir, src)) continue

                if (target.exists()) {
                    val resolved = resolveConflict(src, target, onConflict, applyAllActions)
                    when (resolved.action) {
                        ConflictAction.CANCEL -> return@withContext OperationResult(false, cancelled = true)
                        ConflictAction.SKIP -> continue
                        ConflictAction.REPLACE -> {
                            if (resolved.target.canonicalPath != src.canonicalPath) {
                                if (target.isDirectory) target.deleteRecursively() else target.delete()
                            }
                            collectJobs(src, target, jobs) { totalBytes += it }
                        }
                        ConflictAction.MERGE -> collectJobs(src, target, jobs) { totalBytes += it }
                        ConflictAction.RENAME -> collectJobs(src, resolved.target, jobs) { totalBytes += it }
                    }
                } else {
                    collectJobs(src, target, jobs) { totalBytes += it }
                }
            }

            val totalFiles = jobs.size
            val startTime = System.currentTimeMillis()
            var processedFiles = 0
            var processedBytes = 0L
            var lastNotify = 0L

            for (job in jobs) {
                if (isCancelled()) return@withContext OperationResult(false, cancelled = true)

                if (job.isDir) {
                    job.target.mkdirs()
                    processedFiles++
                    continue
                }

                if (job.source.canonicalPath.equals(job.target.canonicalPath, ignoreCase = true)) {
                    processedFiles++
                    continue
                }

                if (job.target.exists() && job.target.isFile) {
                    val resolved = resolveConflict(job.source, job.target, onConflict, applyAllActions)
                    when (resolved.action) {
                        ConflictAction.CANCEL -> return@withContext OperationResult(false, cancelled = true)
                        ConflictAction.SKIP -> { processedFiles++; continue }
                        ConflictAction.REPLACE, ConflictAction.MERGE -> job.target.delete()
                        ConflictAction.RENAME -> job.target = resolved.target
                    }
                }

                job.target.parentFile?.mkdirs()

                job.source.inputStream().use { input ->
                    job.target.outputStream().use { output ->
                        val buf = ByteArray(256 * 1024)
                        var r: Int
                        while (input.read(buf).also { r = it } > 0) {
                            if (isCancelled()) return@withContext OperationResult(false, cancelled = true)
                            output.write(buf, 0, r)
                            processedBytes += r
                            val now = System.currentTimeMillis()
                            if (now - lastNotify > 80) {
                                lastNotify = now
                                onProgress(progressOf(
                                    job.source.name, processedFiles, totalFiles,
                                    processedBytes, totalBytes, startTime
                                ))
                            }
                        }
                    }
                }
                job.target.setLastModified(job.source.lastModified())
                processedFiles++
                onProgress(progressOf(
                    job.source.name, processedFiles, totalFiles,
                    processedBytes, totalBytes, startTime
                ))
            }

            OperationResult(true)
        } catch (e: Exception) {
            OperationResult(false, errorMessage = e.message ?: e.toString())
        }
    }

    // ==================== MOVE ====================

    override suspend fun move(
        sources: List<String>,
        destination: String,
        onProgress: (OperationProgress) -> Unit,
        onConflict: suspend (ConflictInfo) -> ConflictDecision,
        isCancelled: () -> Boolean
    ): OperationResult = withContext(Dispatchers.IO) {
        try {
            onProgress(OperationProgress(isPreparing = true))

            val destDir = File(destination)
            if (!destDir.exists()) destDir.mkdirs()

            val applyAllActions = mutableMapOf<Boolean, ConflictAction>()
            val targets = mutableListOf<MoveTarget>()

            for (srcPath in sources) {
                val src = File(srcPath)
                if (!src.exists()) continue
                var target = File(destDir, src.name)

                if (src.canonicalPath.equals(target.canonicalPath, ignoreCase = true)) continue
                if (src.isDirectory && isSameOrInside(destDir, src)) continue

                if (target.exists()) {
                    val resolved = resolveConflict(src, target, onConflict, applyAllActions)
                    when (resolved.action) {
                        ConflictAction.CANCEL -> return@withContext OperationResult(false, cancelled = true)
                        ConflictAction.SKIP -> continue
                        ConflictAction.REPLACE -> {
                            if (!resolved.target.canonicalPath.equals(src.canonicalPath, ignoreCase = true)) {
                                target.deleteRecursively()
                            }
                        }
                        ConflictAction.MERGE -> { }
                        ConflictAction.RENAME -> target = resolved.target
                    }
                }
                collectMoveTargets(src, target, targets)
            }

            val dirTargets = targets.filter { it.isDir }
            val fileTargets = targets.filter { !it.isDir }

            val totalFiles = fileTargets.size
            var totalBytes = 0L
            for (t in fileTargets) totalBytes += t.source.length()

            val startTime = System.currentTimeMillis()
            var processed = 0
            var bytesDone = 0L
            var lastNotify = 0L

            for (t in dirTargets) {
                if (isCancelled()) return@withContext OperationResult(false, cancelled = true)
                t.target.mkdirs()
            }

            for (t in fileTargets) {
                if (isCancelled()) return@withContext OperationResult(false, cancelled = true)

                if (t.source.canonicalPath.equals(t.target.canonicalPath, ignoreCase = true)) {
                    processed++
                    continue
                }

                t.target.parentFile?.mkdirs()

                t.source.inputStream().use { input ->
                    t.target.outputStream().use { output ->
                        val buf = ByteArray(256 * 1024)
                        var r: Int
                        while (input.read(buf).also { r = it } > 0) {
                            if (isCancelled()) return@withContext OperationResult(false, cancelled = true)
                            output.write(buf, 0, r)
                            bytesDone += r

                            val now = System.currentTimeMillis()
                            if (now - lastNotify > 80) {
                                lastNotify = now
                                val elapsed = now - startTime
                                val speed = if (elapsed > 0) (bytesDone * 1000L / elapsed) else 0L
                                onProgress(OperationProgress(
                                    currentFile = t.target.name,
                                    filesProcessed = processed,
                                    totalFiles = totalFiles,
                                    bytesProcessed = bytesDone,
                                    totalBytes = totalBytes,
                                    bytesPerSecond = speed,
                                    elapsedMillis = elapsed
                                ))
                            }
                        }
                    }
                }
                val mtime = t.source.lastModified()
                t.source.delete()
                t.target.setLastModified(mtime)
                processed++
            }

            for (srcPath in sources) {
                val src = File(srcPath)
                if (src.exists()) src.deleteRecursively()
            }

            OperationResult(true)
        } catch (e: Exception) {
            OperationResult(false, errorMessage = e.message ?: e.toString())
        }
    }

    private fun collectMoveTargets(src: File, target: File, out: MutableList<MoveTarget>) {
        if (src.isDirectory) {
            out.add(MoveTarget(src, target, isDir = true))
            val children = src.listFiles() ?: return
            for (child in children) {
                collectMoveTargets(child, File(target, child.name), out)
            }
        } else {
            out.add(MoveTarget(src, target, isDir = false))
        }
    }

    // ==================== DELETE ====================

    override suspend fun delete(
        sources: List<String>,
        permanently: Boolean,
        onProgress: (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): OperationResult = withContext(Dispatchers.IO) {
        try {
            onProgress(OperationProgress(isPreparing = true))

            val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
            val canTrash = !permanently && desktop != null &&
                    desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)

            if (canTrash) {
                val total = sources.size
                val startTime = System.currentTimeMillis()
                var processed = 0
                for (path in sources) {
                    if (isCancelled()) return@withContext OperationResult(false, cancelled = true)
                    val f = File(path)
                    if (!f.exists()) continue
                    try { desktop!!.moveToTrash(f) } catch (e: Exception) { f.deleteRecursively() }
                    processed++
                    onProgress(OperationProgress(
                        currentFile = f.name,
                        filesProcessed = processed,
                        totalFiles = total,
                        bytesProcessed = processed.toLong(),
                        totalBytes = total.toLong(),
                        elapsedMillis = System.currentTimeMillis() - startTime
                    ))
                }
                return@withContext OperationResult(true)
            }

            val allFiles = mutableListOf<File>()
            val allDirs = mutableListOf<File>()
            for (path in sources) {
                val f = File(path)
                if (!f.exists()) continue
                collectForDelete(f, allFiles, allDirs)
            }

            val totalFiles = allFiles.size
            val totalBytes = allFiles.sumOf { it.length() }

            val startTime = System.currentTimeMillis()
            var processed = 0
            var bytesDone = 0L
            var lastNotify = 0L

            for (f in allFiles) {
                if (isCancelled()) return@withContext OperationResult(false, cancelled = true)
                val sz = f.length()
                f.delete()
                processed++
                bytesDone += sz

                val now = System.currentTimeMillis()
                if (now - lastNotify > 80) {
                    lastNotify = now
                    val elapsed = now - startTime
                    val speed = if (elapsed > 0) (bytesDone * 1000L / elapsed) else 0L
                    onProgress(OperationProgress(
                        currentFile = f.name,
                        filesProcessed = processed,
                        totalFiles = totalFiles,
                        bytesProcessed = bytesDone,
                        totalBytes = totalBytes,
                        bytesPerSecond = speed,
                        elapsedMillis = elapsed
                    ))
                }
            }

            val sortedDirs = allDirs.sortedByDescending { it.absolutePath.length }
            for (d in sortedDirs) d.delete()

            OperationResult(true)
        } catch (e: Exception) {
            OperationResult(false, errorMessage = e.message ?: e.toString())
        }
    }

    private fun collectForDelete(f: File, files: MutableList<File>, dirs: MutableList<File>) {
        if (f.isFile) { files.add(f); return }
        if (!f.isDirectory) return
        dirs.add(f)
        val children = f.listFiles() ?: return
        for (child in children) collectForDelete(child, files, dirs)
    }

    // ==================== RENAME ====================

    override suspend fun rename(oldPath: String, newName: String): OperationResult =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = newName.trim()
                if (trimmed.isBlank()) return@withContext OperationResult(false, errorMessage = "الاسم فارغ")
                val src = File(oldPath)
                if (!src.exists()) return@withContext OperationResult(false, errorMessage = "العنصر غير موجود")
                if (src.name == trimmed) return@withContext OperationResult(true)

                val parent = src.parentFile
                    ?: return@withContext OperationResult(false, errorMessage = "لا يمكن الوصول للمجلد الأب")

                val target = File(parent, trimmed)
                if (target.exists()) {
                    return@withContext OperationResult(false, errorMessage = "الاسم \"$trimmed\" موجود مسبقاً")
                }

                if (src.renameTo(target)) OperationResult(true)
                else OperationResult(false, errorMessage = "فشل تغيير الاسم")
            } catch (e: Exception) {
                OperationResult(false, errorMessage = e.message ?: e.toString())
            }
        }

    // ==================== Helpers ====================

    private suspend fun resolveConflict(
        src: File,
        target: File,
        onConflict: suspend (ConflictInfo) -> ConflictDecision,
        applyAll: MutableMap<Boolean, ConflictAction>
    ): ResolvedConflict {
        val cached = applyAll[src.isDirectory]
        val action = cached ?: run {
            val info = ConflictInfo(
                sourcePath = src.absolutePath,
                targetPath = target.absolutePath,
                sourceName = src.name,
                targetName = target.name,
                isDirectory = src.isDirectory,
                sourceSize = if (src.isFile) src.length() else 0L,
                targetSize = if (target.isFile) target.length() else 0L,
                sourceModified = src.lastModified(),
                targetModified = target.lastModified()
            )
            val decision = onConflict(info)
            if (decision.applyToAll) applyAll[src.isDirectory] = decision.action
            decision.action
        }

        val resolvedTarget = if (action == ConflictAction.RENAME) findUniqueName(target) else target
        return ResolvedConflict(action, resolvedTarget)
    }

    private fun findUniqueName(target: File): File {
        val parent = target.parentFile ?: return target
        val originalName = target.name

        val dotIndex = originalName.lastIndexOf('.')
        val hasExt = dotIndex > 0 && dotIndex < originalName.length - 1
        val base = if (hasExt) originalName.substring(0, dotIndex) else originalName
        val ext = if (hasExt) originalName.substring(dotIndex) else ""

        var counter = 1
        while (counter < 10_000) {
            val candidate = File(parent, "$base ($counter)$ext")
            if (!candidate.exists()) return candidate
            counter++
        }
        return File(parent, "$base (${System.currentTimeMillis()})$ext")
    }

    private fun collectJobs(src: File, target: File, out: MutableList<CopyJob>, onSize: (Long) -> Unit) {
        if (src.isDirectory) {
            out.add(CopyJob(src, target, isDir = true))
            val children = src.listFiles() ?: return
            for (child in children) collectJobs(child, File(target, child.name), out, onSize)
        } else {
            out.add(CopyJob(src, target, isDir = false))
            onSize(src.length())
        }
    }

    private fun progressOf(
        currentFile: String, filesProcessed: Int, totalFiles: Int,
        bytesProcessed: Long, totalBytes: Long, startTime: Long
    ): OperationProgress {
        val elapsed = System.currentTimeMillis() - startTime
        val speed = if (elapsed > 0) (bytesProcessed * 1000L / elapsed) else 0L
        return OperationProgress(
            currentFile = currentFile, filesProcessed = filesProcessed,
            totalFiles = totalFiles, bytesProcessed = bytesProcessed,
            totalBytes = totalBytes, bytesPerSecond = speed, elapsedMillis = elapsed
        )
    }
}

actual fun createFileSystem(): FileSystem = JvmFileSystem()