package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.model.AnalysisResult
import com.ah.filemanagerplus.data.model.Drive
import kotlinx.coroutines.flow.Flow

interface StorageAnalyzer {
    fun analyze(drive: Drive): Flow<AnalysisResult>
}

expect fun createStorageAnalyzer(): StorageAnalyzer