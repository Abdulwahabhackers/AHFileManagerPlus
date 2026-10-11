package com.ah.filemanagerplus.core.programs

import com.ah.filemanagerplus.data.model.InstalledProgram

actual fun scanInstalledPrograms(): List<InstalledProgram> = ProgramsScanner.scan()