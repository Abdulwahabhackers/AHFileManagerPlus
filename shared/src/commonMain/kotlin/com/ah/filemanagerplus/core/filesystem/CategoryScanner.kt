package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.model.CategoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class CategoryFolder(
    val path: String,
    val name: String,
    val fileCount: Int,
    val totalSize: Long,
    val previewImagePath: String?
)

object CategoryScanner {

    private val SKIP_FOLDERS = setOf(
        "Windows", "\$Recycle.Bin", "System Volume Information",
        "Program Files", "Program Files (x86)", "ProgramData",
        "AppData", "node_modules", ".git", ".gradle",
        ".android", ".androidide", ".idea", ".vscode",
        "build", "target", "out", ".cache",
        "Recovery", "PerfLogs", "\$WinREAgent", "MSOCache",
        ".dart_tool", ".pub-cache", "tmp", "Temp", "flutter"
    )

    private val IMAGE_EXTS = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "ico", "tiff", "heic"
    )

    fun extensionsFor(type: CategoryType): Set<String> = when (type) {
        CategoryType.IMAGES -> IMAGE_EXTS + setOf("svg")
        CategoryType.VIDEOS -> setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp", "ts"
        )
        CategoryType.AUDIO -> setOf(
            "mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "opus", "amr"
        )
        CategoryType.DOCUMENTS -> setOf(
            "pdf", "doc", "docx", "txt", "md", "rtf", "odt",
            "xls", "xlsx", "csv", "ppt", "pptx", "epub"
        )
        CategoryType.DOWNLOADS -> emptySet()
    }

    fun standardFolder(type: CategoryType): String? {
        val home = System.getProperty("user.home") ?: return null
        val name = when (type) {
            CategoryType.IMAGES -> "Pictures"
            CategoryType.VIDEOS -> "Videos"
            CategoryType.AUDIO -> "Music"
            CategoryType.DOCUMENTS -> "Documents"
            CategoryType.DOWNLOADS -> "Downloads"
        }
        val f = File(home, name)
        return if (f.exists() && f.isDirectory) f.absolutePath else null
    }

    fun findFoldersWithTypeFlow(type: CategoryType): Flow<List<CategoryFolder>> = channelFlow {
        val allowed: Set<String> = extensionsFor(type)
        val results = ConcurrentHashMap<String, CategoryFolder>()

        val roots: List<File> = if (type == CategoryType.DOWNLOADS) {
            val dl = standardFolder(type)
            if (dl != null) listOf(File(dl)) else emptyList()
        } else {
            val list = File.listRoots()
            if (list == null) emptyList() else list.filter { it.exists() && it.isDirectory }
        }

        if (roots.isEmpty()) {
            send(emptyList<CategoryFolder>())
            return@channelFlow
        }

        val scanJobs = roots.map { root ->
            launch(Dispatchers.IO) {
                scanRoot(root, allowed, results)
            }
        }

        val emitJob = launch {
            while (true) {
                delay(400)
                val snapshot: List<CategoryFolder> =
                    results.values.sortedByDescending { it.fileCount }.toList()
                send(snapshot)
            }
        }

        scanJobs.joinAll()
        emitJob.cancel()

        val finalList: List<CategoryFolder> =
            results.values.sortedByDescending { it.fileCount }.toList()
        send(finalList)
    }.flowOn(Dispatchers.Default)

    private suspend fun scanRoot(
        root: File,
        allowed: Set<String>,
        results: ConcurrentHashMap<String, CategoryFolder>
    ) = withContext(Dispatchers.IO) {
        val stack = ArrayDeque<File>()
        stack.addLast(root)

        val localVisited = HashSet<String>()

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()

            val key = current.absolutePath.lowercase()
            if (!localVisited.add(key)) continue

            val children = try { current.listFiles() } catch (e: Exception) { null } ?: continue

            var matchCount = 0
            var matchSize = 0L
            var previewImage: String? = null

            for (child in children) {
                if (child.isDirectory) {
                    if (child.name in SKIP_FOLDERS) continue
                    if (child.isHidden) continue
                    stack.addLast(child)
                } else {
                    val ext = try { child.extension.lowercase() } catch (e: Exception) { "" }
                    val isMatch = allowed.isEmpty() || ext in allowed
                    if (isMatch) {
                        matchCount++
                        matchSize += try { child.length() } catch (e: Exception) { 0L }
                    }
                    if (previewImage == null && ext in IMAGE_EXTS) {
                        previewImage = child.absolutePath
                    }
                }
            }

            if (matchCount > 0) {
                results[current.absolutePath] = CategoryFolder(
                    path = current.absolutePath,
                    name = current.name.ifBlank { current.absolutePath },
                    fileCount = matchCount,
                    totalSize = matchSize,
                    previewImagePath = previewImage
                )
            }
        }
    }
}