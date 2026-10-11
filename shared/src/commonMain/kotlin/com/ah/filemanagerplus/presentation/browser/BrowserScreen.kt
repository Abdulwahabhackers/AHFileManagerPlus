package com.ah.filemanagerplus.presentation.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.core.filesystem.FileSorter
import com.ah.filemanagerplus.data.AppState
import com.ah.filemanagerplus.data.SortBy
import com.ah.filemanagerplus.data.ViewMode
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.components.AHContextMenu
import com.ah.filemanagerplus.presentation.components.BottomBarAction
import com.ah.filemanagerplus.presentation.components.Breadcrumb
import com.ah.filemanagerplus.presentation.components.ContextMenuItem
import com.ah.filemanagerplus.presentation.components.SelectionBottomBar
import com.ah.filemanagerplus.presentation.theme.AHColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ARCHIVE_EXTS_LOCAL = setOf(
    "zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz"
)

@Composable
fun BrowserScreen(
    appState: AppState,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onDriveClick: () -> Unit,
    onSegmentClick: (Int) -> Unit,
    onFolderClick: (FileItem) -> Unit,
    onFileOpen: (FileItem) -> Unit,
    onCopyOrMove: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onAddToFavorites: (FileItem) -> Unit,
    onCompress: (FileItem) -> Unit,
    onArchivePreview: (FileItem) -> Unit,
    onArchiveExtractHere: (FileItem) -> Unit,
    onArchiveExtractTo: (FileItem) -> Unit
) {
    val drive: Drive = appState.currentDrive ?: return
    val path: String = appState.currentPath ?: return
    val rawFiles = appState.currentFiles
    val segments = rememberSegments(drive.rootPath, path)
    val title = if (segments.isEmpty()) drive.displayTitle else segments.last()

    val hasSelection = appState.selectedItems.isNotEmpty()
    val hasClipboard = appState.clipboardItems.isNotEmpty()
    val categoryFilter = appState.activeCategoryFilter

    val files = remember(
        rawFiles,
        appState.sortBy,
        appState.sortAscending,
        appState.showHidden,
        categoryFilter
    ) {
        val filtered = if (categoryFilter != null) {
            val allowed = extensionsForCategory(categoryFilter)
            rawFiles.filter { item ->
                item.isDirectory || item.extension.lowercase() in allowed
            }
        } else rawFiles

        FileSorter.apply(
            files = filtered,
            sortBy = appState.sortBy,
            ascending = appState.sortAscending,
            showHidden = appState.showHidden
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasSelection) {
            SelectionAppBar(
                count = appState.selectedItems.size,
                onCancel = { appState.clearSelection() },
                onMenuClick = onMenuClick
            )
        } else {
            AHAppBar(
                title = title,
                onMenuClick = onMenuClick,
                onBackClick = onBack,
                onSearchClick = onSearchClick,
                onAnalyzeClick = onAnalyzeClick,
                onSettingsClick = onSettingsClick,
                onMoreClick = { appState.showOptionsMenu(200, 80) }
            )
        }

        Breadcrumb(
            driveLetter = drive.letter,
            segments = segments,
            usedPercent = drive.usedPercent,
            onHomeClick = onHomeClick,
            onDriveClick = onDriveClick,
            onSegmentClick = onSegmentClick
        )

        if (categoryFilter != null) {
            CategoryFilterChip(
                type = categoryFilter,
                onClear = { appState.clearCategoryFilter() }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Box(modifier = Modifier.weight(1f)) {
            if (files.isEmpty()) {
                EmptyFolder(
                    showHidden = appState.showHidden,
                    isFiltered = categoryFilter != null
                )
            } else {
                when (appState.viewMode) {
                    ViewMode.LIST -> FilesList(
                        files = files,
                        appState = appState,
                        onClick = { file ->
                            if (hasSelection) appState.toggleSelection(file.path)
                            else if (file.isDirectory) onFolderClick(file) else onFileOpen(file)
                        },
                        onRightClick = { file, x, y ->
                            if (!appState.isSelected(file.path)) appState.selectOnly(file.path)
                            appState.showContextMenu(file, x, y)
                        }
                    )
                    ViewMode.GRID -> FilesGrid(
                        files = files,
                        appState = appState,
                        onClick = { file ->
                            if (hasSelection) appState.toggleSelection(file.path)
                            else if (file.isDirectory) onFolderClick(file) else onFileOpen(file)
                        },
                        onRightClick = { file, x, y ->
                            if (!appState.isSelected(file.path)) appState.selectOnly(file.path)
                            appState.showContextMenu(file, x, y)
                        }
                    )
                }
            }
        }

        if (hasSelection && !hasClipboard) {
            SelectionBottomBar(
                actions = listOf(
                    BottomBarAction(icon = "✕", label = "إلغاء", onClick = { appState.clearSelection() }),
                    BottomBarAction(icon = "📋", label = "نسخ", onClick = { onCopyOrMove(true) }),
                    BottomBarAction(icon = "✂️", label = "نقل", onClick = { onCopyOrMove(false) }),
                    BottomBarAction(icon = "🗑️", label = "حذف", onClick = onDelete),
                    BottomBarAction(icon = "✏️", label = "تسمية", onClick = onRename)
                )
            )
        }
    }

    if (appState.optionsMenuVisible) {
        OptionsMenu(
            currentSort = appState.sortBy,
            ascending = appState.sortAscending,
            currentView = appState.viewMode,
            showHidden = appState.showHidden,
            offsetX = appState.optionsMenuX,
            offsetY = appState.optionsMenuY,
            onDismiss = { appState.hideOptionsMenu() },
            onSortBy = { by, asc -> appState.setSortWithDirection(by, asc) },
            onViewMode = { vm -> appState.updateViewMode(vm) },
            onToggleHidden = { appState.toggleShowHidden() }
        )
    }

    // ==================== قائمة السياق ====================
    val contextTarget = appState.contextMenuTarget
    if (contextTarget != null) {
        val ext = contextTarget.name.substringAfterLast('.', "").lowercase()
        val isArchive = ext in ARCHIVE_EXTS_LOCAL

        AHContextMenu(
            offsetX = appState.contextMenuX,
            offsetY = appState.contextMenuY,
            onDismiss = { appState.hideContextMenu() },
            items = buildList {
                // ============ للمجلدات ============
                if (contextTarget.isDirectory) {
                    add(ContextMenuItem(
                        icon = "📂", label = "فتح",
                        onClick = { onFolderClick(contextTarget) }
                    ))
                    add(ContextMenuItem(
                        icon = "📦", label = "ضغط إلى ZIP",
                        onClick = { onCompress(contextTarget) }
                    ))
                }
                // ============ للملفات المضغوطة ============
                else if (isArchive) {
                    add(ContextMenuItem(
                        icon = "👁️", label = "معاينة",
                        onClick = { onArchivePreview(contextTarget) }
                    ))
                    add(ContextMenuItem(
                        icon = "⬆️", label = "استخراج هنا",
                        onClick = { onArchiveExtractHere(contextTarget) }
                    ))
                    add(ContextMenuItem(
                        icon = "📁", label = "استخراج إلى...",
                        onClick = { onArchiveExtractTo(contextTarget) }
                    ))
                }
                // ============ للملفات العادية ============
                else {
                    add(ContextMenuItem(
                        icon = "🔗", label = "فتح",
                        onClick = { onFileOpen(contextTarget) }
                    ))
                }

                add(ContextMenuItem(icon = "", label = "", isDivider = true))

                add(ContextMenuItem(
                    icon = "⭐",
                    label = if (appState.isFavorite(contextTarget.path)) "إزالة من المفضلة"
                            else "إضافة إلى المفضلة",
                    onClick = {
                        if (appState.isFavorite(contextTarget.path)) {
                            appState.removeFromFavorites(contextTarget.path)
                        } else {
                            onAddToFavorites(contextTarget)
                        }
                    }
                ))
                add(ContextMenuItem(
                    icon = "📋", label = "نسخ",
                    onClick = { onCopyOrMove(true) }
                ))
                add(ContextMenuItem(
                    icon = "✂️", label = "نقل",
                    onClick = { onCopyOrMove(false) }
                ))
                add(ContextMenuItem(
                    icon = "✏️", label = "إعادة تسمية",
                    onClick = onRename
                ))
                add(ContextMenuItem(
                    icon = "🗑️", label = "حذف",
                    onClick = onDelete
                ))
                add(ContextMenuItem(icon = "", label = "", isDivider = true))
                add(ContextMenuItem(
                    icon = "ℹ️", label = "خصائص",
                    enabled = false
                ))
            }
        )
    }
}

// ==================== Category Filter Chip ====================

@Composable
private fun CategoryFilterChip(
    type: CategoryType,
    onClear: () -> Unit
) {
    val (label, color, symbol) = when (type) {
        CategoryType.IMAGES -> Triple("صور", AHColors.CategoryImages, "🖼️")
        CategoryType.VIDEOS -> Triple("فيديو", AHColors.CategoryVideo, "🎬")
        CategoryType.AUDIO -> Triple("صوتي", AHColors.CategoryAudio, "🎵")
        CategoryType.DOCUMENTS -> Triple("وثائق", AHColors.CategoryDocuments, "📄")
        CategoryType.DOWNLOADS -> Triple("تنزيلات", AHColors.Primary, "⬇️")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.15f))
                .clickable(onClick = onClear)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = symbol, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label فقط",
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "✕", fontSize = 12.sp, color = color)
        }
    }
}

// ==================== Options Menu ====================

@Composable
private fun OptionsMenu(
    currentSort: SortBy,
    ascending: Boolean,
    currentView: ViewMode,
    showHidden: Boolean,
    offsetX: Int,
    offsetY: Int,
    onDismiss: () -> Unit,
    onSortBy: (SortBy, Boolean) -> Unit,
    onViewMode: (ViewMode) -> Unit,
    onToggleHidden: () -> Unit
) {
    val items = mutableListOf<ContextMenuItem>()

    // ==================== الفرز ====================
    items.add(ContextMenuItem(icon = "", label = "— الفرز —", enabled = false))

    val sortOptions = listOf(
        Triple(SortBy.NAME, true, "الاسم  ↑ (A→Z)"),
        Triple(SortBy.NAME, false, "الاسم  ↓ (Z→A)"),
        Triple(SortBy.SIZE, true, "الحجم  ↑ (الأصغر أولاً)"),
        Triple(SortBy.SIZE, false, "الحجم  ↓ (الأكبر أولاً)"),
        Triple(SortBy.DATE, true, "التاريخ  ↑ (الأقدم أولاً)"),
        Triple(SortBy.DATE, false, "التاريخ  ↓ (الأحدث أولاً)"),
        Triple(SortBy.TYPE, true, "النوع  ↑ (A→Z)"),
        Triple(SortBy.TYPE, false, "النوع  ↓ (Z→A)")
    )

    for ((by, asc, label) in sortOptions) {
        val isSelected = currentSort == by && ascending == asc
        items.add(
            ContextMenuItem(
                icon = if (isSelected) "◉" else "○",
                label = label,
                onClick = { onSortBy(by, asc) }
            )
        )
    }

    items.add(ContextMenuItem(icon = "", label = "", isDivider = true))

    // ==================== العرض ====================
    items.add(ContextMenuItem(icon = "", label = "— العرض —", enabled = false))
    items.add(ContextMenuItem(
        icon = if (currentView == ViewMode.LIST) "◉" else "○",
        label = "قائمة",
        onClick = { onViewMode(ViewMode.LIST) }
    ))
    items.add(ContextMenuItem(
        icon = if (currentView == ViewMode.GRID) "◉" else "○",
        label = "شبكة",
        onClick = { onViewMode(ViewMode.GRID) }
    ))

    items.add(ContextMenuItem(icon = "", label = "", isDivider = true))

    // ==================== إخفاء ====================
    items.add(ContextMenuItem(
        icon = if (showHidden) "☑" else "☐",
        label = "إظهار الملفات المخفية",
        onClick = { onToggleHidden() }
    ))

    AHContextMenu(
        items = items,
        offsetX = offsetX,
        offsetY = offsetY,
        onDismiss = onDismiss
    )
}

// ==================== Lists ====================

@Composable
private fun FilesList(
    files: List<FileItem>,
    appState: AppState,
    onClick: (FileItem) -> Unit,
    onRightClick: (FileItem, Int, Int) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = appState.browserScrollIndex,
        initialFirstVisibleItemScrollOffset = appState.browserScrollOffset
    )

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (i, o) ->
            appState.saveBrowserScroll(i, o)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(files, key = { it.path }) { file ->
            FileRow(
                file = file,
                isSelected = appState.isSelected(file.path),
                onClick = { onClick(file) },
                onRightClick = { x, y -> onRightClick(file, x, y) }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun FilesGrid(
    files: List<FileItem>,
    appState: AppState,
    onClick: (FileItem) -> Unit,
    onRightClick: (FileItem, Int, Int) -> Unit
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = appState.browserScrollIndex,
        initialFirstVisibleItemScrollOffset = appState.browserScrollOffset
    )

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (i, o) ->
            appState.saveBrowserScroll(i, o)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(files, key = { it.path }) { file ->
            FileGridItem(
                file = file,
                isSelected = appState.isSelected(file.path),
                onClick = { onClick(file) },
                onRightClick = { x, y -> onRightClick(file, x, y) }
            )
        }
    }
}

// ==================== FileRow ====================

@Composable
private fun FileRow(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRightClick: (Int, Int) -> Unit
) {
    var rowPosition by remember { mutableStateOf(Offset.Zero) }
    val bgColor = if (isSelected) AHColors.Primary.copy(alpha = 0.20f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .onGloballyPositioned { rowPosition = it.positionInRoot() }
            .pointerInput(file.path) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press &&
                            event.buttons.isSecondaryPressed
                        ) {
                            val change = event.changes.first()
                            val absolute = rowPosition + change.position
                            onRightClick(absolute.x.toInt(), absolute.y.toInt())
                            change.consume()
                        }
                    }
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDate(file.lastModified),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = file.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (file.isHidden)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (file.isDirectory) formatItemCount(file.itemCount)
                       else file.readableSize(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(getListIconBackground(file)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = getListIconSymbol(file), fontSize = 20.sp)
        }
    }
}

// ==================== Empty ====================

@Composable
private fun EmptyFolder(showHidden: Boolean, isFiltered: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                isFiltered -> "لا توجد ملفات من هذا النوع"
                showHidden -> "هذا المجلد فارغ"
                else -> "لا توجد ملفات مرئية"
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Selection AppBar ====================

@Composable
private fun SelectionAppBar(
    count: Int,
    onCancel: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AHColors.AppBarLight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "☰", color = Color.White, fontSize = 20.sp)
        }

        Text(
            text = "$count محدد",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✕", color = Color.White, fontSize = 20.sp)
        }
    }
}

// ==================== Helpers ====================

private fun rememberSegments(driveRoot: String, path: String): List<String> {
    val root = driveRoot.trimEnd('\\', '/')
    val current = path.trimEnd('\\', '/')
    if (current.equals(root, ignoreCase = true)) return emptyList()
    val relative = current.removePrefix(root).trimStart('\\', '/')
    if (relative.isBlank()) return emptyList()
    return relative.split('\\', '/').filter { it.isNotBlank() }
}

private fun extensionsForCategory(type: CategoryType): Set<String> = when (type) {
    CategoryType.IMAGES -> setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "heic"
    )
    CategoryType.VIDEOS -> setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp", "ts"
    )
    CategoryType.AUDIO -> setOf(
        "mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "opus", "amr"
    )
    CategoryType.DOCUMENTS -> setOf(
        "pdf", "doc", "docx", "txt", "md", "rtf", "odt",
        "xls", "xlsx", "csv", "ppt", "pptx", "epub"
    )
    CategoryType.DOWNLOADS -> emptySet()
}

@Composable
private fun getListIconBackground(file: FileItem): Color {
    if (file.isDirectory) return AHColors.CategoryFolder.copy(alpha = 0.15f)
    val ext = file.extension.lowercase()
    return when (ext) {
        "zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz" ->
            Color(0xFFFBC02D).copy(alpha = 0.20f)
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> AHColors.CategoryImages.copy(alpha = 0.15f)
        "mp4", "mkv", "avi", "mov", "webm" -> AHColors.CategoryVideo.copy(alpha = 0.15f)
        "mp3", "wav", "ogg", "m4a", "flac" -> AHColors.CategoryAudio.copy(alpha = 0.15f)
        "pdf", "doc", "docx", "txt", "md" -> AHColors.CategoryDocuments.copy(alpha = 0.15f)
        "exe", "msi", "apk" -> AHColors.CategoryApps.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun getListIconSymbol(file: FileItem): String {
    if (file.isDirectory) return "📁"
    val ext = file.extension.lowercase()
    return when (ext) {
        "zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz" -> "📦"
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "🖼️"
        "mp4", "mkv", "avi", "mov", "webm" -> "🎬"
        "mp3", "wav", "ogg", "m4a", "flac" -> "🎵"
        "pdf" -> "📕"
        "doc", "docx" -> "📘"
        "txt", "md" -> "📄"
        "exe", "msi" -> "⚙️"
        "apk" -> "🤖"
        else -> "📄"
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return try {
        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(timestamp))
    } catch (e: Exception) { "" }
}

private fun formatItemCount(count: Int): String = when {
    count == 0 -> "0 عنصر"
    count == 1 -> "1 عنصر"
    count == 2 -> "2 عنصر"
    count in 3..10 -> "$count عناصر"
    else -> "$count عنصر"
}