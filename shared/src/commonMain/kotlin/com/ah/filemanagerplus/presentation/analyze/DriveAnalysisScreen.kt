package com.ah.filemanagerplus.presentation.analyze

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
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
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.data.model.DriveAnalysisItem
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.components.AHContextMenu
import com.ah.filemanagerplus.presentation.components.ContextMenuItem
import java.util.Locale

@Composable
fun DriveAnalysisScreen(
    drive: Drive?,
    path: String?,
    items: List<DriveAnalysisItem>,
    isAnalyzing: Boolean,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onItemClick: (DriveAnalysisItem) -> Unit,
    onOpenInExplorer: (DriveAnalysisItem) -> Unit,
    onAddToFavorites: (DriveAnalysisItem) -> Unit,
    onCopyPath: (DriveAnalysisItem) -> Unit,
    onExtract: (DriveAnalysisItem) -> Unit,
    onShare: (DriveAnalysisItem) -> Unit,
    onDelete: (DriveAnalysisItem) -> Unit,
    onRename: (DriveAnalysisItem) -> Unit
) {
    if (drive == null || path == null) return

    var contextTarget by remember { mutableStateOf<DriveAnalysisItem?>(null) }
    var contextX by remember { mutableStateOf(0) }
    var contextY by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AHAppBar(
            title = "تحليل: ${path.substringAfterLast('\\').ifBlank { drive.displayTitle }}",
            onBackClick = onBack,
            onMenuClick = onMenuClick
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (isAnalyzing && items.isEmpty()) {
            AnalyzingState()
            return@Column
        }

        if (items.isEmpty()) {
            EmptyState()
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(items, key = { it.path }) { item ->
                DriveAnalysisRow(
                    item = item,
                    onClick = { onItemClick(item) },
                    onRightClick = { x, y ->
                        contextTarget = item
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

    val target = contextTarget
    if (target != null) {
        val ext = target.name.substringAfterLast('.', "").lowercase()
        val isArchive = ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

        AHContextMenu(
            offsetX = contextX,
            offsetY = contextY,
            onDismiss = { contextTarget = null },
            items = buildList {
                add(ContextMenuItem(
                    icon = "📂",
                    label = "فتح",
                    onClick = { onItemClick(target) }
                ))
                add(ContextMenuItem(
                    icon = "🖥️",
                    label = "فتح في المستكشف",
                    onClick = { onOpenInExplorer(target) }
                ))
                add(ContextMenuItem(
                    icon = "↗️",
                    label = "فتح باستخدام",
                    onClick = { onOpenInExplorer(target) }
                ))
                add(ContextMenuItem(icon = "", label = "", isDivider = true))

                if (isArchive) {
                    add(ContextMenuItem(
                        icon = "📦",
                        label = "استخراج هنا",
                        onClick = { onExtract(target) }
                    ))
                    add(ContextMenuItem(icon = "", label = "", isDivider = true))
                }

                add(ContextMenuItem(
                    icon = "⭐",
                    label = "إضافة إلى المفضلة",
                    onClick = { onAddToFavorites(target) }
                ))
                add(ContextMenuItem(
                    icon = "📋",
                    label = "نسخ المسار",
                    onClick = { onCopyPath(target) }
                ))
                add(ContextMenuItem(
                    icon = "📤",
                    label = "مشاركة",
                    onClick = { onShare(target) }
                ))
                add(ContextMenuItem(icon = "", label = "", isDivider = true))

                add(ContextMenuItem(
                    icon = "✏️",
                    label = "إعادة تسمية",
                    onClick = { onRename(target) }
                ))
                add(ContextMenuItem(
                    icon = "🗑️",
                    label = "حذف",
                    onClick = { onDelete(target) }
                ))
            }
        )
    }
}

@Composable
private fun DriveAnalysisRow(
    item: DriveAnalysisItem,
    onClick: () -> Unit,
    onRightClick: (Int, Int) -> Unit
) {
    var rowPosition by remember { mutableStateOf(Offset.Zero) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { rowPosition = it.positionInRoot() }
                .pointerInput(item.path) {
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
            // 1) Icon
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (item.isDirectory) "📁" else iconFor(item.name),
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 2) Bar + name + details
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // الشريط الملوّن
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(
                                (item.percent / 100.0).toFloat().coerceIn(0.02f, 1f)
                            )
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFA8D42B), Color(0xFF42A5F5))
                                )
                            )
                    )

                    // النص فقط — أبيض مع ظل خفيف
                    Text(
                        text = item.name,
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
                        text = formatBytes(item.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (item.isDirectory && item.itemCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عناصر ${item.itemCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 3) Percent
            Text(
                text = "${String.format(Locale.US, "%.2f", item.percent)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun AnalyzingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📊", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "جارٍ التحليل...",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "المجلد فارغ",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun iconFor(name: String): String {
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