package com.ah.filemanagerplus.presentation.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.browser.createThumbnailLoader
import com.ah.filemanagerplus.presentation.theme.AHColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FULL_LOADER by lazy { createThumbnailLoader() }

@Composable
fun MusicPlayerScreen(
    track: FileItem?,
    playlistSize: Int,
    currentIndex: Int,
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    isRepeatOn: Boolean,
    positionMs: Long,
    durationMs: Long,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: (Long) -> Unit
) {
    var speed by remember { mutableStateOf(1.0f) }

    val cover by produceState<ImageBitmap?>(initialValue = null, key1 = track?.path) {
        value = if (track != null) {
            withContext(Dispatchers.IO) { FULL_LOADER.loadCoverArt(track.path) }
        } else null
    }

    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }

    val displayValue = if (isSeeking) seekValue
                       else if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                       else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F8F8),
                        Color(0xFFE8ECEF),
                        Color(0xFFDEE3E8)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================== الشريط العلوي ====================
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x22000000))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "←", color = Color(0xFF212121), fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "قيد التشغيل",
                        color = Color(0xFF757575),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (playlistSize > 0) "${currentIndex + 1} من $playlistSize" else "",
                        color = Color(0xFF212121),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== الغلاف ====================
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AHColors.Primary.copy(alpha = 0.25f),
                                Color(0xFFB0BEC5).copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val c = cover
                if (c != null) {
                    Image(
                        bitmap = c,
                        contentDescription = track?.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = "🎵", fontSize = 120.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = track?.name ?: "لا يوجد ملف",
                color = Color(0xFF212121),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "HACKERS AH",
                color = Color(0xFF757575),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== شريط التمرير ====================
            Slider(
                value = displayValue,
                onValueChange = { fraction ->
                    isSeeking = true
                    seekValue = fraction
                },
                onValueChangeFinished = {
                    if (durationMs > 0) onSeek((seekValue * durationMs).toLong())
                    isSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = AHColors.Primary,
                    activeTrackColor = AHColors.Primary,
                    inactiveTrackColor = Color(0xFFBDBDBD)
                ),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(positionMs),
                    color = Color(0xFF757575),
                    fontSize = 11.sp
                )
                Text(
                    text = formatTime(durationMs),
                    color = Color(0xFF757575),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ==================== الأزرار الرئيسية ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconToggleButton(
                    symbol = "🔀",
                    active = isShuffleOn,
                    onClick = onToggleShuffle
                )

                Spacer(modifier = Modifier.width(10.dp))

                // السابق
                BigNavButton(symbol = "⏮", onClick = onPrev)

                Spacer(modifier = Modifier.width(10.dp))

                // تشغيل/إيقاف
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AHColors.Primary)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPlaying) "❚❚" else "▶",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // التالي
                BigNavButton(symbol = "⏭", onClick = onNext)

                Spacer(modifier = Modifier.width(10.dp))

                // Repeat
                IconToggleButton(
                    symbol = "🔁",
                    active = isRepeatOn,
                    onClick = onToggleRepeat
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== -10 / +10 ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeekButton(symbol = "↺ 10", onClick = {
                    val newPos = (positionMs - 10_000).coerceAtLeast(0)
                    onSeek(newPos)
                })

                Spacer(modifier = Modifier.width(20.dp))

                SeekButton(symbol = "10 ↻", onClick = {
                    val newPos = (positionMs + 10_000).coerceAtMost(durationMs)
                    onSeek(newPos)
                })
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== السرعة ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                speeds.forEach { s ->
                    val selected = s == speed
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) AHColors.Primary else Color(0x22000000))
                            .clickable { speed = s }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (s == 1.0f) "1X" else "${s}X",
                            color = if (selected) Color.White else Color(0xFF757575),
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconToggleButton(
    symbol: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) AHColors.Primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 20.sp,
            color = if (active) AHColors.Primary else Color(0xFF757575)
        )
    }
}

@Composable
private fun BigNavButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color(0xFF212121),
            fontSize = 24.sp
        )
    }
}

@Composable
private fun SeekButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x22000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color(0xFF212121),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}