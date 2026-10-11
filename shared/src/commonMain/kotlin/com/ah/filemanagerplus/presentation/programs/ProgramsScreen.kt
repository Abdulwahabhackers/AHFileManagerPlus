package com.ah.filemanagerplus.presentation.programs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.InstalledProgram
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.components.AHContextMenu
import com.ah.filemanagerplus.presentation.components.ContextMenuItem
import com.ah.filemanagerplus.presentation.theme.AHColors

private val PROGRAM_ICON_LOADER by lazy { createProgramIconLoader() }

@Composable
fun ProgramsScreen(
    programs: List<InstalledProgram>,
    isScanning: Boolean,
    backupStatus: String?,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onProgramClick: (InstalledProgram) -> Unit,
    onOpenFolder: (InstalledProgram) -> Unit,
    onUninstall: (InstalledProgram) -> Unit,
    onShare: (InstalledProgram) -> Unit,
    onCopyInfo: (InstalledProgram) -> Unit,
    onBackup: (InstalledProgram) -> Unit,
    onDetails: (InstalledProgram) -> Unit
) {
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var contextTarget by remember { mutableStateOf<InstalledProgram?>(null) }
    var contextX by remember { mutableStateOf(0) }
    var contextY by remember { mutableStateOf(0) }

    val filtered = remember(programs, searchQuery) {
        if (searchQuery.isBlank()) programs
        else programs.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.publisher.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AHAppBar(
            title = if (isScanning) "البرامج — جارٍ المسح..."
                    else "البرامج (${filtered.size})",
            onBackClick = {
                if (searchOpen) { searchOpen = false; searchQuery = "" }
                else onBack()
            },
            onMenuClick = onMenuClick,
            onSearchClick = {
                searchOpen = !searchOpen
                if (!searchOpen) searchQuery = ""
            }
        )

        if (backupStatus != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AHColors.Primary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = backupStatus,
                    fontSize = 12.sp,
                    color = AHColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (searchOpen) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClose = { searchOpen = false; searchQuery = "" }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (isScanning && programs.isEmpty()) {
            ScanningView()
            return@Column
        }
        if (filtered.isEmpty()) {
            EmptyView(searchQuery)
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filtered, key = { "${it.name}|${it.version}|${it.installLocation}" }) { program ->
                ProgramRow(
                    program = program,
                    onClick = { onProgramClick(program) },
                    onRightClick = { x, y ->
                        contextTarget = program
                        contextX = x
                        contextY = y
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                ContextMenuItem(
                    icon = "📂",
                    label = "فتح مجلد التثبيت",
                    enabled = target.installLocation.isNotBlank(),
                    onClick = { onOpenFolder(target) }
                ),
                ContextMenuItem(
                    icon = "ℹ️",
                    label = "تفاصيل البرنامج",
                    onClick = { onDetails(target) }
                ),
                ContextMenuItem(
                    icon = "💾",
                    label = "نسخ احتياطي",
                    onClick = { onBackup(target) }
                ),
                ContextMenuItem(icon = "", label = "", isDivider = true),
                ContextMenuItem(
                    icon = "📤",
                    label = "مشاركة معلومات",
                    onClick = { onShare(target) }
                ),
                ContextMenuItem(
                    icon = "📋",
                    label = "نسخ المعلومات",
                    onClick = { onCopyInfo(target) }
                ),
                ContextMenuItem(icon = "", label = "", isDivider = true),
                ContextMenuItem(
                    icon = "🗑️",
                    label = "إلغاء التثبيت",
                    enabled = target.uninstallString.isNotBlank(),
                    onClick = { onUninstall(target) }
                )
            )
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
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
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(AHColors.Primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            ) { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            "ابحث في البرامج...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    inner()
                }
            }
        }
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProgramRow(
    program: InstalledProgram,
    onClick: () -> Unit,
    onRightClick: (Int, Int) -> Unit
) {
    var rowPosition by remember { mutableStateOf(Offset.Zero) }

    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = program.iconPath) {
        value = PROGRAM_ICON_LOADER.loadIcon(program.iconPath)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowPosition = it.positionInRoot() }
            .pointerInput(program.name) {
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
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AHColors.CategoryApps.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            val img = icon
            if (img != null) {
                Image(
                    bitmap = img,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("⚙️", fontSize = 22.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                text = program.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (program.version.isNotBlank()) append("v${program.version}")
                    if (program.publisher.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(program.publisher)
                    }
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (program.estimatedSize > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatBytes(program.estimatedSize),
                    fontSize = 10.sp,
                    color = AHColors.Primary
                )
            }
        }
    }
}

@Composable
private fun ScanningView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "⚙️", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "جارٍ مسح البرامج المثبتة...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyView(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("لا يوجد", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (query.isBlank()) "لم يتم العثور على برامج"
                else "لا توجد نتائج لـ \"$query\"",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "${dec(bytes / gb)} GB"
        bytes >= mb -> "${dec(bytes / mb)} MB"
        bytes >= kb -> "${dec(bytes / kb)} KB"
        else -> "$bytes B"
    }
}

private fun dec(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "0"
    val s = (v * 100).toLong()
    val w = s / 100
    val f = (s % 100).toInt()
    return if (f == 0) w.toString() else "$w.${f.toString().padStart(2, '0').trimEnd('0')}"
}