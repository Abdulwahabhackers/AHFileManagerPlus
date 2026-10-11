package com.ah.filemanagerplus.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.CategoryData
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.theme.AHColors

@Composable
fun HomeScreen(
    drives: List<Drive>,
    categories: List<CategoryData>,
    isScanning: Boolean,
    onDriveClick: (Drive) -> Unit,
    onCategoryClick: (CategoryData) -> Unit,
    onSettingsClick: () -> Unit,
    onOperationsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onSystemClick: () -> Unit,
    onProgramsClick: () -> Unit,
    onRecycleBinClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AHAppBar(
            title = "AH File Manager Plus",
            onMenuClick = onMenuClick,
            onAnalyzeClick = onAnalyzeClick,
            onSettingsClick = onSettingsClick,
            onMoreClick = onOperationsClick
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (drives.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle("التخزين")
                }
                items(drives, key = { "drive_${it.rootPath}" }) { drive ->
                    DriveCard(drive = drive, onClick = { onDriveClick(drive) })
                }
            }

            if (categories.isNotEmpty() || isScanning) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(
                        if (isScanning) "التصنيفات — جارٍ المسح..." else "التصنيفات"
                    )
                }
                items(categories, key = { "cat_${it.type.name}" }) { cat ->
                    CategoryCard(
                        data = cat,
                        onClick = { onCategoryClick(cat) }
                    )
                }
            }

            // ==================== قسم النظام والأدوات ====================
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("النظام والأدوات")
            }
            item {
                SystemCard(
                    title = "النظام",
                    icon = "🖥️",
                    subtitle = "Windows",
                    color = Color(0xFF546E7A),
                    onClick = onSystemClick
                )
            }
            item {
                SystemCard(
                    title = "البرامج",
                    icon = "⚙️",
                    subtitle = "المثبتة",
                    color = AHColors.CategoryApps,
                    onClick = onProgramsClick
                )
            }
            item {
                SystemCard(
                    title = "سلة المحذوفات",
                    icon = "🗑️",
                    subtitle = "المحذوفات",
                    color = Color(0xFF757575),
                    onClick = onRecycleBinClick
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DriveCard(
    drive: Drive,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AHColors.PrimaryLight, AHColors.Primary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💾", fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = drive.displayTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = drive.displaySubtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            DriveUsageBar(percent = drive.usedPercent)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${formatBytes(drive.freeBytes)} متاح",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DriveUsageBar(percent: Int) {
    val safePercent = percent.coerceIn(0, 100)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safePercent / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AHColors.Primary)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$safePercent%",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "مستخدم",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryCard(
    data: CategoryData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = categorySymbol(data.type),
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = categoryLabel(data.type),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (data.fileCount > 0) "${data.fileCount} ملف" else "لا ملفات",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SystemCard(
    title: String,
    icon: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun categoryLabel(type: CategoryType): String = when (type) {
    CategoryType.IMAGES -> "صور"
    CategoryType.VIDEOS -> "فيديو"
    CategoryType.AUDIO -> "صوتي"
    CategoryType.DOCUMENTS -> "وثائق"
    CategoryType.DOWNLOADS -> "تنزيلات"
}

private fun categorySymbol(type: CategoryType): String = when (type) {
    CategoryType.IMAGES -> "🖼️"
    CategoryType.VIDEOS -> "🎬"
    CategoryType.AUDIO -> "🎵"
    CategoryType.DOCUMENTS -> "📄"
    CategoryType.DOWNLOADS -> "⬇️"
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