package com.ah.filemanagerplus.core.programs

import com.ah.filemanagerplus.data.model.InstalledProgram
import java.io.File

object ProgramsScanner {

    fun scan(): List<InstalledProgram> {
        val results = mutableListOf<InstalledProgram>()

        val keys = listOf(
            "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        )

        for (keyPath in keys) {
            try {
                scanKey(keyPath, results)
            } catch (e: Exception) {
                println("[Programs] error on $keyPath: ${e.message}")
            }
        }

        return results
            .distinctBy { "${it.name}|${it.version}" }
            .filter { it.name.isNotBlank() }
            .sortedBy { it.name.lowercase() }
    }

    private fun scanKey(rootKey: String, out: MutableList<InstalledProgram>) {
        val process = ProcessBuilder(
            "reg", "query", rootKey, "/s"
        ).redirectErrorStream(true).start()

        val lines = process.inputStream.bufferedReader().readLines()
        process.waitFor()

        var currentKey: String? = null
        val buffer = mutableMapOf<String, String>()

        fun commit() {
            if (currentKey != null && buffer.isNotEmpty()) {
                val name = buffer["DisplayName"] ?: return
                if (name.isBlank()) { buffer.clear(); return }
                if (buffer["SystemComponent"] == "0x1") { buffer.clear(); return }

                val sizeKb = buffer["EstimatedSize"]?.toLongOrNull() ?: 0L
                out.add(
                    InstalledProgram(
                        name = name,
                        version = buffer["DisplayVersion"].orEmpty(),
                        publisher = buffer["Publisher"].orEmpty(),
                        installLocation = buffer["InstallLocation"].orEmpty(),
                        uninstallString = buffer["UninstallString"].orEmpty(),
                        installDate = buffer["InstallDate"].orEmpty(),
                        estimatedSize = sizeKb * 1024,
                        iconPath = buffer["DisplayIcon"]
                            ?.substringBefore(',')
                            ?.trim('"')
                            ?.takeIf { it.isNotBlank() && File(it).exists() }
                    )
                )
            }
            buffer.clear()
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("HKEY_")) {
                commit()
                currentKey = trimmed
                continue
            }
            val match = Regex("""^\s*(\S+)\s+REG_\w+\s+(.*)$""").find(trimmed)
            if (match != null && currentKey != null) {
                buffer[match.groupValues[1]] = match.groupValues[2].trim()
            }
        }
        commit()
    }
}