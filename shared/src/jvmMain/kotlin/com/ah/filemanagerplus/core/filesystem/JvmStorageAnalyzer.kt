package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.model.AnalysisResult
import com.ah.filemanagerplus.data.model.CategorySize
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class JvmStorageAnalyzer : StorageAnalyzer {

    private val imageExts = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "heic", "tiff"
    )
    private val videoExts = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp"
    )
    private val audioExts = setOf(
        "mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "opus", "amr"
    )
    private val docExts = setOf(
        "pdf", "doc", "docx", "txt", "md", "rtf", "odt",
        "xls", "xlsx", "csv", "ppt", "pptx", "epub"
    )

    private val skipFolders = setOf(
        "Windows", "\$Recycle.Bin", "System Volume Information",
        "Program Files", "Program Files (x86)", "ProgramData",
        "AppData", "node_modules", ".git", ".gradle",
        ".android", ".androidide", ".idea", ".vscode",
        "build", "target", "out", ".cache",
        "Recovery", "PerfLogs", "\$WinREAgent", "MSOCache",
        ".dart_tool", ".pub-cache", "tmp", "Temp", "flutter"
    )

    override fun analyze(drive: Drive): Flow<AnalysisResult> = flow {
        val root = File(drive.rootPath)
        val usedBytes = (drive.totalBytes - drive.freeBytes).coerceAtLeast(0L)

        if (!root.exists() || !root.isDirectory) {
            emit(
                AnalysisResult(
                    drive = drive,
                    usedBytes = usedBytes,
                    freeBytes = drive.freeBytes,
                    totalBytes = drive.totalBytes,
                    categories = emptyCategories(),
                    largestFolders = emptyList(),
                    largestFiles = emptyList(),
                    isComplete = true
                )
            )
            return@flow
        }

        val catSizes = mutableMapOf<CategoryType, Pair<Long, Int>>()
        val largestFiles = mutableListOf<FileItem>()
        val folderSizes = mutableMapOf<String, Long>()
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<Pair<File, File?>>()
        stack.addLast(root to null)

        val maxItems = 15
        val minFileSize = 10L * 1024 * 1024

        var lastEmit = System.currentTimeMillis()

        while (stack.isNotEmpty()) {
            val (current, topFolder) = stack.removeLast()
            val pathKey = try { current.absolutePath.lowercase() }
                          catch (e: Exception) { current.absolutePath }
            if (!visited.add(pathKey)) continue

            val children = try { current.listFiles() }
                           catch (e: Exception) { null } ?: continue

            for (child in children) {
                if (child.isDirectory) {
                    if (child.name in skipFolders) continue
                    if (child.isHidden) continue
                    val newTop = topFolder ?: child
                    stack.addLast(child to newTop)
                } else {
                    val size = try { child.length() } catch (e: Exception) { 0L }
                    val ext = try { child.extension.lowercase() }
                              catch (e: Exception) { "" }

                    // ============ التصنيف ============
                    val cat = when (ext) {
                        in imageExts -> CategoryType.IMAGES
                        in videoExts -> CategoryType.VIDEOS
                        in audioExts -> CategoryType.AUDIO
                        in docExts -> CategoryType.DOCUMENTS
                        else -> null
                    }
                    if (cat != null) {
                        val prev = catSizes[cat] ?: (0L to 0)
                        catSizes[cat] = (prev.first + size) to (prev.second + 1)
                    }

                    // ============ أكبر الملفات ============
                    if (size > minFileSize) {
                        largestFiles.add(
                            FileItem(
                                name = child.name,
                                path = child.absolutePath,
                                isDirectory = false,
                                size = size,
                                lastModified = child.lastModified(),
                                isHidden = child.isHidden,
                                itemCount = 0
                            )
                        )
                        if (largestFiles.size > maxItems * 3) {
                            largestFiles.sortByDescending { it.size }
                            while (largestFiles.size > maxItems) {
                                largestFiles.removeAt(largestFiles.size - 1)
                            }
                        }
                    }

                    // ============ حجم المجلد الأب ============
                    val tf = topFolder ?: current
                    folderSizes[tf.absolutePath] =
                        (folderSizes[tf.absolutePath] ?: 0L) + size
                }
            }

            // ============ إرسال دوري ============
            val now = System.currentTimeMillis()
            if (now - lastEmit > 500) {
                lastEmit = now
                emit(buildResult(drive, usedBytes, catSizes, largestFiles, folderSizes, false))
            }
        }

        emit(buildResult(drive, usedBytes, catSizes, largestFiles, folderSizes, true))
    }.flowOn(Dispatchers.IO)

    private fun buildResult(
        drive: Drive,
        usedBytes: Long,
        catSizes: Map<CategoryType, Pair<Long, Int>>,
        largestFiles: List<FileItem>,
        folderSizes: Map<String, Long>,
        complete: Boolean
    ): AnalysisResult {

        val categories = CategoryType.values().map { type ->
            val (size, count) = catSizes[type] ?: (0L to 0)
            CategorySize(type, size, count)
        }

        val topFolders = folderSizes.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { (path, size) ->
                val f = File(path)
                FileItem(
                    name = f.name.ifBlank { path },
                    path = path,
                    isDirectory = true,
                    size = size,
                    lastModified = if (f.exists()) f.lastModified() else 0L,
                    isHidden = false,
                    itemCount = 0
                )
            }

        val topFiles = largestFiles.sortedByDescending { it.size }.take(15)

        return AnalysisResult(
            drive = drive,
            usedBytes = usedBytes,
            freeBytes = drive.freeBytes,
            totalBytes = drive.totalBytes,
            categories = categories,
            largestFolders = topFolders,
            largestFiles = topFiles,
            isComplete = complete
        )
    }

    private fun emptyCategories(): List<CategorySize> =
        CategoryType.values().map { CategorySize(it, 0L, 0) }
}

actual fun createStorageAnalyzer(): StorageAnalyzer = JvmStorageAnalyzer()