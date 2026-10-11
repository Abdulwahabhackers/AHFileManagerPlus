package com.ah.filemanagerplus.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ah.filemanagerplus.core.archive.ArchiveEntry
import com.ah.filemanagerplus.core.filesystem.CategoryFolder
import com.ah.filemanagerplus.core.filesystem.ConflictDecision
import com.ah.filemanagerplus.core.filesystem.ConflictInfo
import com.ah.filemanagerplus.core.filesystem.OperationProgress
import com.ah.filemanagerplus.core.filesystem.OperationType
import com.ah.filemanagerplus.data.model.AnalysisResult
import com.ah.filemanagerplus.data.model.CategoryData
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.data.model.DriveAnalysisItem
import com.ah.filemanagerplus.data.model.FavoriteItem
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.data.model.InstalledProgram
import kotlinx.coroutines.CompletableDeferred
import kotlin.random.Random

enum class Screen {
    HOME, BROWSER, SEARCH, CATEGORY_BROWSER,
    IMAGE_VIEWER, TEXT_VIEWER, MUSIC_PLAYER, VIDEO_PLAYER,
    SETTINGS, ABOUT, ANALYZE, DRIVE_ANALYSIS,
    ARCHIVE_VIEWER, PROGRAMS
}
enum class ClipboardAction { COPY, MOVE }
enum class SortBy { NAME, SIZE, DATE, TYPE }
enum class ViewMode { LIST, GRID }

data class OperationJob(
    val id: String,
    val type: OperationType,
    val progress: OperationProgress,
    val isPreparing: Boolean = true,
    val isFinished: Boolean = false,
    val errorMessage: String? = null
) {
    val percent: Int get() = progress.percent
}

class AppState {
    var drives by mutableStateOf<List<Drive>>(emptyList())
        private set

    var categories by mutableStateOf<List<CategoryData>>(emptyList())
        private set

    var isScanningCategories by mutableStateOf(false)
        private set

    var currentScreen by mutableStateOf(Screen.HOME)
        private set

    var currentDrive by mutableStateOf<Drive?>(null)
        private set

    var currentPath by mutableStateOf<String?>(null)
        private set

    var currentFiles by mutableStateOf<List<FileItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var filesRevision by mutableStateOf(0)
        private set

    // ==================== SCROLL POSITION ====================
    var browserScrollIndex by mutableStateOf(0)
        private set
    var browserScrollOffset by mutableStateOf(0)
        private set

    fun saveBrowserScroll(index: Int, offset: Int) {
        browserScrollIndex = index
        browserScrollOffset = offset
    }

    fun resetBrowserScroll() {
        browserScrollIndex = 0
        browserScrollOffset = 0
    }

    // ==================== DRAWER ====================
    var drawerOpen by mutableStateOf(false)
        private set

    fun openDrawer() { drawerOpen = true }
    fun closeDrawer() { drawerOpen = false }

    // ==================== FAVORITES ====================
    var favorites by mutableStateOf<List<FavoriteItem>>(emptyList())
        private set

    var recentFiles by mutableStateOf<List<FileItem>>(emptyList())
        private set

    private var favoritesStorage: FavoritesStorage? = null

    fun initFavorites(storage: FavoritesStorage) {
        favoritesStorage = storage
        favorites = storage.loadAll()
    }

    fun addToFavorites(item: FileItem) {
        if (favorites.any { it.path == item.path }) return
        val newFav = FavoriteItem(
            path = item.path,
            name = item.name,
            isDirectory = item.isDirectory,
            addedAt = System.currentTimeMillis()
        )
        favorites = (listOf(newFav) + favorites).take(200)
        favoritesStorage?.save(favorites)
    }

    fun removeFromFavorites(path: String) {
        favorites = favorites.filter { it.path != path }
        favoritesStorage?.save(favorites)
    }

    fun isFavorite(path: String): Boolean = favorites.any { it.path == path }

    fun addToRecent(item: FileItem) {
        if (item.isDirectory) return
        recentFiles = (listOf(item) + recentFiles.filter { it.path != item.path }).take(50)
    }

    fun clearRecent() {
        recentFiles = emptyList()
    }

    fun openFavorite(fav: FavoriteItem) {
        val path = fav.path
        val matchedDrive = drives.firstOrNull { drive ->
            path.startsWith(drive.rootPath.trimEnd('\\', '/'), ignoreCase = true)
        } ?: drives.firstOrNull()

        if (matchedDrive != null) {
            currentDrive = matchedDrive
            currentPath = if (fav.isDirectory) path else path.substringBeforeLast('\\', path)
            currentScreen = Screen.BROWSER
            activeCategoryFilter = null
            closeDrawer()
            clearSelection()
        }
    }

    // ==================== ANALYSIS ====================
    var analysisDrive by mutableStateOf<Drive?>(null)
        private set
    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set
    var isAnalyzing by mutableStateOf(false)
        private set
    var analysisSelectedDriveIndex by mutableStateOf(0)
        private set

    fun setAnalysisDriveIndex(index: Int) {
        analysisSelectedDriveIndex = index
    }

    fun openAnalysis(drive: Drive) {
        val idx = drives.indexOfFirst { it.rootPath == drive.rootPath }
        analysisSelectedDriveIndex = if (idx >= 0) idx else 0
        analysisDrive = drive
        analysisResult = null
        isAnalyzing = true
        currentScreen = Screen.ANALYZE
    }

    fun openAnalysisByIndex(index: Int) {
        val drive = drives.getOrNull(index) ?: return
        analysisSelectedDriveIndex = index
        analysisDrive = drive
        analysisResult = null
        isAnalyzing = true
        currentScreen = Screen.ANALYZE
    }

    fun updateAnalysis(result: AnalysisResult) {
        analysisResult = result
    }

    fun updateAnalyzing(v: Boolean) { isAnalyzing = v }

    fun closeAnalysis() {
        currentScreen = Screen.HOME
        analysisDrive = null
        analysisResult = null
        isAnalyzing = false
        analysisSelectedDriveIndex = 0
    }

    // ==================== PROGRAMS ====================
    var installedPrograms by mutableStateOf<List<InstalledProgram>>(emptyList())
        private set
    var isScanningPrograms by mutableStateOf(false)
        private set

    var programDetailsTarget by mutableStateOf<InstalledProgram?>(null)
        private set
    var programBackupStatus by mutableStateOf<String?>(null)
        private set

    fun updatePrograms(list: List<InstalledProgram>) {
        installedPrograms = list
        isScanningPrograms = false
    }
    fun updateScanningPrograms(v: Boolean) { isScanningPrograms = v }

    fun openPrograms() {
        isScanningPrograms = true
        currentScreen = Screen.PROGRAMS
    }
    fun closePrograms() {
        currentScreen = Screen.HOME
    }

    fun showProgramDetails(program: InstalledProgram) {
        programDetailsTarget = program
    }
    fun hideProgramDetails() { programDetailsTarget = null }

    fun updateProgramBackupStatus(msg: String?) { programBackupStatus = msg }

    /** مسار سلة المحذوفات (Shell namespace) */
    fun getRecycleBinPath(): String = "shell:RecycleBinFolder"

    // ==================== DRIVE ANALYSIS ====================
    var driveAnalysisDrive by mutableStateOf<Drive?>(null)
        private set
    var driveAnalysisPath by mutableStateOf<String?>(null)
        private set
    var driveAnalysisItems by mutableStateOf<List<DriveAnalysisItem>>(emptyList())
        private set
    var isDriveAnalyzing by mutableStateOf(false)
        private set

    var driveAnalysisReturnScreen by mutableStateOf(Screen.HOME)
        private set

    var categoryAnalysisMode by mutableStateOf(false)
        private set

    private val driveAnalysisCache = mutableMapOf<String, List<DriveAnalysisItem>>()
    private val driveAnalysisHistory = mutableListOf<String>()

    fun toggleCategoryAnalysisMode() { categoryAnalysisMode = !categoryAnalysisMode }
    fun updateCategoryAnalysisMode(v: Boolean) { categoryAnalysisMode = v }

    fun openDriveAnalysis(
        drive: Drive,
        path: String,
        pushHistory: Boolean = true,
        returnTo: Screen? = null
    ) {
        if (driveAnalysisPath == null && returnTo != null) {
            driveAnalysisReturnScreen = returnTo
        }

        val current = driveAnalysisPath
        if (pushHistory && current != null && current != path) {
            driveAnalysisHistory.add(current)
        }

        driveAnalysisDrive = drive
        driveAnalysisPath = path

        val cached = driveAnalysisCache[path]
        if (cached != null) {
            driveAnalysisItems = cached
            isDriveAnalyzing = false
        } else {
            driveAnalysisItems = emptyList()
            isDriveAnalyzing = true
        }
        currentScreen = Screen.DRIVE_ANALYSIS
    }

    fun openDriveAnalysisFromHome() {
        val drive = drives.firstOrNull() ?: return
        openDriveAnalysis(drive, drive.rootPath, returnTo = Screen.HOME)
    }

    fun updateDriveAnalysis(items: List<DriveAnalysisItem>) {
        driveAnalysisItems = items
        driveAnalysisPath?.let { driveAnalysisCache[it] = items }
    }

    fun updateDriveAnalyzing(v: Boolean) { isDriveAnalyzing = v }

    fun driveAnalysisGoBack(): Boolean {
        if (driveAnalysisHistory.isEmpty()) {
            closeDriveAnalysis()
            return true
        }
        val prev = driveAnalysisHistory.removeAt(driveAnalysisHistory.lastIndex)
        val drive = driveAnalysisDrive ?: return false
        openDriveAnalysis(drive, prev, pushHistory = false)
        return true
    }

    fun closeDriveAnalysis() {
        currentScreen = driveAnalysisReturnScreen
        driveAnalysisDrive = null
        driveAnalysisPath = null
        driveAnalysisItems = emptyList()
        driveAnalysisHistory.clear()
        isDriveAnalyzing = false
    }

    // ==================== ARCHIVE ====================
    var archivePath by mutableStateOf<String?>(null)
        private set
    var archiveEntries by mutableStateOf<List<ArchiveEntry>>(emptyList())
        private set
    var archiveCurrentFolder by mutableStateOf("")
        private set
    var isArchiveLoading by mutableStateOf(false)
        private set
    var archiveNeedsPassword by mutableStateOf(false)
        private set
    var archivePassword by mutableStateOf<String?>(null)
        private set

    var pendingCompressSources by mutableStateOf<List<String>>(emptyList())
        private set
    var pendingCompressDestinationFolder by mutableStateOf<String?>(null)
        private set

    fun openArchiveViewer(path: String, needsPassword: Boolean = false) {
        archivePath = path
        archiveEntries = emptyList()
        archiveCurrentFolder = ""
        archiveNeedsPassword = needsPassword
        archivePassword = null
        isArchiveLoading = true
        currentScreen = Screen.ARCHIVE_VIEWER
    }

    fun updateArchiveEntries(list: List<ArchiveEntry>) {
        archiveEntries = list
        isArchiveLoading = false
    }

    fun updateArchivePassword(p: String?) {
        archivePassword = p
        archiveNeedsPassword = false
    }

    fun updateArchiveNeedsPassword(v: Boolean) { archiveNeedsPassword = v }
    fun updateArchiveLoading(v: Boolean) { isArchiveLoading = v }

    fun updateArchiveCurrentFolder(f: String) { archiveCurrentFolder = f }

    fun closeArchiveViewer() {
        currentScreen = if (archivePath != null) Screen.BROWSER else Screen.HOME
        archivePath = null
        archiveEntries = emptyList()
        archiveCurrentFolder = ""
        archiveNeedsPassword = false
        archivePassword = null
        isArchiveLoading = false
    }

    fun startCompress(sources: List<String>, destFolder: String) {
        pendingCompressSources = sources
        pendingCompressDestinationFolder = destFolder
    }
    fun clearCompress() {
        pendingCompressSources = emptyList()
        pendingCompressDestinationFolder = null
    }

    // ==================== SETTINGS ====================
    var settings by mutableStateOf(AppSettings())
        private set
    var settingsReturnScreen by mutableStateOf(Screen.HOME)
        private set

    private var settingsStorage: SettingsStorage? = null

    fun initSettings(storage: SettingsStorage) {
        settingsStorage = storage
        settings = storage.load()
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
        settingsStorage?.save(newSettings)
    }

    fun openSettings() {
        settingsReturnScreen = currentScreen
        currentScreen = Screen.SETTINGS
    }

    fun closeSettings() {
        currentScreen = settingsReturnScreen
    }

    fun openAbout() {
        currentScreen = Screen.ABOUT
    }

    fun closeAbout() {
        currentScreen = Screen.SETTINGS
    }

    // ==================== ONBOARDING ====================
    var showOnboarding by mutableStateOf(false)
        private set

    fun checkOnboarding() {
        showOnboarding = !settings.onboardingSeen
    }

    fun completeOnboarding() {
        val newSettings = settings.copy(onboardingSeen = true)
        updateSettings(newSettings)
        showOnboarding = false
    }

    fun resetOnboarding() {
        val newSettings = settings.copy(onboardingSeen = false)
        updateSettings(newSettings)
        showOnboarding = true
    }

    // ==================== Category Browsing ====================
    var activeCategoryFilter by mutableStateOf<CategoryType?>(null)
        private set
    var categoryScanType by mutableStateOf<CategoryType?>(null)
        private set
    var categoryFolders by mutableStateOf<List<CategoryFolder>>(emptyList())
        private set
    var isScanningCategoryFolders by mutableStateOf(false)
        private set
    var categoryRootPath by mutableStateOf<String?>(null)
        private set

    fun openCategory(type: CategoryType) {
        activeCategoryFilter = type
        categoryScanType = type
        categoryFolders = emptyList()
        isScanningCategoryFolders = true
        categoryRootPath = null
        categoryAnalysisMode = false
        currentScreen = Screen.CATEGORY_BROWSER
    }

    fun updateCategoryFolders(list: List<CategoryFolder>) { categoryFolders = list }
    fun updateScanningCategoryFolders(value: Boolean) { isScanningCategoryFolders = value }

    fun selectCategoryFolder(folderPath: String) {
        val matchedDrive = drives.firstOrNull { drive ->
            folderPath.startsWith(drive.rootPath.trimEnd('\\', '/'), ignoreCase = true)
        } ?: drives.firstOrNull()

        if (matchedDrive != null) {
            currentDrive = matchedDrive
            currentPath = folderPath
            categoryRootPath = folderPath
            currentScreen = Screen.BROWSER
            resetBrowserScroll()
            clearSelection()
            hideContextMenu()
            hideOptionsMenu()
        }
    }

    fun closeCategory() {
        activeCategoryFilter = null
        categoryScanType = null
        categoryFolders = emptyList()
        categoryRootPath = null
        isScanningCategoryFolders = false
        categoryAnalysisMode = false
        currentScreen = Screen.HOME
        currentDrive = null
        currentPath = null
        currentFiles = emptyList()
        clearSelection()
    }

    fun clearCategoryFilter() {
        activeCategoryFilter = null
        categoryRootPath = null
    }

    // ==================== IMAGE VIEWER ====================
    var viewerImages by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var viewerIndex by mutableStateOf(0)
        private set
    var viewerReturnScreen by mutableStateOf(Screen.BROWSER)
        private set

    fun openImageViewer(images: List<FileItem>, index: Int) {
        if (images.isEmpty()) return
        viewerImages = images
        viewerIndex = index.coerceIn(0, images.size - 1)
        viewerReturnScreen = currentScreen
        currentScreen = Screen.IMAGE_VIEWER
    }

    fun viewerNext() {
        if (viewerImages.isEmpty()) return
        viewerIndex = (viewerIndex + 1) % viewerImages.size
    }

    fun viewerPrev() {
        if (viewerImages.isEmpty()) return
        viewerIndex = if (viewerIndex - 1 < 0) viewerImages.size - 1 else viewerIndex - 1
    }

    fun viewerSetIndex(index: Int) {
        if (index in viewerImages.indices) viewerIndex = index
    }

    fun closeImageViewer() {
        currentScreen = viewerReturnScreen
        viewerImages = emptyList()
        viewerIndex = 0
    }

    val viewerCurrentImage: FileItem?
        get() = viewerImages.getOrNull(viewerIndex)

    // ==================== TEXT VIEWER ====================
    var textViewerFile by mutableStateOf<FileItem?>(null)
        private set
    var textViewerReturnScreen by mutableStateOf(Screen.BROWSER)
        private set

    fun openTextViewer(file: FileItem) {
        textViewerFile = file
        textViewerReturnScreen = currentScreen
        currentScreen = Screen.TEXT_VIEWER
    }

    fun closeTextViewer() {
        currentScreen = textViewerReturnScreen
        textViewerFile = null
    }

    // ==================== AUDIO PLAYER ====================
    var audioCurrentFile by mutableStateOf<FileItem?>(null)
        private set
    var audioIsPlaying by mutableStateOf(false)
        private set
    var audioPositionMs by mutableStateOf(0L)
        private set
    var audioDurationMs by mutableStateOf(0L)
        private set

    fun updateAudioCurrentFile(file: FileItem?) { audioCurrentFile = file }
    fun updateAudioPlaying(value: Boolean) { audioIsPlaying = value }
    fun updateAudioPosition(ms: Long) { audioPositionMs = ms }
    fun updateAudioDuration(ms: Long) { audioDurationMs = ms }

    fun clearAudio() {
        audioCurrentFile = null
        audioIsPlaying = false
        audioPositionMs = 0L
        audioDurationMs = 0L
        musicPlaylist = emptyList()
        musicCurrentIndex = 0
    }

    // ==================== MUSIC PLAYER SCREEN ====================
    var musicPlaylist by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var musicCurrentIndex by mutableStateOf(0)
        private set
    var musicPlayerReturnScreen by mutableStateOf(Screen.BROWSER)
        private set

    var isShuffleOn by mutableStateOf(false)
        private set
    var isRepeatOn by mutableStateOf(false)
        private set

    fun toggleShuffle() { isShuffleOn = !isShuffleOn }
    fun toggleRepeat() { isRepeatOn = !isRepeatOn }

    fun openMusicPlayer(playlist: List<FileItem>, startIndex: Int) {
        if (playlist.isEmpty()) return
        musicPlaylist = playlist
        musicCurrentIndex = startIndex.coerceIn(0, playlist.size - 1)
        musicPlayerReturnScreen = currentScreen
        currentScreen = Screen.MUSIC_PLAYER
    }

    fun setMusicPlaylist(playlist: List<FileItem>, index: Int) {
        if (playlist.isEmpty()) return
        musicPlaylist = playlist
        musicCurrentIndex = index.coerceIn(0, playlist.size - 1)
    }

    fun updateMusicIndex(index: Int) {
        if (index in musicPlaylist.indices) musicCurrentIndex = index
    }

    fun musicNext(): FileItem? {
        if (musicPlaylist.isEmpty()) return null
        musicCurrentIndex = if (isShuffleOn) {
            val candidates = musicPlaylist.indices.filter { it != musicCurrentIndex }
            if (candidates.isEmpty()) musicCurrentIndex else candidates.random()
        } else {
            (musicCurrentIndex + 1) % musicPlaylist.size
        }
        return musicPlaylist.getOrNull(musicCurrentIndex)
    }

    fun musicPrev(): FileItem? {
        if (musicPlaylist.isEmpty()) return null
        musicCurrentIndex = if (isShuffleOn) {
            val candidates = musicPlaylist.indices.filter { it != musicCurrentIndex }
            if (candidates.isEmpty()) musicCurrentIndex else candidates.random()
        } else {
            if (musicCurrentIndex - 1 < 0) musicPlaylist.size - 1 else musicCurrentIndex - 1
        }
        return musicPlaylist.getOrNull(musicCurrentIndex)
    }

    fun musicOnTrackFinished(): FileItem? {
        if (musicPlaylist.isEmpty()) return null
        if (isShuffleOn) return musicNext()
        val isLast = musicCurrentIndex >= musicPlaylist.size - 1
        return when {
            isRepeatOn -> {
                musicCurrentIndex = 0
                musicPlaylist.getOrNull(0)
            }
            isLast -> null
            else -> musicNext()
        }
    }

    fun closeMusicPlayer() {
        currentScreen = musicPlayerReturnScreen
    }

    val musicCurrentTrack: FileItem?
        get() = musicPlaylist.getOrNull(musicCurrentIndex)

    // ==================== VIDEO PLAYER ====================
    var videoCurrentFile by mutableStateOf<FileItem?>(null)
        private set
    var videoIsPlaying by mutableStateOf(false)
        private set
    var videoPositionMs by mutableStateOf(0L)
        private set
    var videoDurationMs by mutableStateOf(0L)
        private set
    var videoVolume by mutableStateOf(1.0)
        private set
    var videoRate by mutableStateOf(1.0)
        private set
    var videoReturnScreen by mutableStateOf(Screen.BROWSER)
        private set
    var videoErrorMessage by mutableStateOf<String?>(null)
        private set
    var videoPlaylist by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var videoCurrentIndex by mutableStateOf(0)
        private set

    fun openVideoPlayer(file: FileItem) {
        videoCurrentFile = file
        videoIsPlaying = false
        videoPositionMs = 0L
        videoDurationMs = 0L
        videoVolume = 1.0
        videoRate = 1.0
        videoErrorMessage = null
        videoReturnScreen = currentScreen
        currentScreen = Screen.VIDEO_PLAYER
    }

    fun openVideoPlayerScreen(playlist: List<FileItem>, index: Int) {
        if (playlist.isEmpty()) return
        videoPlaylist = playlist
        videoCurrentIndex = index.coerceIn(0, playlist.size - 1)
        videoCurrentFile = videoPlaylist[videoCurrentIndex]
        videoIsPlaying = false
        videoPositionMs = 0L
        videoDurationMs = 0L
        videoVolume = 1.0
        videoRate = 1.0
        videoErrorMessage = null
        videoReturnScreen = currentScreen
        currentScreen = Screen.VIDEO_PLAYER
    }

    fun videoNext(): FileItem? {
        if (videoPlaylist.isEmpty()) return null
        videoCurrentIndex = (videoCurrentIndex + 1) % videoPlaylist.size
        val next = videoPlaylist.getOrNull(videoCurrentIndex)
        videoCurrentFile = next
        videoPositionMs = 0L
        videoDurationMs = 0L
        videoErrorMessage = null
        return next
    }

    fun videoPrev(): FileItem? {
        if (videoPlaylist.isEmpty()) return null
        videoCurrentIndex = if (videoCurrentIndex - 1 < 0)
            videoPlaylist.size - 1 else videoCurrentIndex - 1
        val prev = videoPlaylist.getOrNull(videoCurrentIndex)
        videoCurrentFile = prev
        videoPositionMs = 0L
        videoDurationMs = 0L
        videoErrorMessage = null
        return prev
    }

    fun updateVideoCurrentFile(file: FileItem?) { videoCurrentFile = file }
    fun updateVideoPlaying(value: Boolean) { videoIsPlaying = value }
    fun updateVideoPosition(ms: Long) { videoPositionMs = ms }
    fun updateVideoDuration(ms: Long) { videoDurationMs = ms }
    fun updateVideoVolume(v: Double) { videoVolume = v.coerceIn(0.0, 1.0) }
    fun updateVideoRate(r: Double) { videoRate = r.coerceIn(0.25, 4.0) }
    fun updateVideoError(msg: String?) { videoErrorMessage = msg }
    fun clearVideoError() { videoErrorMessage = null }

    fun closeVideoPlayer() {
        currentScreen = videoReturnScreen
        videoCurrentFile = null
        videoIsPlaying = false
        videoPositionMs = 0L
        videoDurationMs = 0L
        videoErrorMessage = null
        videoPlaylist = emptyList()
        videoCurrentIndex = 0
    }

    // ==================== SEARCH ====================
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set
    var searchRootPath by mutableStateOf<String?>(null)
        private set

    fun openSearch(rootPath: String) {
        searchRootPath = rootPath
        searchQuery = ""
        searchResults = emptyList()
        currentScreen = Screen.SEARCH
    }

    fun updateSearchQuery(q: String) { searchQuery = q }
    fun updateSearchResults(list: List<FileItem>) { searchResults = list }
    fun updateSearching(value: Boolean) { isSearching = value }

    fun closeSearch() {
        currentScreen = if (currentDrive != null) Screen.BROWSER else Screen.HOME
        searchQuery = ""
        searchResults = emptyList()
        isSearching = false
        searchRootPath = null
    }

    // ==================== View Preferences ====================
    var sortBy by mutableStateOf(SortBy.NAME)
        private set
    var sortAscending by mutableStateOf(true)
        private set
    var viewMode by mutableStateOf(ViewMode.LIST)
        private set
    var showHidden by mutableStateOf(false)
        private set

    fun setSort(by: SortBy) {
        if (sortBy == by) {
            sortAscending = !sortAscending
        } else {
            sortBy = by
            // ✅ افتراضي ذكي حسب النوع
            sortAscending = when (by) {
                SortBy.NAME, SortBy.TYPE -> true      // A→Z
                SortBy.SIZE, SortBy.DATE -> false     // الأكبر/الأحدث أولاً
            }
        }
    }

    /** تعيين الفرز مع اتجاه صريح */
    fun setSortWithDirection(by: SortBy, ascending: Boolean) {
        sortBy = by
        this.sortAscending = ascending
    }

    fun updateViewMode(mode: ViewMode) { viewMode = mode }
    fun toggleShowHidden() { showHidden = !showHidden }

    // Selection
    var selectedItems by mutableStateOf<Set<String>>(emptySet())
        private set

    fun isSelected(path: String): Boolean = path in selectedItems
    fun toggleSelection(path: String) {
        selectedItems = if (path in selectedItems) selectedItems - path else selectedItems + path
    }
    fun selectOnly(path: String) { selectedItems = setOf(path) }
    fun clearSelection() { selectedItems = emptySet() }

    // Context menu
    var contextMenuTarget by mutableStateOf<FileItem?>(null)
        private set
    var contextMenuX by mutableStateOf(0)
        private set
    var contextMenuY by mutableStateOf(0)
        private set

    fun showContextMenu(item: FileItem, x: Int, y: Int) {
        contextMenuTarget = item; contextMenuX = x; contextMenuY = y
    }
    fun hideContextMenu() { contextMenuTarget = null }

    // Options menu
    var optionsMenuVisible by mutableStateOf(false)
        private set
    var optionsMenuX by mutableStateOf(0)
        private set
    var optionsMenuY by mutableStateOf(0)
        private set

    fun showOptionsMenu(x: Int, y: Int) {
        optionsMenuVisible = true
        optionsMenuX = x
        optionsMenuY = y
    }
    fun hideOptionsMenu() { optionsMenuVisible = false }

    // Clipboard
    var clipboardItems by mutableStateOf<List<String>>(emptyList())
        private set
    var clipboardAction by mutableStateOf<ClipboardAction?>(null)
        private set

    fun startClipboard(action: ClipboardAction) {
        if (selectedItems.isEmpty()) return
        clipboardItems = selectedItems.toList()
        clipboardAction = action
        clearSelection()
    }
    fun cancelClipboard() {
        clipboardItems = emptyList()
        clipboardAction = null
    }

    // Operations
    var activeOperations by mutableStateOf<List<OperationJob>>(emptyList())
        private set
    var operationsDialogVisible by mutableStateOf(false)
        private set
    var operationsScreenVisible by mutableStateOf(false)
        private set

    private val cancelledIds = mutableSetOf<String>()

    fun beginOperation(type: OperationType): String {
        val id = "op_${System.currentTimeMillis()}_${Random.nextInt(10000)}"
        val job = OperationJob(
            id = id, type = type,
            progress = OperationProgress(isPreparing = true)
        )
        activeOperations = activeOperations + job
        cancelledIds.remove(id)
        operationsDialogVisible = true
        operationsScreenVisible = false
        return id
    }

    fun updateOperationProgress(id: String, p: OperationProgress) {
        activeOperations = activeOperations.map {
            if (it.id == id) it.copy(progress = p, isPreparing = p.isPreparing) else it
        }
    }

    fun markOperationFinished(id: String, error: String? = null) {
        activeOperations = activeOperations.map {
            if (it.id == id) it.copy(isFinished = true, errorMessage = error) else it
        }
    }

    fun removeOperation(id: String) {
        activeOperations = activeOperations.filter { it.id != id }
        cancelledIds.remove(id)
        if (activeOperations.isEmpty()) {
            operationsDialogVisible = false
            operationsScreenVisible = false
        }
    }

    fun clearFinishedOperations() {
        activeOperations = activeOperations.filter { !it.isFinished }
        if (activeOperations.isEmpty()) {
            operationsDialogVisible = false
            operationsScreenVisible = false
        }
    }

    fun requestCancelOperation(id: String) { cancelledIds.add(id) }
    fun isOperationCancelled(id: String): Boolean = id in cancelledIds
    fun hideOperationsDialog() { operationsDialogVisible = false }
    fun openOperationsScreen() {
        operationsDialogVisible = false
        operationsScreenVisible = true
    }
    fun closeOperationsScreen() { operationsScreenVisible = false }

    val hasActiveOperations: Boolean get() = activeOperations.isNotEmpty()
    val operationsCount: Int get() = activeOperations.size
    val allOperationsFinished: Boolean
        get() = activeOperations.isNotEmpty() && activeOperations.all { it.isFinished }

    // Conflict
    var pendingConflict by mutableStateOf<ConflictInfo?>(null)
        private set
    private var conflictDeferred: CompletableDeferred<ConflictDecision>? = null

    suspend fun requestConflictDecision(info: ConflictInfo): ConflictDecision {
        pendingConflict = info
        val deferred = CompletableDeferred<ConflictDecision>()
        conflictDeferred = deferred
        return deferred.await()
    }
    fun resolveConflict(decision: ConflictDecision) {
        pendingConflict = null
        conflictDeferred?.complete(decision)
        conflictDeferred = null
    }

    // Delete dialog
    var deleteTarget by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var isDeleteDialogVisible by mutableStateOf(false)
        private set

    fun showDeleteDialog(items: List<FileItem>) {
        deleteTarget = items
        isDeleteDialogVisible = true
    }
    fun hideDeleteDialog() {
        isDeleteDialogVisible = false
        deleteTarget = emptyList()
    }

    // Rename dialog
    var renameTarget by mutableStateOf<FileItem?>(null)
        private set

    fun showRenameDialog(item: FileItem) { renameTarget = item }
    fun hideRenameDialog() { renameTarget = null }

    // Navigation
    fun updateDrives(list: List<Drive>) { drives = list }
    fun updateCategories(list: List<CategoryData>) { categories = list }
    fun updateScanningCategories(value: Boolean) { isScanningCategories = value }
    fun updateLoading(value: Boolean) { isLoading = value }

    fun openDrive(drive: Drive) {
        currentDrive = drive
        currentPath = drive.rootPath
        currentScreen = Screen.BROWSER
        activeCategoryFilter = null
        categoryRootPath = null
        resetBrowserScroll()
        clearSelection()
        hideContextMenu()
        hideOptionsMenu()
    }

    fun navigateTo(path: String) {
        currentPath = path
        resetBrowserScroll()
        clearSelection()
        hideContextMenu()
        hideOptionsMenu()
    }

    fun switchToBrowser() {
        if (currentDrive != null && currentPath != null) {
            currentScreen = Screen.BROWSER
        }
    }

    fun goBack(): Boolean {
        when (currentScreen) {
            Screen.ARCHIVE_VIEWER -> { closeArchiveViewer(); return true }
            Screen.DRIVE_ANALYSIS -> { driveAnalysisGoBack(); return true }
            Screen.ANALYZE -> { closeAnalysis(); return true }
            Screen.ABOUT -> { closeAbout(); return true }
            Screen.SETTINGS -> { closeSettings(); return true }
            Screen.VIDEO_PLAYER -> { closeVideoPlayer(); return true }
            Screen.MUSIC_PLAYER -> { closeMusicPlayer(); return true }
            Screen.TEXT_VIEWER -> { closeTextViewer(); return true }
            Screen.IMAGE_VIEWER -> { closeImageViewer(); return true }
            Screen.SEARCH -> { closeSearch(); return true }
            Screen.CATEGORY_BROWSER -> { closeCategory(); return true }
            Screen.PROGRAMS -> { closePrograms(); return true }
            Screen.BROWSER -> {
                val catRoot = categoryRootPath
                if (catRoot != null && currentPath == catRoot) {
                    currentScreen = Screen.CATEGORY_BROWSER
                    return true
                }

                val path = currentPath
                if (path != null && currentDrive != null) {
                    val driveRoot = currentDrive!!.rootPath.trimEnd('\\', '/')
                    val cleanPath = path.trimEnd('\\', '/')
                    if (cleanPath.equals(driveRoot, ignoreCase = true) ||
                        cleanPath.length <= driveRoot.length
                    ) {
                        if (activeCategoryFilter != null) {
                            currentScreen = Screen.CATEGORY_BROWSER
                        } else {
                            closeBrowser()
                        }
                        return true
                    }
                }
            }
            Screen.HOME -> { }
        }
        return false
    }

    fun closeBrowser() {
        currentDrive = null
        currentPath = null
        currentFiles = emptyList()
        currentScreen = Screen.HOME
        activeCategoryFilter = null
        categoryRootPath = null
        resetBrowserScroll()
        clearSelection()
        hideContextMenu()
        hideOptionsMenu()
    }

    fun updateFiles(list: List<FileItem>) { currentFiles = list }
    fun refreshFiles() { filesRevision++ }
}