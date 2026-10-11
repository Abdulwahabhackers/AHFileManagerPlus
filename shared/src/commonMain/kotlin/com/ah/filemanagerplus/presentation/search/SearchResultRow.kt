package com.ah.filemanagerplus.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.theme.AHColors

@Composable
fun SearchResultRow(
    file: FileItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // الحجم/التاريخ — على أقصى اليمين
        Text(
            text = if (file.isDirectory) "" else file.readableSize(),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        // الاسم + المسار
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = file.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
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

        Spacer(modifier = Modifier.width(12.dp))

        // الأيقونة
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBackground(file)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconSymbol(file), fontSize = 20.sp)
        }
    }
}

private fun iconBackground(file: FileItem): Color {
    if (file.isDirectory) return AHColors.CategoryFolder.copy(alpha = 0.15f)
    return when (file.extension) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> AHColors.CategoryImages.copy(alpha = 0.15f)
        "mp4", "mkv", "avi", "mov", "webm" -> AHColors.CategoryVideo.copy(alpha = 0.15f)
        "mp3", "wav", "ogg", "m4a", "flac" -> AHColors.CategoryAudio.copy(alpha = 0.15f)
        "pdf", "doc", "docx", "txt", "md" -> AHColors.CategoryDocuments.copy(alpha = 0.15f)
        "exe", "msi", "apk" -> AHColors.CategoryApps.copy(alpha = 0.15f)
        "zip", "rar", "7z", "tar", "gz" -> AHColors.CategoryFolder.copy(alpha = 0.15f)
        else -> Color(0xFFF5F5F5)
    }
}

private fun iconSymbol(file: FileItem): String {
    if (file.isDirectory) return "📁"
    return when (file.extension) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "🖼️"
        "mp4", "mkv", "avi", "mov", "webm" -> "🎬"
        "mp3", "wav", "ogg", "m4a", "flac" -> "🎵"
        "pdf" -> "📕"
        "doc", "docx" -> "📘"
        "txt", "md" -> "📄"
        "zip", "rar", "7z", "tar", "gz" -> "📦"
        "exe", "msi" -> "⚙️"
        "apk" -> "🤖"
        else -> "📄"
    }
}