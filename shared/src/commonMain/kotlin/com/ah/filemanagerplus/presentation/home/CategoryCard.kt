package com.ah.filemanagerplus.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.CategoryData
import com.ah.filemanagerplus.data.model.CategoryType
import com.ah.filemanagerplus.presentation.theme.AHColors

@Composable
fun CategoryCard(
    data: CategoryData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(enabled = data.available, onClick = onClick),
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
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // أيقونة
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor(data.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = categorySymbol(data.type), fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الاسم
            Text(
                text = data.arabicName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // العدد
            Text(
                text = if (data.available) "(${data.fileCount})" else "—",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // الحجم
            if (data.available && data.totalSize > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatSize(data.totalSize),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun categoryColor(type: CategoryType): Color = when (type) {
    CategoryType.IMAGES -> AHColors.CategoryImages
    CategoryType.VIDEOS -> AHColors.CategoryVideo
    CategoryType.AUDIO -> AHColors.CategoryAudio
    CategoryType.DOCUMENTS -> AHColors.CategoryDocuments
    CategoryType.DOWNLOADS -> AHColors.Primary
}

private fun categorySymbol(type: CategoryType): String = when (type) {
    CategoryType.IMAGES -> "🖼️"
    CategoryType.VIDEOS -> "🎬"
    CategoryType.AUDIO -> "🎵"
    CategoryType.DOCUMENTS -> "📄"
    CategoryType.DOWNLOADS -> "⬇️"
}

private fun formatSize(bytes: Long): String {
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