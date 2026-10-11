package com.ah.filemanagerplus.data.model

data class InstalledProgram(
    val name: String,
    val version: String,
    val publisher: String,
    val installLocation: String,
    val uninstallString: String,
    val installDate: String,
    val estimatedSize: Long,
    val iconPath: String?
)