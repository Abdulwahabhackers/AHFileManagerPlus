package com.ah.filemanagerplus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.Drive
import com.ah.filemanagerplus.data.model.FavoriteItem
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.theme.AHColors

private enum class DrawerTab { FOLDERS, FAVORITES, RECENT }

@Composable
fun AppDrawer(
    drives: List<Drive>,
    favorites: List<FavoriteItem>,
    recentFiles: List<FileItem>,
    onHomeClick: () -> Unit,
    onDriveClick: (Drive) -> Unit,
    onSystemClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onProgramsClick: () -> Unit,
    onFavoriteClick: (FavoriteItem) -> Unit,
    onRemoveFavorite: (FavoriteItem) -> Unit,
    onRecentClick: (FileItem) -> Unit,
    onClearRecent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(DrawerTab.FOLDERS) }

    Column(
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DrawerTabIcon("📁", currentTab == DrawerTab.FOLDERS) { currentTab = DrawerTab.FOLDERS }
            DrawerTabIcon("⭐", currentTab == DrawerTab.FAVORITES) { currentTab = DrawerTab.FAVORITES }
            DrawerTabIcon("🕐", currentTab == DrawerTab.RECENT) { currentTab = DrawerTab.RECENT }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when (currentTab) {
            DrawerTab.FOLDERS -> FoldersTab(
                drives = drives,
                onHomeClick = onHomeClick,
                onDriveClick = onDriveClick,
                onSystemClick = onSystemClick,
                onRecycleBinClick = onRecycleBinClick,
                onProgramsClick = onProgramsClick
            )
            DrawerTab.FAVORITES -> FavoritesTab(
                favorites = favorites,
                onItemClick = onFavoriteClick,
                onRemove = onRemoveFavorite
            )
            DrawerTab.RECENT -> RecentTab(
                files = recentFiles,
                onItemClick = onRecentClick,
                onClear = onClearRecent
            )
        }
    }
}

@Composable
private fun DrawerTabIcon(icon: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) AHColors.Primary else Color.Transparent)
        )
    }
}

@Composable
private fun FoldersTab(
    drives: List<Drive>,
    onHomeClick: () -> Unit,
    onDriveClick: (Drive) -> Unit,
    onSystemClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onProgramsClick: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxHeight()) {
        item {
            DrawerItem(
                icon = "🏠",
                iconBg = Color(0xFFE53935).copy(alpha = 0.15f),
                title = "الصفحة الرئيسية",
                onClick = onHomeClick
            )
        }

        items(drives, key = { it.rootPath }) { drive ->
            DrawerItem(
                icon = "💾",
                iconBg = AHColors.Primary.copy(alpha = 0.15f),
                title = "${drive.usedPercent}%  ${drive.displayTitle}",
                showProgress = drive.usedPercent,
                onClick = { onDriveClick(drive) }
            )
        }

        item {
            DrawerItem(
                icon = "🖥️",
                iconBg = Color(0xFF546E7A).copy(alpha = 0.15f),
                title = "النظام",
                subtitle = "C:\\Windows",
                onClick = onSystemClick
            )
        }

        item {
            DrawerItem(
                icon = "⚙️",
                iconBg = AHColors.CategoryApps.copy(alpha = 0.15f),
                title = "البرامج",
                subtitle = "البرامج المثبتة على الجهاز",
                onClick = onProgramsClick
            )
        }

        item {
            DrawerItem(
                icon = "🗑️",
                iconBg = Color(0xFF757575).copy(alpha = 0.15f),
                title = "سلة المحذوفات",
                onClick = onRecycleBinClick
            )
        }
    }
}

@Composable
private fun FavoritesTab(
    favorites: List<FavoriteItem>,
    onItemClick: (FavoriteItem) -> Unit,
    onRemove: (FavoriteItem) -> Unit
) {
    if (favorites.isEmpty()) {
        EmptyTab("لا توجد عناصر في المفضلة", "⭐")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxHeight()) {
        items(favorites, key = { it.path }) { fav ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(fav) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AHColors.CategoryFolder.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (fav.isDirectory) "📁" else "📄", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fav.name,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = fav.path,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onRemove(fav) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", fontSize = 14.sp, color = Color(0xFFD32F2F))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun RecentTab(
    files: List<FileItem>,
    onItemClick: (FileItem) -> Unit,
    onClear: () -> Unit
) {
    if (files.isEmpty()) {
        EmptyTab("لا توجد ملفات حديثة", "🕐")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxHeight()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFD32F2F).copy(alpha = 0.10f))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "مسح الكل",
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        items(files, key = { it.path }) { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(file) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBgForRecent(file)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconForRecent(file), fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = file.path,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun DrawerItem(
    icon: String,
    iconBg: Color,
    title: String,
    subtitle: String? = null,
    showProgress: Int? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (showProgress != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(showProgress.coerceIn(0, 100) / 100f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AHColors.Primary)
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

@Composable
private fun EmptyTab(message: String, icon: String) {
    Box(
        modifier = Modifier.fillMaxHeight().fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun iconForRecent(file: FileItem): String {
    if (file.isDirectory) return "📁"
    return when (file.extension.lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "🖼️"
        "mp4", "mkv", "avi", "mov", "webm" -> "🎬"
        "mp3", "wav", "ogg", "m4a", "flac" -> "🎵"
        "pdf" -> "📕"
        "doc", "docx" -> "📘"
        "txt", "md" -> "📄"
        "zip", "rar", "7z", "tar", "gz" -> "📦"
        "exe", "msi" -> "⚙️"
        else -> "📄"
    }
}

@Composable
private fun iconBgForRecent(file: FileItem): Color {
    if (file.isDirectory) return AHColors.CategoryFolder.copy(alpha = 0.15f)
    return when (file.extension.lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> AHColors.CategoryImages.copy(alpha = 0.15f)
        "mp4", "mkv", "avi", "mov", "webm" -> AHColors.CategoryVideo.copy(alpha = 0.15f)
        "mp3", "wav", "ogg", "m4a", "flac" -> AHColors.CategoryAudio.copy(alpha = 0.15f)
        "pdf", "doc", "docx", "txt", "md" -> AHColors.CategoryDocuments.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}