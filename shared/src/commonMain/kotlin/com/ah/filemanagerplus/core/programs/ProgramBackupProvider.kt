package com.ah.filemanagerplus.core.programs

import com.ah.filemanagerplus.data.model.InstalledProgram

expect suspend fun runProgramBackup(
    program: InstalledProgram,
    onProgress: (String) -> Unit
): String?