package com.ah.filemanagerplus.presentation.archive

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ah.filemanagerplus.core.archive.ArchiveEntry
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.components.AHContextMenu
import com.ah.filemanagerplus.presentation.components.ContextMenuItem
import com.ah.filemanagerplus.presentation.theme.AHColors

@Composable
fun ArchiveViewerScreen(
    archivePath: String?,
    entries: List<ArchiveEntry>,
    currentFolder: String,
    isLoading: Boolean,
    needsPassword: Boolean,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onFolderClick: (String) -> Unit,
    onPreviewFile: (ArchiveEntry) -> Unit,
    onExtractAll: () -> Unit,
    onExtractEntry: (ArchiveEntry) -> Unit,
    onExtractEntryTo: (ArchiveEntry) -> Unit,
    onCopyEntryPath: (ArchiveEntry) -> Unit,
    onPasswordEntered: (String) -> Unit
) {
    if (archivePath == null) return

    var contextEntry by remember { mutableStateOf<ArchiveEntry?>(null) }
    var ctxX by remember { mutableStateOf(0) }
    var ctxY by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AHAppBar(
            title = "📦 ${archivePath.substringAfterLast('\\')}",
            onBackClick = onBack,
            onMenuClick = onMenuClick
        )

        if (currentFolder.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "/$currentFolder",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AHColors.Primary.copy(alpha = 0.12f))
                    .clickable(onClick = onExtractAll)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "⬆ استخراج الكل",
                    fontSize = 12.sp,
                    color = AHColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when {
            needsPassword -> PasswordPrompt(onSubmit = onPasswordEntered)

            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "جارٍ قراءة الأرشيف...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                val current = entries.filter { entry ->
                    val rel = entry.path.removePrefix(currentFolder).removePrefix("/")
                    if (entry.isDirectory) {
                        val r = rel.trimEnd('/')
                        r.isNotBlank() && !r.contains('/')
                    } else {
                        rel.isNotBlank() && !rel.contains('/')
                    }
                }

                if (current.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "الأرشيف فارغ",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        if (currentFolder.isNotBlank()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val parent = currentFolder.substringBeforeLast('/', "")
                                            onFolderClick(parent)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "📁", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = ".. (رجوع)",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }
                        }

                        items(current, key = { it.path }) { entry ->
                            ArchiveEntryRow(
                                entry = entry,
                                onClick = {
                                    if (entry.isDirectory) {
                                        onFolderClick(entry.path.trimEnd('/'))
                                    } else {
                                        onPreviewFile(entry)
                                    }
                                },
                                onRightClick = { x, y ->
                                    contextEntry = entry
                                    ctxX = x
                                    ctxY = y
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }

    val cEntry = contextEntry
    if (cEntry != null) {
        AHContextMenu(
            items = buildList {
                if (!cEntry.isDirectory) {
                    add(ContextMenuItem(
                        icon = "👁️",
                        label = "معاينة",
                        onClick = { onPreviewFile(cEntry) }
                    ))
                }
                add(ContextMenuItem(
                    icon = "⬆️",
                    label = "استخراج هنا",
                    onClick = { onExtractEntry(cEntry) }
                ))
                add(ContextMenuItem(
                    icon = "📁",
                    label = "استخراج إلى...",
                    onClick = { onExtractEntryTo(cEntry) }
                ))
                add(ContextMenuItem(
                    icon = "📋",
                    label = "نسخ المسار",
                    onClick = { onCopyEntryPath(cEntry) }
                ))
            },
            offsetX = ctxX,
            offsetY = ctxY,
            onDismiss = { contextEntry = null }
        )
    }
}

@Composable
private fun ArchiveEntryRow(
    entry: ArchiveEntry,
    onClick: () -> Unit,
    onRightClick: (Int, Int) -> Unit
) {
    var rowPosition by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowPosition = it.positionInRoot() }
            .pointerInput(entry.path) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press &&
                            event.buttons.isSecondaryPressed
                        ) {
                            val change = event.changes.first()
                            val abs = rowPosition + change.position
                            onRightClick(abs.x.toInt(), abs.y.toInt())
                            change.consume()
                        }
                    }
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (entry.isDirectory) AHColors.CategoryFolder.copy(alpha = 0.15f)
                    else AHColors.CategoryDocuments.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (entry.isDirectory) "📁" else iconForArchive(entry.name),
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = entry.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (entry.isDirectory) "مجلد"
                       else "${formatBytes(entry.size)} • ${formatBytes(entry.compressedSize)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PasswordPrompt(onSubmit: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "🔒", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "الأرشيف محمي بكلمة سر",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it },
                label = { Text("كلمة السر") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AHColors.Primary)
                    .clickable { onSubmit(pwd) }
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(text = "فتح", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

private fun iconForArchive(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp" -> "🖼️"
        "mp4", "mkv", "avi", "mov" -> "🎬"
        "mp3", "wav", "ogg" -> "🎵"
        "pdf" -> "📕"
        "txt", "md" -> "📄"
        else -> "📄"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "${"%.2f".format(bytes / gb)} GB"
        bytes >= mb -> "${"%.2f".format(bytes / mb)} MB"
        bytes >= kb -> "${"%.2f".format(bytes / kb)} KB"
        else -> "$bytes B"
    }
}