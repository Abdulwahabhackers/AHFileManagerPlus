package com.ah.filemanagerplus.presentation.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.browser.createThumbnailLoader
import com.ah.filemanagerplus.presentation.theme.AHColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MINI_LOADER by lazy { createThumbnailLoader() }

@Composable
fun MiniPlayer(
    file: FileItem,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpenFull: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0)
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    else 0f

    // تحميل الغلاف
    val cover by produceState<ImageBitmap?>(initialValue = null, key1 = file.path) {
        value = withContext(Dispatchers.IO) { MINI_LOADER.loadCoverArt(file.path) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xEE1A1A1A))
    ) {
        // شريط تقدم رقيق
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(AHColors.Primary)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // الغلاف (قابل للنقر لفتح Full)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2D2D2D))
                    .clickable(onClick = onOpenFull),
                contentAlignment = Alignment.Center
            ) {
                val c = cover
                if (c != null) {
                    Image(
                        bitmap = c,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = "🎵", fontSize = 22.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // الاسم والفنان (قابل للنقر)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenFull)
            ) {
                Text(
                    text = file.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "HACKERS AH  •  ${formatTime(positionMs)} / ${formatTime(durationMs)}",
                    color = Color(0xAAFFFFFF),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // ⏮ السابق
            MiniButton(symbol = "⏮", size = 32.dp, onClick = onPrev)

            // ▶/❚❚ تشغيل
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AHColors.Primary)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "❚❚" else "▶",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ⏭ التالي
            MiniButton(symbol = "⏭", size = 32.dp, onClick = onNext)

            // ✕ إغلاق
            MiniButton(symbol = "✕", size = 28.dp, onClick = onStop)
        }
    }
}

@Composable
private fun MiniButton(
    symbol: String,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, color = Color.White, fontSize = 14.sp)
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}