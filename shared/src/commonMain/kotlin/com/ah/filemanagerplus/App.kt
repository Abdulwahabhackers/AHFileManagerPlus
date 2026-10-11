package com.ah.filemanagerplus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ah.filemanagerplus.core.archive.createArchiveManager
import com.ah.filemanagerplus.core.audio.createAudioPlayer
import com.ah.filemanagerplus.core.filesystem.CategoryScanner
import com.ah.filemanagerplus.core.filesystem.OperationProgress
import com.ah.filemanagerplus.core.filesystem.OperationType
import com.ah.filemanagerplus.core.filesystem.SearchEngine
import com.ah.filemanagerplus.core.filesystem.createDriveAnalyzer
import com.ah.filemanagerplus.core.filesystem.createFileSystem
import com.ah.filemanagerplus.core.filesystem.createStorageAnalyzer
import com.ah.filemanagerplus.core.programs.runProgramBackup
import com.ah.filemanagerplus.core.programs.scanInstalledPrograms
import com.ah.filemanagerplus.core.utils.createDesktopShortcut
import com.ah.filemanagerplus.core.utils.desktopShortcutExists
import com.ah.filemanagerplus.core.utils.hasAskedShortcut
import com.ah.filemanagerplus.core.utils.markShortcutAsked
import com.ah.filemanagerplus.core.video.createVideoPlayer
import com.ah.filemanagerplus.data.AppState
import com.ah.filemanagerplus.data.ClipboardAction
import com.ah.filemanagerplus.data.Screen
import com.ah.filemanagerplus.data.createFavoritesStorage
import com.ah.filemanagerplus.data.createSettingsStorage
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.analyze.AnalyzeScreen
import com.ah.filemanagerplus.presentation.analyze.DriveAnalysisScreen
import com.ah.filemanagerplus.presentation.archive.ArchiveConfig
import com.ah.filemanagerplus.presentation.archive.ArchiveViewerScreen
import com.ah.filemanagerplus.presentation.archive.CreateArchiveDialog
import com.ah.filemanagerplus.presentation.browser.BrowserScreen
import com.ah.filemanagerplus.presentation.category.CategoryBrowserScreen
import com.ah.filemanagerplus.presentation.category.toFileItem
import com.ah.filemanagerplus.presentation.components.AppDrawer
import com.ah.filemanagerplus.presentation.components.ClipboardBottomBar
import com.ah.filemanagerplus.presentation.components.ConflictDialog
import com.ah.filemanagerplus.presentation.components.ConfirmDeleteDialog
import com.ah.filemanagerplus.presentation.components.MiniPlayer
import com.ah.filemanagerplus.presentation.components.OperationBadge
import com.ah.filemanagerplus.presentation.components.RenameDialog
import com.ah.filemanagerplus.presentation.components.ShortcutDialog
import com.ah.filemanagerplus.presentation.home.HomeScreen
import com.ah.filemanagerplus.presentation.onboarding.OnboardingScreen
import com.ah.filemanagerplus.presentation.operations.OperationsDialog
import com.ah.filemanagerplus.presentation.operations.OperationsScreen
import com.ah.filemanagerplus.presentation.programs.ProgramDetailsDialog
import com.ah.filemanagerplus.presentation.programs.ProgramsScreen
import com.ah.filemanagerplus.presentation.search.SearchScreen
import com.ah.filemanagerplus.presentation.settings.AboutScreen
import com.ah.filemanagerplus.presentation.settings.SettingsScreen
import com.ah.filemanagerplus.presentation.theme.AHFileManagerTheme
import com.ah.filemanagerplus.presentation.viewer.ImageViewerScreen
import com.ah.filemanagerplus.presentation.viewer.MusicPlayerScreen
import com.ah.filemanagerplus.presentation.viewer.TextViewerScreen
import com.ah.filemanagerplus.presentation.viewer.VideoPlayerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val IMAGE_EXTS = setOf(
    "jpg", "jpeg", "png", "gif", "bmp", "webp", "ico", "tiff", "heic"
)

private val TEXT_EXTS = setOf(
    "txt", "md", "json", "xml", "kt", "kts",
    "java", "gradle", "properties", "log",
    "csv", "ini", "yml", "yaml", "html",
    "css", "js", "py", "c", "cpp", "h", "sh",
    "bat", "cmd", "conf", "config", "toml"
)

private val AUDIO_EXTS = setOf(
    "mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "opus", "amr"
)

private val VIDEO_EXTS = setOf(
    "mp4", "m4v", "mov", "flv", "hls", "m3u8"
)

private val ARCHIVE_EXTS = setOf(
    "zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz"
)

@Composable
fun App() {
    val settingsStorage = remember { createSettingsStorage() }
    val favoritesStorage = remember { createFavoritesStorage() }
    val appState = remember { AppState() }

    LaunchedEffect(Unit) {
        appState.initSettings(settingsStorage)
        appState.initFavorites(favoritesStorage)
    }

    LaunchedEffect(appState.settings.onboardingSeen) {
        appState.checkOnboarding()
    }

    AHFileManagerTheme(themeMode = appState.settings.themeMode) {
        val fileSystem = remember { createFileSystem() }
        val scope = rememberCoroutineScope()
        val audioPlayer = remember { createAudioPlayer() }
        val videoPlayer = remember { createVideoPlayer() }
        val storageAnalyzer = remember { createStorageAnalyzer() }
        val driveAnalyzer = remember { createDriveAnalyzer() }
        val archiveManager = remember { createArchiveManager() }

        var showShortcutDialog by remember {
            mutableStateOf(!hasAskedShortcut() && !desktopShortcutExists())
        }

        val drawerState = rememberDrawerState(DrawerValue.Closed)

        LaunchedEffect(appState.drawerOpen) {
            if (appState.drawerOpen) drawerState.open() else drawerState.close()
        }

        LaunchedEffect(drawerState.isOpen) {
            if (!drawerState.isOpen && appState.drawerOpen) {
                appState.closeDrawer()
            }
        }

        LaunchedEffect(Unit) {
            appState.updateLoading(true)
            appState.updateDrives(fileSystem.listDrives())
            appState.updateLoading(false)
        }

        LaunchedEffect(Unit) {
            appState.updateScanningCategories(true)
            val cats = withContext(Dispatchers.IO) {
                CategoryType.values().map { fileSystem.scanCategory(it) }
            }
            appState.updateCategories(cats)
            appState.updateScanningCategories(false)
        }

        // ==================== مسح البرامج المثبتة ====================
        LaunchedEffect(appState.currentScreen) {
            if (appState.currentScreen != Screen.PROGRAMS) return@LaunchedEffect
            if (!appState.isScanningPrograms) return@LaunchedEffect
            val programs = withContext(Dispatchers.IO) {
                try { scanInstalledPrograms() } catch (e: Exception) { emptyList() }
            }
            appState.updatePrograms(programs)
        }

        LaunchedEffect(appState.currentPath, appState.filesRevision) {
            val path = appState.currentPath
            if (path != null) {
                appState.updateFiles(fileSystem.listFiles(path))
            }
        }

        LaunchedEffect(appState.categoryScanType, appState.currentScreen) {
            if (appState.currentScreen != Screen.CATEGORY_BROWSER) return@LaunchedEffect
            val type = appState.categoryScanType ?: return@LaunchedEffect
            appState.updateScanningCategoryFolders(true)
            CategoryScanner.findFoldersWithTypeFlow(type).collect { batch ->
                appState.updateCategoryFolders(batch)
            }
            appState.updateScanningCategoryFolders(false)
        }

        // ==================== تحليل التخزين العام ====================
        LaunchedEffect(appState.currentScreen, appState.analysisDrive) {
            if (appState.currentScreen != Screen.ANALYZE) return@LaunchedEffect
            val drive = appState.analysisDrive ?: return@LaunchedEffect
            storageAnalyzer.analyze(drive).collect { result ->
                appState.updateAnalysis(result)
                if (result.isComplete) appState.updateAnalyzing(false)
            }
        }

        // ==================== تحليل القرص ====================
        LaunchedEffect(appState.currentScreen, appState.driveAnalysisPath) {
            if (appState.currentScreen != Screen.DRIVE_ANALYSIS) return@LaunchedEffect
            if (!appState.isDriveAnalyzing) return@LaunchedEffect
            val path = appState.driveAnalysisPath ?: return@LaunchedEffect
            driveAnalyzer.analyzeFolder(path).collect { items ->
                appState.updateDriveAnalysis(items)
                appState.updateDriveAnalyzing(false)
            }
        }

        // ==================== تحميل الأرشيف ====================
        LaunchedEffect(appState.currentScreen, appState.archivePath, appState.archivePassword) {
            if (appState.currentScreen != Screen.ARCHIVE_VIEWER) return@LaunchedEffect
            val path = appState.archivePath ?: return@LaunchedEffect

            val needs = archiveManager.needsPassword(path)
            if (needs && appState.archivePassword == null) {
                appState.updateArchiveNeedsPassword(true)
                appState.updateArchiveLoading(false)
                return@LaunchedEffect
            }
            val entries = archiveManager.listContents(path, appState.archivePassword)
            appState.updateArchiveEntries(entries)
        }

        LaunchedEffect(appState.searchQuery, appState.searchRootPath, appState.currentScreen) {
            if (appState.currentScreen != Screen.SEARCH) return@LaunchedEffect
            val q = appState.searchQuery
            val root = appState.searchRootPath
            if (q.isBlank() || root == null) {
                appState.updateSearchResults(emptyList())
                appState.updateSearching(false)
                return@LaunchedEffect
            }
            delay(120)
            appState.updateSearching(true)
            appState.updateSearchResults(emptyList())
            SearchEngine.searchFlow(root, q, maxResults = 800).collect { batch ->
                appState.updateSearchResults(batch)
            }
            appState.updateSearching(false)
        }

        LaunchedEffect(appState.activeOperations.map { it.isFinished }) {
            if (appState.allOperationsFinished) {
                delay(3000)
                appState.clearFinishedOperations()
            }
        }

        LaunchedEffect(appState.audioCurrentFile) {
            while (appState.audioCurrentFile != null) {
                delay(400)
                appState.updateAudioPosition(audioPlayer.getPositionMs())
                appState.updateAudioDuration(audioPlayer.getDurationMs())
                appState.updateAudioPlaying(audioPlayer.isPlaying())
            }
        }

        LaunchedEffect(appState.videoCurrentFile, appState.currentScreen) {
            while (appState.currentScreen == Screen.VIDEO_PLAYER) {
                delay(400)
                appState.updateVideoPosition(videoPlayer.getPositionMs())
                appState.updateVideoDuration(videoPlayer.getDurationMs())
                appState.updateVideoPlaying(videoPlayer.isPlaying())
            }
        }

        LaunchedEffect(Unit) {
            videoPlayer.setOnError { msg ->
                scope.launch { appState.updateVideoError(msg) }
            }
        }

        LaunchedEffect(Unit) {
            audioPlayer.setOnComplete {
                scope.launch {
                    val next = appState.musicOnTrackFinished()
                    if (next != null) {
                        audioPlayer.play(next.path)
                        appState.updateAudioCurrentFile(next)
                        appState.updateAudioPlaying(true)
                        appState.updateAudioPosition(0L)
                        appState.updateAudioDuration(audioPlayer.getDurationMs())
                    } else {
                        audioPlayer.stop()
                        appState.clearAudio()
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                audioPlayer.release()
                videoPlayer.release()
            }
        }

        val doPaste: () -> Unit = {
            val items = appState.clipboardItems
            val action = appState.clipboardAction
            val dest = appState.currentPath
            if (items.isNotEmpty() && action != null && dest != null) {
                val opType = when (action) {
                    ClipboardAction.COPY -> OperationType.COPY
                    ClipboardAction.MOVE -> OperationType.MOVE
                }
                val opId = appState.beginOperation(opType)
                scope.launch {
                    val conflictHandler: suspend (com.ah.filemanagerplus.core.filesystem.ConflictInfo)
                    -> com.ah.filemanagerplus.core.filesystem.ConflictDecision = { info ->
                        appState.requestConflictDecision(info)
                    }
                    val result = when (action) {
                        ClipboardAction.COPY -> fileSystem.copy(
                            sources = items, destination = dest,
                            onProgress = { appState.updateOperationProgress(opId, it) },
                            onConflict = conflictHandler,
                            isCancelled = { appState.isOperationCancelled(opId) }
                        )
                        ClipboardAction.MOVE -> fileSystem.move(
                            sources = items, destination = dest,
                            onProgress = { appState.updateOperationProgress(opId, it) },
                            onConflict = conflictHandler,
                            isCancelled = { appState.isOperationCancelled(opId) }
                        )
                    }
                    appState.markOperationFinished(opId, result.errorMessage)
                    appState.cancelClipboard()
                    appState.refreshFiles()
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = appState.currentScreen == Screen.HOME ||
                    appState.currentScreen == Screen.BROWSER ||
                    appState.currentScreen == Screen.SEARCH ||
                    appState.currentScreen == Screen.CATEGORY_BROWSER ||
                    appState.currentScreen == Screen.SETTINGS ||
                    appState.currentScreen == Screen.ABOUT ||
                    appState.currentScreen == Screen.ANALYZE ||
                    appState.currentScreen == Screen.DRIVE_ANALYSIS ||
                    appState.currentScreen == Screen.ARCHIVE_VIEWER ||
                    appState.currentScreen == Screen.PROGRAMS,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background
                ) {
                    AppDrawer(
                        drives = appState.drives,
                        favorites = appState.favorites,
                        recentFiles = appState.recentFiles,
                        onHomeClick = {
                            appState.closeBrowser()
                            appState.closeDrawer()
                        },
                        onDriveClick = {
                            appState.openDrive(it)
                            appState.closeDrawer()
                        },
                        onSystemClick = {
                            appState.closeDrawer()
                            val windowsFolder = "C:\\Windows"
                            val drive = appState.drives.firstOrNull { d ->
                                windowsFolder.startsWith(
                                    d.rootPath.trimEnd('\\', '/'),
                                    ignoreCase = true
                                )
                            }
                            if (drive != null) {
                                appState.openDrive(drive)
                                appState.navigateTo(windowsFolder)
                            }
                        },
                        onRecycleBinClick = {
                            appState.closeDrawer()
                            try {
                                ProcessBuilder("explorer.exe", "shell:RecycleBinFolder").start()
                            } catch (e: Exception) {
                                try {
                                    java.awt.Desktop.getDesktop().open(
                                        java.io.File("C:\\\$Recycle.Bin")
                                    )
                                } catch (e2: Exception) {}
                            }
                        },
                        onProgramsClick = {
                            appState.closeDrawer()
                            appState.openPrograms()
                        },
                        onFavoriteClick = { appState.openFavorite(it) },
                        onRemoveFavorite = { appState.removeFromFavorites(it.path) },
                        onRecentClick = { file ->
                            appState.addToRecent(file)
                            val ext = file.extension.lowercase()
                            when {
                                ext in IMAGE_EXTS -> appState.openImageViewer(listOf(file), 0)
                                ext in TEXT_EXTS -> appState.openTextViewer(file)
                                ext in AUDIO_EXTS -> {
                                    val ok = audioPlayer.play(file.path)
                                    if (ok) {
                                        appState.updateAudioCurrentFile(file)
                                        appState.updateAudioPlaying(true)
                                        appState.updateAudioPosition(0L)
                                        appState.updateAudioDuration(audioPlayer.getDurationMs())
                                        appState.setMusicPlaylist(listOf(file), 0)
                                    }
                                }
                                ext in VIDEO_EXTS -> appState.openVideoPlayerScreen(listOf(file), 0)
                                ext in ARCHIVE_EXTS -> {
                                    scope.launch {
                                        val needs = archiveManager.needsPassword(file.path)
                                        appState.openArchiveViewer(file.path, needs)
                                    }
                                }
                                else -> {
                                    try {
                                        java.awt.Desktop.getDesktop().open(java.io.File(file.path))
                                    } catch (e: Exception) {}
                                }
                            }
                            appState.closeDrawer()
                        },
                        onClearRecent = { appState.clearRecent() }
                    )
                }
            }
        ) {
            if (appState.showOnboarding) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingScreen(
                        onFinish = { appState.completeOnboarding() }
                    )
                }
            } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (appState.operationsScreenVisible) {
                        OperationsScreen(
                            operations = appState.activeOperations,
                            onBack = { appState.closeOperationsScreen() },
                            onCancelOperation = { id -> appState.requestCancelOperation(id) },
                            onRemoveOperation = { id -> appState.removeOperation(id) }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                when (appState.currentScreen) {
                                    Screen.HOME -> HomeScreen(
                                        drives = appState.drives,
                                        categories = appState.categories,
                                        isScanning = appState.isScanningCategories,
                                        onDriveClick = { appState.openDrive(it) },
                                        onCategoryClick = { cat ->
                                            appState.openCategory(cat.type)
                                        },
                                        onSettingsClick = { appState.openSettings() },
                                        onOperationsClick = { appState.openOperationsScreen() },
                                        onMenuClick = { appState.openDrawer() },
                                        onAnalyzeClick = { appState.openAnalysisByIndex(0) },
                                        onSystemClick = {
                                            val windowsFolder = "C:\\Windows"
                                            val drive = appState.drives.firstOrNull { d ->
                                                windowsFolder.startsWith(
                                                    d.rootPath.trimEnd('\\', '/'),
                                                    ignoreCase = true
                                                )
                                            }
                                            if (drive != null) {
                                                appState.openDrive(drive)
                                                appState.navigateTo(windowsFolder)
                                            }
                                        },
                                        onProgramsClick = { appState.openPrograms() },
                                        onRecycleBinClick = {
                                            try {
                                                ProcessBuilder("explorer.exe", "shell:RecycleBinFolder").start()
                                            } catch (e: Exception) {
                                                try {
                                                    java.awt.Desktop.getDesktop().open(
                                                        java.io.File("C:\\\$Recycle.Bin")
                                                    )
                                                } catch (e2: Exception) {}
                                            }
                                        }
                                    )

                                    Screen.PROGRAMS -> ProgramsScreen(
                                        programs = appState.installedPrograms,
                                        isScanning = appState.isScanningPrograms,
                                        backupStatus = appState.programBackupStatus,
                                        onBack = { appState.closePrograms() },
                                        onMenuClick = { appState.openDrawer() },
                                        onProgramClick = { program ->
                                            if (program.installLocation.isNotBlank()) {
                                                val folder = java.io.File(program.installLocation)
                                                if (folder.exists()) {
                                                    val drive = appState.drives.firstOrNull { d ->
                                                        folder.absolutePath.startsWith(
                                                            d.rootPath.trimEnd('\\', '/'),
                                                            ignoreCase = true
                                                        )
                                                    }
                                                    if (drive != null) {
                                                        appState.openDrive(drive)
                                                        appState.navigateTo(folder.absolutePath)
                                                    }
                                                }
                                            }
                                        },
                                        onOpenFolder = { program ->
                                            if (program.installLocation.isNotBlank()) {
                                                try {
                                                    java.awt.Desktop.getDesktop().open(
                                                        java.io.File(program.installLocation)
                                                    )
                                                } catch (e: Exception) {}
                                            }
                                        },
                                        onUninstall = { program ->
                                            if (program.uninstallString.isNotBlank()) {
                                                try {
                                                    ProcessBuilder("cmd", "/c", program.uninstallString)
                                                        .start()
                                                } catch (e: Exception) {}
                                            }
                                        },
                                        onShare = { program ->
                                            try {
                                                val info = "البرنامج: ${program.name}\nالإصدار: ${program.version}\nالناشر: ${program.publisher}\nالمسار: ${program.installLocation}"
                                                val sel = java.awt.datatransfer.StringSelection(info)
                                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
                                                appState.updateProgramBackupStatus("✅ تم نسخ معلومات \"${program.name}\" إلى الحافظة")
                                            } catch (e: Exception) {}
                                        },
                                        onCopyInfo = { program ->
                                            try {
                                                val info = "الاسم: ${program.name}\nالإصدار: ${program.version}\nالناشر: ${program.publisher}\nتاريخ التثبيت: ${program.installDate}\nالحجم: ${program.estimatedSize} B\nالمسار: ${program.installLocation}\nإلغاء التثبيت: ${program.uninstallString}"
                                                val sel = java.awt.datatransfer.StringSelection(info)
                                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
                                                appState.updateProgramBackupStatus("✅ تم النسخ")
                                            } catch (e: Exception) {}
                                        },
                                        onBackup = { program ->
                                            appState.updateProgramBackupStatus("⏳ جارٍ إنشاء نسخة احتياطية من \"${program.name}\"...")
                                            scope.launch {
                                                val result = runProgramBackup(program) { msg ->
                                                    appState.updateProgramBackupStatus("⏳ $msg")
                                                }
                                                if (result != null) {
                                                    appState.updateProgramBackupStatus("✅ تم حفظ النسخة في Documents\\AHFM Backups")
                                                } else {
                                                    appState.updateProgramBackupStatus("❌ فشل النسخ الاحتياطي")
                                                }
                                                delay(5000)
                                                appState.updateProgramBackupStatus(null)
                                            }
                                        },
                                        onDetails = { program -> appState.showProgramDetails(program) }
                                    )

                                    Screen.DRIVE_ANALYSIS -> DriveAnalysisScreen(
                                        drive = appState.driveAnalysisDrive,
                                        path = appState.driveAnalysisPath,
                                        items = appState.driveAnalysisItems,
                                        isAnalyzing = appState.isDriveAnalyzing,
                                        onBack = { appState.driveAnalysisGoBack() },
                                        onMenuClick = { appState.openDrawer() },
                                        onItemClick = { item ->
                                            val drive = appState.driveAnalysisDrive
                                            if (item.isDirectory) {
                                                if (drive != null) {
                                                    appState.openDriveAnalysis(drive, item.path)
                                                }
                                            } else {
                                                val ext = item.name.substringAfterLast('.', "").lowercase()
                                                val fileItem = FileItem(
                                                    name = item.name,
                                                    path = item.path,
                                                    isDirectory = false,
                                                    size = item.size,
                                                    lastModified = 0L,
                                                    isHidden = false,
                                                    itemCount = 0
                                                )
                                                when {
                                                    ext in IMAGE_EXTS -> {
                                                        val images = appState.driveAnalysisItems
                                                            .filter {
                                                                !it.isDirectory &&
                                                                it.name.substringAfterLast('.', "").lowercase() in IMAGE_EXTS
                                                            }
                                                            .map {
                                                                FileItem(
                                                                    name = it.name,
                                                                    path = it.path,
                                                                    isDirectory = false,
                                                                    size = it.size,
                                                                    lastModified = 0L,
                                                                    isHidden = false,
                                                                    itemCount = 0
                                                                )
                                                            }
                                                        val idx = images.indexOfFirst { it.path == item.path }
                                                        if (idx >= 0) appState.openImageViewer(images, idx)
                                                    }
                                                    ext in VIDEO_EXTS -> {
                                                        val videos = appState.driveAnalysisItems
                                                            .filter {
                                                                !it.isDirectory &&
                                                                it.name.substringAfterLast('.', "").lowercase() in VIDEO_EXTS
                                                            }
                                                            .map {
                                                                FileItem(
                                                                    name = it.name,
                                                                    path = it.path,
                                                                    isDirectory = false,
                                                                    size = it.size,
                                                                    lastModified = 0L,
                                                                    isHidden = false,
                                                                    itemCount = 0
                                                                )
                                                            }
                                                        val idx = videos.indexOfFirst { it.path == item.path }
                                                        if (idx >= 0) appState.openVideoPlayerScreen(videos, idx)
                                                    }
                                                    ext in AUDIO_EXTS -> {
                                                        val ok = audioPlayer.play(item.path)
                                                        if (ok) {
                                                            appState.updateAudioCurrentFile(fileItem)
                                                            appState.updateAudioPlaying(true)
                                                            appState.updateAudioPosition(0L)
                                                            appState.updateAudioDuration(audioPlayer.getDurationMs())
                                                            val playlist = appState.driveAnalysisItems
                                                                .filter {
                                                                    !it.isDirectory &&
                                                                    it.name.substringAfterLast('.', "").lowercase() in AUDIO_EXTS
                                                                }
                                                                .map {
                                                                    FileItem(
                                                                        name = it.name,
                                                                        path = it.path,
                                                                        isDirectory = false,
                                                                        size = it.size,
                                                                        lastModified = 0L,
                                                                        isHidden = false,
                                                                        itemCount = 0
                                                                    )
                                                                }
                                                            val idx = playlist.indexOfFirst { it.path == item.path }
                                                            if (idx >= 0) appState.setMusicPlaylist(playlist, idx)
                                                        }
                                                    }
                                                    ext in TEXT_EXTS -> {
                                                        appState.openTextViewer(fileItem)
                                                    }
                                                    ext in ARCHIVE_EXTS -> {
                                                        scope.launch {
                                                            val needs = archiveManager.needsPassword(item.path)
                                                            appState.openArchiveViewer(item.path, needs)
                                                        }
                                                    }
                                                    else -> {
                                                        try {
                                                            java.awt.Desktop.getDesktop()
                                                                .open(java.io.File(item.path))
                                                        } catch (e: Exception) {}
                                                    }
                                                }
                                            }
                                        },
                                        onOpenInExplorer = { item ->
                                            try {
                                                val f = java.io.File(item.path)
                                                val target = if (f.isDirectory) f
                                                            else f.parentFile ?: f
                                                java.awt.Desktop.getDesktop().open(target)
                                            } catch (e: Exception) {}
                                        },
                                        onAddToFavorites = { item ->
                                            val fileItem = FileItem(
                                                name = item.name,
                                                path = item.path,
                                                isDirectory = item.isDirectory,
                                                size = item.size,
                                                lastModified = 0L,
                                                isHidden = false,
                                                itemCount = item.itemCount
                                            )
                                            appState.addToFavorites(fileItem)
                                        },
                                        onCopyPath = { item ->
                                            try {
                                                val sel = java.awt.datatransfer.StringSelection(item.path)
                                                java.awt.Toolkit.getDefaultToolkit()
                                                    .systemClipboard.setContents(sel, sel)
                                            } catch (e: Exception) {}
                                        },
                                        onExtract = { item ->
                                            val archivePath = item.path
                                            val destPath = java.io.File(archivePath).parentFile?.absolutePath
                                            if (destPath != null) {
                                                val opId = appState.beginOperation(OperationType.EXTRACT)
                                                scope.launch {
                                                    archiveManager.extract(
                                                        archivePath = archivePath,
                                                        destination = destPath,
                                                        password = null,
                                                        onProgress = { p ->
                                                            appState.updateOperationProgress(opId, OperationProgress(
                                                                currentFile = p.currentFile,
                                                                filesProcessed = p.filesProcessed,
                                                                totalFiles = p.totalFiles,
                                                                bytesProcessed = p.bytesProcessed,
                                                                totalBytes = p.totalBytes,
                                                                elapsedMillis = 0L
                                                            ))
                                                        },
                                                        isCancelled = { appState.isOperationCancelled(opId) }
                                                    )
                                                    appState.markOperationFinished(opId)
                                                    appState.refreshFiles()
                                                }
                                            }
                                        },
                                        onShare = { item ->
                                            try {
                                                val sel = java.awt.datatransfer.StringSelection(item.path)
                                                java.awt.Toolkit.getDefaultToolkit()
                                                    .systemClipboard.setContents(sel, sel)
                                            } catch (e: Exception) {}
                                        },
                                        onDelete = { item ->
                                            val fileItem = FileItem(
                                                name = item.name,
                                                path = item.path,
                                                isDirectory = item.isDirectory,
                                                size = item.size,
                                                lastModified = 0L,
                                                isHidden = false,
                                                itemCount = item.itemCount
                                            )
                                            appState.showDeleteDialog(listOf(fileItem))
                                        },
                                        onRename = { item ->
                                            val fileItem = FileItem(
                                                name = item.name,
                                                path = item.path,
                                                isDirectory = item.isDirectory,
                                                size = item.size,
                                                lastModified = 0L,
                                                isHidden = false,
                                                itemCount = item.itemCount
                                            )
                                            appState.showRenameDialog(fileItem)
                                        }
                                    )

                                    Screen.ANALYZE -> AnalyzeScreen(
                                        drives = appState.drives,
                                        selectedDriveIndex = appState.analysisSelectedDriveIndex,
                                        result = appState.analysisResult,
                                        isAnalyzing = appState.isAnalyzing,
                                        onDriveTabClick = { index ->
                                            appState.openAnalysisByIndex(index)
                                        },
                                        onBack = { appState.closeAnalysis() },
                                        onMenuClick = { appState.openDrawer() },
                                        onFileClick = { item ->
                                            if (item.isDirectory) {
                                                val drive = appState.drives.firstOrNull { d ->
                                                    item.path.startsWith(
                                                        d.rootPath.trimEnd('\\', '/'),
                                                        ignoreCase = true
                                                    )
                                                }
                                                if (drive != null) {
                                                    appState.openDrive(drive)
                                                    appState.navigateTo(item.path)
                                                }
                                            } else {
                                                try {
                                                    java.awt.Desktop.getDesktop()
                                                        .open(java.io.File(item.path))
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    )

                                    Screen.ARCHIVE_VIEWER -> ArchiveViewerScreen(
                                        archivePath = appState.archivePath,
                                        entries = appState.archiveEntries,
                                        currentFolder = appState.archiveCurrentFolder,
                                        isLoading = appState.isArchiveLoading,
                                        needsPassword = appState.archiveNeedsPassword,
                                        onBack = { appState.closeArchiveViewer() },
                                        onMenuClick = { appState.openDrawer() },
                                        onFolderClick = { folder -> appState.updateArchiveCurrentFolder(folder) },
                                        onPreviewFile = { entry ->
                                            val temp = File(System.getProperty("java.io.tmpdir"), "AHFM_preview")
                                            if (!temp.exists()) temp.mkdirs()
                                            scope.launch {
                                                val archivePath = appState.archivePath ?: return@launch
                                                archiveManager.extractSelected(
                                                    archivePath = archivePath,
                                                    entryNames = listOf(entry.path),
                                                    destination = temp.absolutePath,
                                                    password = appState.archivePassword,
                                                    onProgress = { },
                                                    isCancelled = { false }
                                                )
                                                val extracted = File(temp, entry.path)
                                                if (extracted.exists()) {
                                                    try {
                                                        java.awt.Desktop.getDesktop().open(extracted)
                                                    } catch (e: Exception) {
                                                        println("[Preview] open error: ${e.message}")
                                                    }
                                                }
                                            }
                                        },
                                        onExtractAll = {
                                            val archivePath = appState.archivePath
                                            val destPath = appState.currentPath
                                                ?: archivePath?.substringBeforeLast('\\')
                                            if (archivePath != null && destPath != null) {
                                                val opId = appState.beginOperation(OperationType.EXTRACT)
                                                scope.launch {
                                                    archiveManager.extract(
                                                        archivePath = archivePath,
                                                        destination = destPath,
                                                        password = appState.archivePassword,
                                                        onProgress = { p ->
                                                            appState.updateOperationProgress(opId, OperationProgress(
                                                                currentFile = p.currentFile,
                                                                filesProcessed = p.filesProcessed,
                                                                totalFiles = p.totalFiles,
                                                                bytesProcessed = p.bytesProcessed,
                                                                totalBytes = p.totalBytes,
                                                                elapsedMillis = 0L
                                                            ))
                                                        },
                                                        isCancelled = { appState.isOperationCancelled(opId) }
                                                    )
                                                    appState.markOperationFinished(opId)
                                                    appState.refreshFiles()
                                                }
                                            }
                                        },
                                        onExtractEntry = { entry ->
                                            val destPath = appState.archivePath?.substringBeforeLast('\\')
                                            if (destPath != null) {
                                                val opId = appState.beginOperation(OperationType.EXTRACT)
                                                scope.launch {
                                                    archiveManager.extractSelected(
                                                        archivePath = appState.archivePath!!,
                                                        entryNames = listOf(entry.path),
                                                        destination = destPath,
                                                        password = appState.archivePassword,
                                                        onProgress = { p ->
                                                            appState.updateOperationProgress(opId, OperationProgress(
                                                                currentFile = p.currentFile,
                                                                filesProcessed = p.filesProcessed,
                                                                totalFiles = p.totalFiles,
                                                                bytesProcessed = p.bytesProcessed,
                                                                totalBytes = p.totalBytes
                                                            ))
                                                        },
                                                        isCancelled = { appState.isOperationCancelled(opId) }
                                                    )
                                                    appState.markOperationFinished(opId)
                                                    appState.refreshFiles()
                                                }
                                            }
                                        },
                                        onExtractEntryTo = { entry ->
                                            val baseName = appState.archivePath?.substringAfterLast('\\')?.substringBeforeLast('.')
                                            val destPath = appState.archivePath?.substringBeforeLast('\\') + "\\" + baseName
                                            if (destPath != null) {
                                                val opId = appState.beginOperation(OperationType.EXTRACT)
                                                scope.launch {
                                                    archiveManager.extractSelected(
                                                        archivePath = appState.archivePath!!,
                                                        entryNames = listOf(entry.path),
                                                        destination = destPath,
                                                        password = appState.archivePassword,
                                                        onProgress = { p ->
                                                            appState.updateOperationProgress(opId, OperationProgress(
                                                                currentFile = p.currentFile,
                                                                filesProcessed = p.filesProcessed,
                                                                totalFiles = p.totalFiles,
                                                                bytesProcessed = p.bytesProcessed,
                                                                totalBytes = p.totalBytes
                                                            ))
                                                        },
                                                        isCancelled = { appState.isOperationCancelled(opId) }
                                                    )
                                                    appState.markOperationFinished(opId)
                                                    appState.refreshFiles()
                                                }
                                            }
                                        },
                                        onCopyEntryPath = { entry ->
                                            try {
                                                val sel = java.awt.datatransfer.StringSelection(entry.path)
                                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
                                            } catch (e: Exception) {}
                                        },
                                        onPasswordEntered = { pwd -> appState.updateArchivePassword(pwd) }
                                    )

                                    Screen.CATEGORY_BROWSER -> {
                                        val type = appState.categoryScanType ?: CategoryType.IMAGES
                                        CategoryBrowserScreen(
                                            type = type,
                                            folders = appState.categoryFolders,
                                            isScanning = appState.isScanningCategoryFolders,
                                            analysisMode = appState.categoryAnalysisMode,
                                            onToggleAnalysis = { appState.toggleCategoryAnalysisMode() },
                                            onBack = { appState.closeCategory() },
                                            onMenuClick = { appState.openDrawer() },
                                            onFolderClick = { folder ->
                                                if (appState.categoryAnalysisMode) {
                                                    val drive = appState.drives.firstOrNull { d ->
                                                        folder.path.startsWith(
                                                            d.rootPath.trimEnd('\\', '/'),
                                                            ignoreCase = true
                                                        )
                                                    }
                                                    if (drive != null) {
                                                        appState.openDriveAnalysis(
                                                            drive = drive,
                                                            path = folder.path,
                                                            returnTo = Screen.CATEGORY_BROWSER
                                                        )
                                                    }
                                                } else {
                                                    appState.selectCategoryFolder(folder.path)
                                                }
                                            },
                                            onFileOpen = { hit ->
                                                val ext = hit.name.substringAfterLast('.', "").lowercase()
                                                val fileItem = FileItem(
                                                    name = hit.name,
                                                    path = hit.path,
                                                    isDirectory = false,
                                                    size = hit.size,
                                                    lastModified = 0L,
                                                    isHidden = false,
                                                    itemCount = 0
                                                )
                                                when {
                                                    ext in IMAGE_EXTS -> appState.openImageViewer(listOf(fileItem), 0)
                                                    ext in VIDEO_EXTS -> appState.openVideoPlayerScreen(listOf(fileItem), 0)
                                                    ext in AUDIO_EXTS -> {
                                                        if (audioPlayer.play(hit.path)) {
                                                            appState.updateAudioCurrentFile(fileItem)
                                                            appState.updateAudioPlaying(true)
                                                            appState.updateAudioPosition(0L)
                                                            appState.updateAudioDuration(audioPlayer.getDurationMs())
                                                        }
                                                    }
                                                    ext in TEXT_EXTS -> appState.openTextViewer(fileItem)
                                                    ext in ARCHIVE_EXTS -> {
                                                        scope.launch {
                                                            val needs = archiveManager.needsPassword(hit.path)
                                                            appState.openArchiveViewer(hit.path, needs)
                                                        }
                                                    }
                                                    else -> {
                                                        try {
                                                            java.awt.Desktop.getDesktop()
                                                                .open(java.io.File(hit.path))
                                                        } catch (e: Exception) {}
                                                    }
                                                }
                                            },
                                            onAddToFavorites = { folder ->
                                                appState.addToFavorites(folder.toFileItem())
                                            },
                                            onOpenInExplorer = { folder ->
                                                try {
                                                    java.awt.Desktop.getDesktop()
                                                        .open(java.io.File(folder.path))
                                                } catch (e: Exception) {}
                                            },
                                            onCopyPath = { folder ->
                                                try {
                                                    val sel = java.awt.datatransfer.StringSelection(folder.path)
                                                    java.awt.Toolkit.getDefaultToolkit()
                                                        .systemClipboard.setContents(sel, sel)
                                                } catch (e: Exception) {}
                                            },
                                            onCopy = { folder ->
                                                appState.selectCategoryFolder(folder.path)
                                                appState.selectOnly(folder.path)
                                                appState.startClipboard(
                                                    com.ah.filemanagerplus.data.ClipboardAction.COPY
                                                )
                                            },
                                            onMove = { folder ->
                                                appState.selectCategoryFolder(folder.path)
                                                appState.selectOnly(folder.path)
                                                appState.startClipboard(
                                                    com.ah.filemanagerplus.data.ClipboardAction.MOVE
                                                )
                                            },
                                            onDelete = { folder ->
                                                appState.showDeleteDialog(listOf(folder.toFileItem()))
                                            },
                                            onRename = { folder ->
                                                appState.showRenameDialog(folder.toFileItem())
                                            },
                                            onShare = { folder ->
                                                try {
                                                    val sel = java.awt.datatransfer.StringSelection(folder.path)
                                                    java.awt.Toolkit.getDefaultToolkit()
                                                        .systemClipboard.setContents(sel, sel)
                                                } catch (e: Exception) {}
                                            },
                                            onProperties = { folder ->
                                                println("[Properties] ${folder.path}")
                                            }
                                        )
                                    }

                                    Screen.IMAGE_VIEWER -> ImageViewerScreen(
                                        images = appState.viewerImages,
                                        currentIndex = appState.viewerIndex,
                                        onClose = { appState.closeImageViewer() },
                                        onNext = { appState.viewerNext() },
                                        onPrev = { appState.viewerPrev() }
                                    )

                                    Screen.TEXT_VIEWER -> {
                                        val file = appState.textViewerFile
                                        if (file != null) {
                                            TextViewerScreen(
                                                file = file,
                                                onClose = { appState.closeTextViewer() }
                                            )
                                        }
                                    }

                                    Screen.MUSIC_PLAYER -> MusicPlayerScreen(
                                        track = appState.musicCurrentTrack,
                                        playlistSize = appState.musicPlaylist.size,
                                        currentIndex = appState.musicCurrentIndex,
                                        isPlaying = appState.audioIsPlaying,
                                        isShuffleOn = appState.isShuffleOn,
                                        isRepeatOn = appState.isRepeatOn,
                                        positionMs = appState.audioPositionMs,
                                        durationMs = appState.audioDurationMs,
                                        onClose = { appState.closeMusicPlayer() },
                                        onPlayPause = {
                                            if (appState.audioIsPlaying) {
                                                audioPlayer.pause()
                                                appState.updateAudioPlaying(false)
                                            } else {
                                                audioPlayer.resume()
                                                appState.updateAudioPlaying(true)
                                            }
                                        },
                                        onNext = {
                                            val next = appState.musicNext()
                                            if (next != null) {
                                                audioPlayer.play(next.path)
                                                appState.updateAudioCurrentFile(next)
                                                appState.updateAudioPlaying(true)
                                                appState.updateAudioPosition(0L)
                                                appState.updateAudioDuration(audioPlayer.getDurationMs())
                                            }
                                        },
                                        onPrev = {
                                            val prev = appState.musicPrev()
                                            if (prev != null) {
                                                audioPlayer.play(prev.path)
                                                appState.updateAudioCurrentFile(prev)
                                                appState.updateAudioPlaying(true)
                                                appState.updateAudioPosition(0L)
                                                appState.updateAudioDuration(audioPlayer.getDurationMs())
                                            }
                                        },
                                        onToggleShuffle = { appState.toggleShuffle() },
                                        onToggleRepeat = { appState.toggleRepeat() },
                                        onSeek = { ms -> audioPlayer.seekTo(ms) }
                                    )

                                    Screen.VIDEO_PLAYER -> VideoPlayerScreen(
                                        file = appState.videoCurrentFile,
                                        videoPlayer = videoPlayer,
                                        isPlaying = appState.videoIsPlaying,
                                        positionMs = appState.videoPositionMs,
                                        durationMs = appState.videoDurationMs,
                                        errorMessage = appState.videoErrorMessage,
                                        playlistSize = appState.videoPlaylist.size,
                                        currentIndex = appState.videoCurrentIndex,
                                        onClose = {
                                            videoPlayer.release()
                                            appState.updateVideoCurrentFile(null)
                                            appState.closeVideoPlayer()
                                        },
                                        onPlayPause = {
                                            if (appState.videoIsPlaying) {
                                                videoPlayer.pause()
                                                appState.updateVideoPlaying(false)
                                            } else {
                                                videoPlayer.play()
                                                appState.updateVideoPlaying(true)
                                            }
                                        },
                                        onSeek = { ms -> videoPlayer.seekTo(ms) },
                                        onNext = {
                                            val next = appState.videoNext()
                                            if (next != null) appState.updateVideoPlaying(true)
                                        },
                                        onPrev = {
                                            val prev = appState.videoPrev()
                                            if (prev != null) appState.updateVideoPlaying(true)
                                        }
                                    )

                                    Screen.SETTINGS -> SettingsScreen(
                                        settings = appState.settings,
                                        onSettingsChange = { appState.updateSettings(it) },
                                        onBack = { appState.closeSettings() },
                                        onOpenAbout = { appState.openAbout() },
                                        onResetOnboarding = { appState.resetOnboarding() },
                                        onMenuClick = { appState.openDrawer() }
                                    )

                                    Screen.ABOUT -> AboutScreen(
                                        onBack = { appState.closeAbout() },
                                        onMenuClick = { appState.openDrawer() }
                                    )

                                    Screen.BROWSER -> BrowserScreen(
                                        appState = appState,
                                        onBack = {
                                            val handled = appState.goBack()
                                            if (!handled) appState.closeBrowser()
                                        },
                                        onHomeClick = { appState.closeBrowser() },
                                        onDriveClick = {
                                            val root = appState.currentDrive?.rootPath
                                            if (root != null) appState.navigateTo(root)
                                        },
                                        onSegmentClick = { index ->
                                            val drive = appState.currentDrive
                                            val path = appState.currentPath
                                            if (drive != null && path != null) {
                                                val segments = computeSegments(drive.rootPath, path)
                                                val newSegments = segments.take(index + 1)
                                                val newPath = drive.rootPath.trimEnd('\\', '/') +
                                                        "\\" + newSegments.joinToString("\\")
                                                appState.navigateTo(newPath)
                                            }
                                        },
                                        onFolderClick = { appState.navigateTo(it.path) },
                                        onFileOpen = { file ->
                                            val ext = file.extension.lowercase()
                                            when {
                                                ext in ARCHIVE_EXTS -> {
                                                    scope.launch {
                                                        val needsPwd = archiveManager.needsPassword(file.path)
                                                        appState.openArchiveViewer(file.path, needsPwd)
                                                    }
                                                }
                                                ext in IMAGE_EXTS -> {
                                                    val images = appState.currentFiles
                                                        .filter {
                                                            it.extension.lowercase() in IMAGE_EXTS &&
                                                            !it.isDirectory
                                                        }
                                                    val idx = images.indexOfFirst { it.path == file.path }
                                                    if (idx >= 0) appState.openImageViewer(images, idx)
                                                }
                                                ext in VIDEO_EXTS -> {
                                                    val videos = appState.currentFiles
                                                        .filter {
                                                            it.extension.lowercase() in VIDEO_EXTS &&
                                                            !it.isDirectory
                                                        }
                                                    val idx = videos.indexOfFirst { it.path == file.path }
                                                    if (idx >= 0) appState.openVideoPlayerScreen(videos, idx)
                                                }
                                                ext in AUDIO_EXTS -> {
                                                    val ok = audioPlayer.play(file.path)
                                                    if (ok) {
                                                        appState.updateAudioCurrentFile(file)
                                                        appState.updateAudioPlaying(true)
                                                        appState.updateAudioPosition(0L)
                                                        appState.updateAudioDuration(audioPlayer.getDurationMs())
                                                        val playlist = appState.currentFiles
                                                            .filter {
                                                                it.extension.lowercase() in AUDIO_EXTS &&
                                                                !it.isDirectory
                                                            }
                                                        val idx = playlist.indexOfFirst { it.path == file.path }
                                                        if (idx >= 0) appState.setMusicPlaylist(playlist, idx)
                                                    }
                                                }
                                                ext in TEXT_EXTS -> {
                                                    appState.openTextViewer(file)
                                                }
                                                else -> {
                                                    try {
                                                        java.awt.Desktop.getDesktop()
                                                            .open(java.io.File(file.path))
                                                    } catch (e: Exception) {
                                                        println("Cannot open: ${e.message}")
                                                    }
                                                }
                                            }
                                        },
                                        onCopyOrMove = { isCopy ->
                                            appState.startClipboard(
                                                if (isCopy) ClipboardAction.COPY
                                                else ClipboardAction.MOVE
                                            )
                                        },
                                        onDelete = {
                                            val items = appState.currentFiles
                                                .filter { appState.isSelected(it.path) }
                                            if (items.isNotEmpty()) appState.showDeleteDialog(items)
                                        },
                                        onRename = {
                                            val items = appState.currentFiles
                                                .filter { appState.isSelected(it.path) }
                                            if (items.size == 1) appState.showRenameDialog(items.first())
                                        },
                                        onSearchClick = {
                                            val root = appState.currentPath
                                            if (root != null) appState.openSearch(root)
                                        },
                                        onSettingsClick = { appState.openSettings() },
                                        onMenuClick = { appState.openDrawer() },
                                        onAddToFavorites = { appState.addToFavorites(it) },
                                        onAnalyzeClick = {
                                            val drive = appState.currentDrive
                                            val path = appState.currentPath
                                            if (drive != null && path != null) {
                                                appState.openDriveAnalysis(drive, path)
                                            }
                                        },
                                        onCompress = { file ->
                                            val dest = appState.currentPath
                                            if (dest != null) {
                                                appState.startCompress(listOf(file.path), dest)
                                            }
                                        },
                                        onArchivePreview = { file ->
                                            scope.launch {
                                                val needsPwd = archiveManager.needsPassword(file.path)
                                                appState.openArchiveViewer(file.path, needsPwd)
                                            }
                                        },
                                        onArchiveExtractHere = { file ->
                                            val dest = appState.currentPath
                                            if (dest != null) {
                                                val opId = appState.beginOperation(OperationType.EXTRACT)
                                                scope.launch {
                                                    archiveManager.extract(
                                                        archivePath = file.path,
                                                        destination = dest,
                                                        password = null,
                                                        onProgress = { p ->
                                                            appState.updateOperationProgress(opId, OperationProgress(
                                                                currentFile = p.currentFile,
                                                                filesProcessed = p.filesProcessed,
                                                                totalFiles = p.totalFiles,
                                                                bytesProcessed = p.bytesProcessed,
                                                                totalBytes = p.totalBytes
                                                            ))
                                                        },
                                                        isCancelled = { appState.isOperationCancelled(opId) }
                                                    )
                                                    appState.markOperationFinished(opId)
                                                    appState.refreshFiles()
                                                }
                                            }
                                        },
                                        onArchiveExtractTo = { file ->
                                            val dest = appState.currentPath
                                            if (dest != null) {
                                                val archiveName = file.name.substringBeforeLast('.')
                                                val newDest = "$dest\\$archiveName"
                                                val opId = appState.beginOperation(OperationType.EXTRACT)
                                                scope.launch {
                                                    archiveManager.extract(
                                                        archivePath = file.path,
                                                        destination = newDest,
                                                        password = null,
                                                        onProgress = { p ->
                                                            appState.updateOperationProgress(opId, OperationProgress(
                                                                currentFile = p.currentFile,
                                                                filesProcessed = p.filesProcessed,
                                                                totalFiles = p.totalFiles,
                                                                bytesProcessed = p.bytesProcessed,
                                                                totalBytes = p.totalBytes
                                                            ))
                                                        },
                                                        isCancelled = { appState.isOperationCancelled(opId) }
                                                    )
                                                    appState.markOperationFinished(opId)
                                                    appState.refreshFiles()
                                                }
                                            }
                                        }
                                    )

                                    Screen.SEARCH -> SearchScreen(
                                        rootPath = appState.searchRootPath ?: "",
                                        query = appState.searchQuery,
                                        results = appState.searchResults,
                                        isSearching = appState.isSearching,
                                        onQueryChange = { appState.updateSearchQuery(it) },
                                        onBack = { appState.closeSearch() },
                                        onMenuClick = { appState.openDrawer() },
                                        onResultClick = { file ->
                                            val ext = file.extension.lowercase()
                                            when {
                                                file.isDirectory -> {
                                                    appState.navigateTo(file.path)
                                                    appState.switchToBrowser()
                                                }
                                                ext in IMAGE_EXTS -> {
                                                    appState.openImageViewer(listOf(file), 0)
                                                }
                                                ext in TEXT_EXTS -> {
                                                    appState.openTextViewer(file)
                                                }
                                                ext in AUDIO_EXTS -> {
                                                    val ok = audioPlayer.play(file.path)
                                                    if (ok) {
                                                        appState.updateAudioCurrentFile(file)
                                                        appState.updateAudioPlaying(true)
                                                        appState.updateAudioPosition(0L)
                                                        appState.updateAudioDuration(audioPlayer.getDurationMs())
                                                        appState.setMusicPlaylist(listOf(file), 0)
                                                    }
                                                }
                                                ext in VIDEO_EXTS -> {
                                                    appState.openVideoPlayerScreen(listOf(file), 0)
                                                }
                                                ext in ARCHIVE_EXTS -> {
                                                    scope.launch {
                                                        val needs = archiveManager.needsPassword(file.path)
                                                        appState.openArchiveViewer(file.path, needs)
                                                    }
                                                }
                                                else -> {
                                                    try {
                                                        java.awt.Desktop.getDesktop()
                                                            .open(java.io.File(file.path))
                                                    } catch (e: Exception) {
                                                        println("Cannot open: ${e.message}")
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            val audioFile = appState.audioCurrentFile
                            if (audioFile != null && appState.currentScreen != Screen.MUSIC_PLAYER) {
                                MiniPlayer(
                                    file = audioFile,
                                    isPlaying = appState.audioIsPlaying,
                                    positionMs = appState.audioPositionMs,
                                    durationMs = appState.audioDurationMs,
                                    onOpenFull = {
                                        if (appState.musicPlaylist.isNotEmpty()) {
                                            appState.openMusicPlayer(
                                                appState.musicPlaylist,
                                                appState.musicCurrentIndex
                                            )
                                        }
                                    },
                                    onPlayPause = {
                                        if (appState.audioIsPlaying) {
                                            audioPlayer.pause()
                                            appState.updateAudioPlaying(false)
                                        } else {
                                            audioPlayer.resume()
                                            appState.updateAudioPlaying(true)
                                        }
                                    },
                                    onNext = {
                                        val next = appState.musicNext()
                                        if (next != null) {
                                            audioPlayer.play(next.path)
                                            appState.updateAudioCurrentFile(next)
                                            appState.updateAudioPlaying(true)
                                            appState.updateAudioPosition(0L)
                                            appState.updateAudioDuration(audioPlayer.getDurationMs())
                                        }
                                    },
                                    onPrev = {
                                        val prev = appState.musicPrev()
                                        if (prev != null) {
                                            audioPlayer.play(prev.path)
                                            appState.updateAudioCurrentFile(prev)
                                            appState.updateAudioPlaying(true)
                                            appState.updateAudioPosition(0L)
                                            appState.updateAudioDuration(audioPlayer.getDurationMs())
                                        }
                                    },
                                    onStop = {
                                        audioPlayer.stop()
                                        appState.clearAudio()
                                    }
                                )
                            }

                            if (appState.clipboardItems.isNotEmpty()) {
                                val actionLabel = when (appState.clipboardAction) {
                                    ClipboardAction.COPY -> "نسخ"
                                    ClipboardAction.MOVE -> "نقل"
                                    null -> ""
                                }
                                ClipboardBottomBar(
                                    actionLabel = actionLabel,
                                    count = appState.clipboardItems.size,
                                    canPaste = appState.currentPath != null,
                                    onCancel = { appState.cancelClipboard() },
                                    onPaste = doPaste
                                )
                            }
                        }

                        if (appState.hasActiveOperations && !appState.operationsDialogVisible) {
                            val runningOp = appState.activeOperations.firstOrNull { !it.isFinished }
                                ?: appState.activeOperations.first()
                            OperationBadge(
                                count = appState.operationsCount,
                                percent = runningOp.percent,
                                onClick = { appState.openOperationsScreen() },
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 20.dp, bottom = 96.dp)
                            )
                        }
                    }
                }

                if (appState.operationsDialogVisible && appState.hasActiveOperations) {
                    OperationsDialog(
                        operations = appState.activeOperations,
                        onCancelOperation = { id -> appState.requestCancelOperation(id) },
                        onHide = { appState.hideOperationsDialog() }
                    )
                }

                val conflict = appState.pendingConflict
                if (conflict != null) {
                    ConflictDialog(
                        info = conflict,
                        onResolve = { decision -> appState.resolveConflict(decision) }
                    )
                }

                if (appState.isDeleteDialogVisible && appState.deleteTarget.isNotEmpty()) {
                    val deleteItems = appState.deleteTarget
                    ConfirmDeleteDialog(
                        items = deleteItems,
                        onCancel = { appState.hideDeleteDialog() },
                        onConfirm = { permanent ->
                            appState.hideDeleteDialog()
                            val opId = appState.beginOperation(OperationType.DELETE)
                            scope.launch {
                                val result = fileSystem.delete(
                                    sources = deleteItems.map { it.path },
                                    permanently = permanent,
                                    onProgress = { appState.updateOperationProgress(opId, it) },
                                    isCancelled = { appState.isOperationCancelled(opId) }
                                )
                                appState.markOperationFinished(opId, result.errorMessage)
                                appState.clearSelection()
                                appState.refreshFiles()
                            }
                        }
                    )
                }

                val renameTarget = appState.renameTarget
                if (renameTarget != null) {
                    RenameDialog(
                        currentName = renameTarget.name,
                        onCancel = { appState.hideRenameDialog() },
                        onConfirm = { newName ->
                            appState.hideRenameDialog()
                            scope.launch {
                                val result = fileSystem.rename(renameTarget.path, newName)
                                appState.clearSelection()
                                appState.refreshFiles()
                            }
                        }
                    )
                }

                // ==================== نافذة إنشاء الأرشيف ====================
                val compressSources = appState.pendingCompressSources
                val compressDest = appState.pendingCompressDestinationFolder
                if (compressSources.isNotEmpty() && compressDest != null) {
                    val defaultName = java.io.File(compressSources.first()).name
                    CreateArchiveDialog(
                        defaultName = defaultName,
                        onCancel = { appState.clearCompress() },
                        onCreate = { config: ArchiveConfig ->
                            appState.clearCompress()
                            val destPath = "$compressDest${java.io.File.separator}${config.name}${config.format.extension}"
                            val opId = appState.beginOperation(OperationType.COMPRESS)
                            scope.launch {
                                val ok = archiveManager.compress(
                                    sources = compressSources,
                                    destinationArchive = destPath,
                                    format = config.format,
                                    level = config.level,
                                    password = config.password,
                                    onProgress = { p ->
                                        appState.updateOperationProgress(opId, OperationProgress(
                                            currentFile = p.currentFile,
                                            filesProcessed = p.filesProcessed,
                                            totalFiles = p.totalFiles,
                                            bytesProcessed = p.bytesProcessed,
                                            totalBytes = p.totalBytes
                                        ))
                                    },
                                    isCancelled = { appState.isOperationCancelled(opId) }
                                )
                                appState.markOperationFinished(opId)
                                if (ok && config.deleteAfter) {
                                    compressSources.forEach { p ->
                                        try { java.io.File(p).deleteRecursively() } catch (e: Exception) {}
                                    }
                                }
                                appState.refreshFiles()
                            }
                        }
                    )
                }

                // ==================== حوار تفاصيل البرنامج ====================
                val detailsTarget = appState.programDetailsTarget
                if (detailsTarget != null) {
                    ProgramDetailsDialog(
                        program = detailsTarget,
                        onDismiss = { appState.hideProgramDetails() },
                        onBackup = {
                            appState.hideProgramDetails()
                            appState.updateProgramBackupStatus("⏳ جارٍ إنشاء نسخة احتياطية...")
                            scope.launch {
                                val result = runProgramBackup(detailsTarget) { msg ->
                                    appState.updateProgramBackupStatus("⏳ $msg")
                                }
                                appState.updateProgramBackupStatus(
                                    if (result != null) "✅ تم حفظ النسخة في Documents\\AHFM Backups"
                                    else "❌ فشل النسخ الاحتياطي"
                                )
                                delay(5000)
                                appState.updateProgramBackupStatus(null)
                            }
                        },
                        onOpenFolder = {
                            if (detailsTarget.installLocation.isNotBlank()) {
                                try {
                                    java.awt.Desktop.getDesktop().open(
                                        java.io.File(detailsTarget.installLocation)
                                    )
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }

                if (showShortcutDialog) {
                    ShortcutDialog(
                        onDismiss = {
                            markShortcutAsked()
                            showShortcutDialog = false
                        },
                        onCreate = {
                            createDesktopShortcut()
                            markShortcutAsked()
                            showShortcutDialog = false
                        }
                    )
                }
            }
            }
        }
    }
}

private fun computeSegments(driveRoot: String, path: String): List<String> {
    val root = driveRoot.trimEnd('\\', '/')
    val current = path.trimEnd('\\', '/')
    if (current.equals(root, ignoreCase = true)) return emptyList()
    val relative = current.removePrefix(root).trimStart('\\', '/')
    if (relative.isBlank()) return emptyList()
    return relative.split('\\', '/').filter { it.isNotBlank() }
}