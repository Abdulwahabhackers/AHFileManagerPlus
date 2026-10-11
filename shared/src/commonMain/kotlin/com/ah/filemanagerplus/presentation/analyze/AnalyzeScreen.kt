package com.ah.filemanagerplus.presentation.analyze

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.AnalysisResult
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.theme.AHColors
import java.util.Locale

@Composable
fun AnalyzeScreen(
    drives: List<Drive>,
    selectedDriveIndex: Int,
    result: AnalysisResult?,
    isAnalyzing: Boolean,
    onDriveTabClick: (Int) -> Unit,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onFileClick: (FileItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AHAppBar(
            title = "تحليل التخزين",
            onBackClick = onBack,
            onMenuClick = onMenuClick
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (drives.size > 1) {
            DrivesTabs(
                drives = drives,
                selectedIndex = selectedDriveIndex,
                onTabClick = onDriveTabClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }

        if (result == null) {
            AnalyzingView()
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(result = result)
            }

            if (result.largestFolders.isNotEmpty()) {
                item {
                    SectionTitle(
                        if (result.isComplete) "أكبر المجلدات"
                        else "أكبر المجلدات (جارٍ المسح...)"
                    )
                }
                val foldersTotal = result.largestFolders.sumOf { it.size }.coerceAtLeast(1L)
                items(
                    count = result.largestFolders.size,
                    key = { i -> "f_${result.largestFolders[i].path}" }
                ) { i ->
                    val folder = result.largestFolders[i]
                    UnifiedAnalysisRow(
                        name = folder.name,
                        path = folder.path,
                        isDirectory = true,
                        size = folder.size,
                        itemCount = folder.itemCount,
                        percent = folder.size.toDouble() / foldersTotal,
                        onClick = { onFileClick(folder) }
                    )
                }
            }

            if (result.largestFiles.isNotEmpty()) {
                item {
                    SectionTitle("ملفات كبيرة")
                }
                val filesTotal = result.largestFiles.sumOf { it.size }.coerceAtLeast(1L)
                items(
                    count = result.largestFiles.size,
                    key = { i -> "l_${result.largestFiles[i].path}" }
                ) { i ->
                    val file = result.largestFiles[i]
                    UnifiedAnalysisRow(
                        name = file.name,
                        path = file.path,
                        isDirectory = false,
                        size = file.size,
                        itemCount = 0,
                        percent = file.size.toDouble() / filesTotal,
                        onClick = { onFileClick(file) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DrivesTabs(
    drives: List<Drive>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        drives.forEachIndexed { index, drive ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabClick(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = drive.displayTitle,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) AHColors.Primary else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(result: AnalysisResult) {
    val usedPct = if (result.totalBytes > 0)
        (result.usedBytes * 100 / result.totalBytes).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.drive.displayTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${formatBytes(result.freeBytes)} مجاناً",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AHColors.Primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "$usedPct%",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usedPct / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AHColors.Primary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        CategoriesGrid(result = result)
    }
}

@Composable
private fun CategoriesGrid(result: AnalysisResult) {
    val cats = result.categories.filter { it.type != CategoryType.DOWNLOADS }
    Column(modifier = Modifier.fillMaxWidth()) {
        cats.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { cat ->
                    CategoryCell(
                        type = cat.type,
                        size = cat.totalSize,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CategoryCell(type: CategoryType, size: Long, modifier: Modifier = Modifier) {
    val (label, symbol) = when (type) {
        CategoryType.IMAGES -> "صور" to "🖼️"
        CategoryType.VIDEOS -> "فيديو" to "🎬"
        CategoryType.AUDIO -> "صوتي" to "🎵"
        CategoryType.DOCUMENTS -> "وثائق" to "📄"
        CategoryType.DOWNLOADS -> "تنزيلات" to "⬇️"
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = symbol, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = formatBytes(size),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UnifiedAnalysisRow(
    name: String,
    path: String,
    isDirectory: Boolean,
    size: Long,
    itemCount: Int,
    percent: Double,
    onClick: () -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDirectory) "📁" else iconFor(name),
                    fontSize = 24.sp
                )
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
                    // النص فقط — أبيض مع ظل
                    Text(
                        text = name,
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
                        text = formatBytes(size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (isDirectory && itemCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عناصر $itemCount",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun AnalyzingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📊", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "جارٍ تحليل التخزين...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
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