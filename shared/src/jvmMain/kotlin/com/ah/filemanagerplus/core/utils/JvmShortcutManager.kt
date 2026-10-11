package com.ah.filemanagerplus.core.utils

import java.io.File
import java.util.prefs.Preferences

private const val KEY_ASKED = "shortcut_asked"

private val shortcutPrefs: Preferences by lazy {
    Preferences.userRoot().node("com/ah/filemanagerplus/shortcut")
}

actual fun hasAskedShortcut(): Boolean =
    shortcutPrefs.getBoolean(KEY_ASKED, false)

actual fun markShortcutAsked() {
    try {
        shortcutPrefs.putBoolean(KEY_ASKED, true)
        shortcutPrefs.flush()
    } catch (e: Exception) {
        println("[Shortcut] markAsked error: ${e.message}")
    }
}

private fun getDesktopPath(): File? {
    val candidates = mutableListOf<File>()

    val userProfile = System.getenv("USERPROFILE")
    if (userProfile != null) {
        candidates.add(File(userProfile, "Desktop"))
        candidates.add(File(userProfile, "OneDrive\\Desktop"))
    }

    val home = System.getProperty("user.home")
    if (home != null) {
        candidates.add(File(home, "Desktop"))
    }

    // من PowerShell مباشرة (الأدق مع OneDrive)
    try {
        val proc = ProcessBuilder(
            "powershell", "-NoProfile", "-Command",
            "[Environment]::GetFolderPath('Desktop')"
        ).redirectErrorStream(true).start()

        val output = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor()
        if (output.isNotBlank()) {
            val f = File(output)
            if (f.exists()) candidates.add(0, f)
        }
    } catch (_: Exception) {}

    return candidates.firstOrNull { it.exists() && it.isDirectory }
}

private fun findAppDirectory(): File? {
    try {
        val cmd = ProcessHandle.current().info().command().orElse(null)
        if (cmd != null) {
            val cmdFile = File(cmd)
            if (!cmdFile.name.lowercase().startsWith("java")) {
                val parent = cmdFile.parentFile
                if (parent != null && File(parent, "AHFileManagerPlus.exe").exists()) {
                    return parent
                }
                if (parent != null) return parent
            }
        }
    } catch (_: Exception) {}

    try {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        if (File(userDir, "AHFileManagerPlus.exe").exists()) return userDir
        val parent = userDir.parentFile
        if (parent != null && File(parent, "AHFileManagerPlus.exe").exists()) return parent
    } catch (_: Exception) {}

    return null
}

actual fun createDesktopShortcut(): Boolean {
    return try {
        val desktop = getDesktopPath() ?: return false
        val appDir = findAppDirectory() ?: return false

        val exe = File(appDir, "AHFileManagerPlus.exe")
        if (!exe.exists()) return false

        val icoFile = File(appDir, "icon.ico")
        val iconLocation = if (icoFile.exists()) {
            icoFile.absolutePath
        } else {
            "${exe.absolutePath},0"
        }

        val lnkPath = File(desktop, "AH File Manager Plus.lnk").absolutePath
        File(lnkPath).takeIf { it.exists() }?.delete()

        val vbs = """
            Set oWS = WScript.CreateObject("WScript.Shell")
            sLinkFile = "$lnkPath"
            Set oLink = oWS.CreateShortcut(sLinkFile)
            oLink.TargetPath = "${exe.absolutePath}"
            oLink.WorkingDirectory = "${appDir.absolutePath}"
            oLink.Description = "AH File Manager Plus"
            oLink.IconLocation = "$iconLocation"
            oLink.Save
        """.trimIndent()

        val vbsFile = File.createTempFile("create_shortcut_", ".vbs")
        vbsFile.writeText(vbs, Charsets.UTF_8)

        val proc = ProcessBuilder("wscript.exe", vbsFile.absolutePath).start()
        val exitCode = proc.waitFor()
        vbsFile.delete()

        exitCode == 0 && File(lnkPath).exists()
    } catch (e: Exception) {
        println("[Shortcut] create error: ${e.message}")
        false
    }
}

actual fun deleteDesktopShortcut(): Boolean {
    return try {
        val desktop = getDesktopPath() ?: return false
        val lnk = File(desktop, "AH File Manager Plus.lnk")
        if (lnk.exists()) lnk.delete() else false
    } catch (_: Exception) {
        false
    }
}

actual fun desktopShortcutExists(): Boolean {
    val desktop = getDesktopPath() ?: return false
    return File(desktop, "AH File Manager Plus.lnk").exists()
}