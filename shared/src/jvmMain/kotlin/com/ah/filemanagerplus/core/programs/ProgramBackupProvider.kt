package com.ah.filemanagerplus.core.programs

import com.ah.filemanagerplus.data.model.InstalledProgram

actual suspend fun runProgramBackup(
    program: InstalledProgram,
    onProgress: (String) -> Unit
): String? = ProgramBackupManager.backup(program, onProgress)