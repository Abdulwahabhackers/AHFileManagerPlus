package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.model.DriveAnalysisItem
import kotlinx.coroutines.flow.Flow

interface DriveAnalyzer {
    fun analyzeFolder(path: String): Flow<List<DriveAnalysisItem>>
}

expect fun createDriveAnalyzer(): DriveAnalyzer