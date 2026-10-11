package com.ah.filemanagerplus.presentation.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.core.filesystem.CategoryFolder
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.presentation.browser.createThumbnailLoader
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.components.AHContextMenu
import com.ah.filemanagerplus.presentation.components.ContextMenuItem
import com.ah.filemanagerplus.presentation.theme.AHColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val CATEGORY_THUMB_LOADER by lazy { createThumbnailLoader() }

data class SearchHit(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val parentFolder: String
)

@Composable
fun CategoryBrowserScreen(
    type: CategoryType,
    folders: List<CategoryFolder>,
    isScanning: Boolean,
    analysisMode: Boolean,
    onToggleAnalysis: () -> Unit,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onFolderClick: (CategoryFolder) -> Unit,
    onFileOpen: (SearchHit) -> Unit,
    onAddToFavorites: (CategoryFolder) -> Unit,
    onOpenInExplorer: (CategoryFolder) -> Unit,
    onCopyPath: (CategoryFolder) -> Unit,
    onCopy: (CategoryFolder) -> Unit,
    onMove: (CategoryFolder) -> Unit,
    onDelete: (CategoryFolder) -> Unit,
    onRename: (CategoryFolder) -> Unit,
    onShare: (CategoryFolder) -> Unit,
    onProperties: (CategoryFolder) -> Unit
) {
    val label = categoryLabel(type)

    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchHits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var contextTarget by remember { mutableStateOf<CategoryFolder?>(null) }
    var contextX by remember { mutableStateOf(0) }
    var contextY by remember { mutableStateOf(0) }

    // ==================== البحث الشامل (debounced) ====================
    LaunchedEffect(searchQuery, folders) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            searchHits = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        delay(250)
        isSearching = true
        searchHits = emptyList()

        val lower = q.lowercase()
        val allHits = mutableListOf<SearchHit>()

        // 1) بحث في أسماء المجلدات
        for (f in folders) {
            if (f.name.contains(q, ignoreCase = true)) {
                allHits.add(
                    SearchHit(
                        name = f.name,
                        path = f.path,
                        isDirectory = true,
                        size = f.totalSize,
                        parentFolder = f.path.substringBeforeLast('\\', "")
                    )
                )
            }
        }

        // 2) بحث داخل كل مجلد
        val results = withContext(Dispatchers.IO) {
            val merged = mutableListOf<SearchHit>()
            for (folder in folders) {
                if (merged.size >= 500) break
                merged.addAll(scanFolderContents(folder.path, lower, maxHits = 100))
            }
            merged
        }
        allHits.addAll(results)
        searchHits = allHits.take(500)
        isSearching = false
    }

    // ==================== ترتيب للتحليل ====================
    val analysisList = remember(folders) {
        folders.sortedByDescending { it.totalSize }
    }
    val analysisTotal = remember(analysisList) {
        analysisList.sumOf { it.totalSize }.coerceAtLeast(1L)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AHAppBar(
            title = when {
                searchOpen -> "بحث في $label"
                isScanning && folders.isEmpty() -> "$label — جارٍ المسح..."
                analysisMode -> "تحليل $label"
                else -> "$label (${folders.size} مجلد)"
            },
            onBackClick = {
                when {
                    searchOpen -> { searchOpen = false; searchQuery = "" }
                    analysisMode -> onToggleAnalysis()
                    else -> onBack()
                }
            },
            onMenuClick = onMenuClick,
            onSearchClick = {
                searchOpen = !searchOpen
                if (!searchOpen) searchQuery = ""
            },
            onAnalyzeClick = {
                if (!searchOpen) onToggleAnalysis()
            }
        )

        if (searchOpen) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClose = { searchOpen = false; searchQuery = "" }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when {
            folders.isEmpty() && isScanning -> ScanningView(label = label)
            folders.isEmpty() -> EmptyView(label = label)

            searchOpen -> {
                if (isSearching && searchHits.isEmpty()) {
                    LoadingSearch()
                } else if (searchHits.isEmpty()) {
                    NoSearchResults(searchQuery)
                } else {
                    SearchResultsList(
                        hits = searchHits,
                        onFolderClick = { hit ->
                            val folder = folders.firstOrNull { it.path == hit.path }
                            if (folder != null) onFolderClick(folder)
                        },
                        onFileClick = onFileOpen
                    )
                }
            }

            analysisMode -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(analysisList, key = { "an_${it.path}" }) { folder ->
                        AnalysisFolderRow(
                            folder = folder,
                            percent = folder.totalSize.toDouble() / analysisTotal,
                            onClick = { onFolderClick(folder) },
                            onRightClick = { x, y ->
                                contextTarget = folder
                                contextX = x
                                contextY = y
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(folders, key = { it.path }) { folder ->
                        CategoryFolderRow(
                            folder = folder,
                            onClick = { onFolderClick(folder) },
                            onRightClick = { x, y ->
                                contextTarget = folder
                                contextX = x
                                contextY = y
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }

    val target = contextTarget
    if (target != null) {
        AHContextMenu(
            offsetX = contextX,
            offsetY = contextY,
            onDismiss = { contextTarget = null },
            items = listOf(
                ContextMenuItem(icon = "📂", label = "فتح", onClick = { onFolderClick(target) }),
                ContextMenuItem(icon = "🖥️", label = "فتح في المستكشف", onClick = { onOpenInExplorer(target) }),
                ContextMenuItem(icon = "", label = "", isDivider = true),
                ContextMenuItem(icon = "⭐", label = "إضافة إلى المفضلة", onClick = { onAddToFavorites(target) }),
                ContextMenuItem(icon = "📋", label = "نسخ المسار", onClick = { onCopyPath(target) }),
                ContextMenuItem(icon = "📤", label = "مشاركة", onClick = { onShare(target) }),
                ContextMenuItem(icon = "", label = "", isDivider = true),
                ContextMenuItem(icon = "📋", label = "نسخ", onClick = { onCopy(target) }),
                ContextMenuItem(icon = "✂️", label = "نقل", onClick = { onMove(target) }),
                ContextMenuItem(icon = "✏️", label = "إعادة تسمية", onClick = { onRename(target) }),
                ContextMenuItem(icon = "🗑️", label = "حذف", onClick = { onDelete(target) }),
                ContextMenuItem(icon = "", label = "", isDivider = true),
                ContextMenuItem(icon = "ℹ️", label = "خصائص", onClick = { onProperties(target) })
            )
        )
    }
}

// ==================== Scanning Helper ====================

private suspend fun scanFolderContents(
    folderPath: String,
    query: String,
    maxHits: Int = 100
): List<SearchHit> = withContext(Dispatchers.IO) {
    val root = File(folderPath)
    if (!root.exists() || !root.isDirectory) return@withContext emptyList()

    val results = mutableListOf<SearchHit>()
    val stack = ArrayDeque<File>()
    stack.addLast(root)
    val visited = mutableSetOf<String>()

    val skipFolders = setOf(
        "Windows", "\$Recycle.Bin", "System Volume Information",
        "Program Files", "Program Files (x86)", "ProgramData",
        "AppData", "node_modules", ".git", ".gradle",
        ".android", ".idea", ".vscode", "build", "target", "out"
    )

    while (stack.isNotEmpty() && results.size < maxHits) {
        val current = stack.removeLast()
        val key = try { current.absolutePath.lowercase() }
                  catch (e: Exception) { current.absolutePath }
        if (!visited.add(key)) continue

        val children = try { current.listFiles() } catch (e: Exception) { null } ?: continue
        for (child in children) {
            if (child.isDirectory) {
                if (child.name in skipFolders) continue
                if (child.isHidden) continue
                stack.addLast(child)
            } else {
                if (child.name.contains(query, ignoreCase = true)) {
                    results.add(
                        SearchHit(
                            name = child.name,
                            path = child.absolutePath,
                            isDirectory = false,
                            size = try { child.length() } catch (e: Exception) { 0L },
                            parentFolder = folderPath
                        )
                    )
                    if (results.size >= maxHits) break
                }
            }
        }
    }
    results
}

// ==================== Search UI ====================

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🔍", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(AHColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            ) { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "ابحث في الأسماء والمحتوى...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    inner()
                }
            }
        }

        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchResultsList(
    hits: List<SearchHit>,
    onFolderClick: (SearchHit) -> Unit,
    onFileClick: (SearchHit) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(hits, key = { it.path }) { hit ->
            SearchHitRow(
                hit = hit,
                onClick = { if (hit.isDirectory) onFolderClick(hit) else onFileClick(hit) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun SearchHitRow(hit: SearchHit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AHColors.CategoryFolder.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hit.isDirectory) "📁" else iconForName(hit.name),
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = hit.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (hit.isDirectory) hit.path else "${formatBytes(hit.size)} • ${hit.parentFolder}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LoadingSearch() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔍", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "جارٍ البحث في المحتوى...",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== Regular Row ====================

@Composable
private fun CategoryFolderRow(
    folder: CategoryFolder,
    onClick: () -> Unit,
    onRightClick: (Int, Int) -> Unit
) {
    var rowPosition by remember { mutableStateOf(Offset.Zero) }

    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = folder.previewImagePath) {
        value = if (folder.previewImagePath != null) {
            withContext(Dispatchers.IO) {
                CATEGORY_THUMB_LOADER.loadImage(folder.previewImagePath)
            }
        } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowPosition = it.positionInRoot() }
            .pointerInput(folder.path) {
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${folder.fileCount} ملف",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = folder.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = folder.path,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AHColors.CategoryFolder.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = folder.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(text = "📁", fontSize = 26.sp)
            }
        }
    }
}

// ==================== Analysis Row ====================

@Composable
private fun AnalysisFolderRow(
    folder: CategoryFolder,
    percent: Double,
    onClick: () -> Unit,
    onRightClick: (Int, Int) -> Unit
) {
    var rowPosition by remember { mutableStateOf(Offset.Zero) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { rowPosition = it.positionInRoot() }
                .pointerInput(folder.path) {
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📁", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percent.toFloat().coerceIn(0.02f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFA8D42B), Color(0xFF42A5F5))
                                )
                            )
                    )
                    Text(
                        text = folder.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xCC000000),
                                offset = Offset(1.5f, 1.5f),
                                blurRadius = 4f
                            )
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatBytes(folder.totalSize),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "عناصر ${folder.fileCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "${String.format(Locale.US, "%.2f", percent * 100)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

// ==================== Views ====================

@Composable
private fun ScanningView(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔍", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "جارٍ البحث عن مجلدات $label...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyView(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "لا يوجد", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "لم يتم العثور على أي مجلدات تحتوي $label",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoSearchResults(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔍", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "لا توجد نتائج لـ \"$query\"",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== Helpers ====================

private fun categoryLabel(type: CategoryType): String = when (type) {
    CategoryType.IMAGES -> "صور"
    CategoryType.VIDEOS -> "فيديو"
    CategoryType.AUDIO -> "صوتي"
    CategoryType.DOCUMENTS -> "وثائق"
    CategoryType.DOWNLOADS -> "تنزيلات"
}

private fun iconForName(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "🖼️"
        "mp4", "mkv", "avi", "mov", "webm" -> "🎬"
        "mp3", "wav", "ogg", "m4a", "flac" -> "🎵"
        "pdf" -> "📕"
        "doc", "docx" -> "📘"
        "txt", "md" -> "📄"
        "zip", "rar", "7z" -> "📦"
        "exe", "msi" -> "⚙️"
        else -> "📄"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    val tb = gb * 1024
    return when {
        bytes >= tb -> "${dec(bytes / tb)} TB"
        bytes >= gb -> "${dec(bytes / gb)} GB"
        bytes >= mb -> "${dec(bytes / mb)} MB"
        bytes >= kb -> "${dec(bytes / kb)} KB"
        else -> "$bytes B"
    }
}

private fun dec(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    val scaled = (value * 100).toLong()
    val whole = scaled / 100
    val frac = (scaled % 100).toInt()
    return if (frac == 0) whole.toString()
    else "$whole.${frac.toString().padStart(2, '0').trimEnd('0')}"
}