package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object SearchEngine {

    private val SKIP_FOLDERS = setOf(
        "Windows", "\$Recycle.Bin", "System Volume Information",
        "Program Files", "Program Files (x86)", "ProgramData",
        "AppData", "node_modules", ".git", ".gradle",
        ".android", ".androidide", ".idea", ".vscode",
        "build", "target", "out", ".cache",
        "Recovery", "PerfLogs", "\$WinREAgent", "MSOCache"
    )

    /**
     * بحث متدفق (Flow) — يصدر نتائج تدريجياً أثناء المسح.
     * يشتغل على IO Dispatcher.
     */
    fun searchFlow(
        root: String,
        query: String,
        maxResults: Int = 1000
    ): Flow<List<FileItem>> = flow {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val rootFile = File(root)
        if (!rootFile.exists() || !rootFile.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val results = mutableListOf<FileItem>()
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<File>()
        stack.addLast(rootFile)

        var lastEmitTime = 0L
        var lastEmitCount = 0

        while (stack.isNotEmpty() && results.size < maxResults) {
            val current = stack.removeLast()
            val path = try { current.canonicalPath } catch (e: Exception) { current.absolutePath }
            if (!visited.add(path)) continue

            val children = try { current.listFiles() } catch (e: Exception) { null } ?: continue

            for (child in children) {
                if (results.size >= maxResults) break
                val nameLower = child.name.lowercase()

                if (nameLower.contains(q)) {
                    results.add(
                        FileItem(
                            name = child.name,
                            path = child.absolutePath,
                            isDirectory = child.isDirectory,
                            size = if (child.isFile) child.length() else 0L,
                            lastModified = child.lastModified(),
                            isHidden = child.isHidden,
                            itemCount = 0
                        )
                    )
                }

                if (child.isDirectory && child.name !in SKIP_FOLDERS && !child.isHidden) {
                    stack.addLast(child)
                }
            }

            // كل 200ms أو كل 20 نتيجة جديدة — أصدر دفعة
            val now = System.currentTimeMillis()
            if (results.size > lastEmitCount &&
                (now - lastEmitTime > 200 || results.size - lastEmitCount >= 20)
            ) {
                lastEmitTime = now
                lastEmitCount = results.size
                emit(results.sortedWith(sortComparator()).toList())
            }
        }

        // الإصدار النهائي
        emit(results.sortedWith(sortComparator()).toList())
    }.flowOn(Dispatchers.IO)

    private fun sortComparator() = compareByDescending<FileItem> { it.isDirectory }
        .thenBy { it.name.lowercase() }
}