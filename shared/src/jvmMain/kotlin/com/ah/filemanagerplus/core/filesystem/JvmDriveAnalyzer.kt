package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.model.DriveAnalysisItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class JvmDriveAnalyzer : DriveAnalyzer {

    override fun analyzeFolder(path: String): Flow<List<DriveAnalysisItem>> = flow {
        val root = File(path)
        if (!root.exists() || !root.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val children: Array<File> = try { root.listFiles() } catch (e: Exception) { null } ?: emptyArray()

        val items = mutableListOf<DriveAnalysisItem>()

        for (child in children) {
            if (child.isHidden) continue
            val size = if (child.isDirectory) calculateSize(child) else child.length()
            val count = if (child.isDirectory) countFiles(child) else 0
            items.add(
                DriveAnalysisItem(
                    name = child.name,
                    path = child.absolutePath,
                    isDirectory = child.isDirectory,
                    size = size,
                    itemCount = count,
                    percent = 0.0
                )
            )
        }

        val total = items.sumOf { it.size }.coerceAtLeast(1L)

        val withPct = items
            .sortedByDescending { it.size }
            .map { it.copy(percent = it.size.toDouble() / total * 100.0) }

        emit(withPct)
    }.flowOn(Dispatchers.IO)

    private fun calculateSize(f: File): Long {
        var total = 0L
        val stack = ArrayDeque<File>()
        stack.addLast(f)
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast()
            val children = try { cur.listFiles() } catch (e: Exception) { null } ?: continue
            for (c in children) {
                if (c.isDirectory) stack.addLast(c)
                else total += try { c.length() } catch (e: Exception) { 0L }
            }
        }
        return total
    }

    private fun countFiles(f: File): Int {
        var n = 0
        val stack = ArrayDeque<File>()
        stack.addLast(f)
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast()
            val children = try { cur.listFiles() } catch (e: Exception) { null } ?: continue
            for (c in children) {
                if (c.isDirectory) stack.addLast(c) else n++
            }
        }
        return n
    }
}

actual fun createDriveAnalyzer(): DriveAnalyzer = JvmDriveAnalyzer()