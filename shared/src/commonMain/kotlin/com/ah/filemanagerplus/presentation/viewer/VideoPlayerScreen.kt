package com.ah.filemanagerplus.presentation.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.core.video.VideoPlayer
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.theme.AHColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val FRAME_INTERVAL_MS = 33L
private const val CONTROLS_HIDE_DELAY_MS = 3000L

@Composable
fun VideoPlayerScreen(
    file: FileItem?,
    videoPlayer: VideoPlayer,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    errorMessage: String?,
    playlistSize: Int,
    currentIndex: Int,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }

    // رؤية عناصر التحكم
    var controlsVisible by remember { mutableStateOf(true) }
    // عدّاد لتفادي إعادة التشغيل بالخطأ — كل ضغطة زر تزيد الرقم
    var interactionTick by remember { mutableStateOf(0L) }

    // ==================== تحميل الفيديو ====================
    LaunchedEffect(file?.path) {
        val path = file?.path
        if (path != null) {
            controlsVisible = true
            interactionTick = System.currentTimeMillis()
            val ok = withContext(Dispatchers.IO) { videoPlayer.load(path) }
            if (ok) videoPlayer.play()
        }
    }

    // ==================== التقاط الإطارات ====================
    LaunchedEffect(file?.path) {
        while (true) {
            if (videoPlayer.isPlaying() && errorMessage == null) {
                val frame = withContext(Dispatchers.IO) {
                    videoPlayer.captureFrame(0, 0)
                }
                if (frame != null) currentFrame = frame
            }
            delay(FRAME_INTERVAL_MS)
        }
    }

    // ==================== إخفاء تلقائي ====================
    LaunchedEffect(controlsVisible, isPlaying, isSeeking, interactionTick, errorMessage) {
        if (controlsVisible && isPlaying && !isSeeking && errorMessage == null) {
            delay(CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    // ==================== إظهار دائم عند التوقف ====================
    LaunchedEffect(isPlaying, errorMessage) {
        if (!isPlaying || errorMessage != null) {
            controlsVisible = true
        }
    }

    val displayValue = if (isSeeking) seekValue
                       else if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                       else 0f

    // ==================== Root ====================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(errorMessage) {
                detectTapGestures(
                    onTap = {
                        if (errorMessage == null) {
                            controlsVisible = !controlsVisible
                            if (controlsVisible) {
                                interactionTick = System.currentTimeMillis()
                            }
                        }
                    }
                )
            }
    ) {
        // ==================== صورة الفيديو ====================
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (errorMessage == null) {
                val frame = currentFrame
                if (frame != null) {
                    Image(
                        bitmap = frame,
                        contentDescription = file?.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "جارٍ التحميل...",
                        color = Color(0xAAFFFFFF),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ==================== overlay الخطأ (يظهر دائماً) ====================
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE000000))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "⚠️", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا يمكن تشغيل هذا الفيديو",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xAAFFFFFF),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(AHColors.Primary)
                            .clickable(onClick = onClose)
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "رجوع",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ملاحظة: بعض الصيغ (MKV، HEVC، AVI) غير مدعومة حالياً",
                        color = Color(0x66FFFFFF),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ==================== عناصر التحكم (تختفي تلقائياً) ====================
        AnimatedVisibility(
            visible = controlsVisible && errorMessage == null,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ============ الشريط العلوي ============
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0x88000000))
                        .padding(horizontal = 12.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                interactionTick = System.currentTimeMillis()
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "←", color = Color.White, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file?.name ?: "لا يوجد فيديو",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (playlistSize > 0) {
                            Text(
                                text = "${currentIndex + 1} من $playlistSize",
                                color = Color(0xAAFFFFFF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // ============ أزرار التنقل الجانبية ============
                if (playlistSize > 1) {
                    // السابق
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .clickable {
                                interactionTick = System.currentTimeMillis()
                                onPrev()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⏮", color = Color.White, fontSize = 26.sp)
                    }

                    // التالي
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .clickable {
                                interactionTick = System.currentTimeMillis()
                                onNext()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⏭", color = Color.White, fontSize = 26.sp)
                    }
                }

                // ============ الشريط السفلي ============
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x88000000))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Slider(
                        value = displayValue,
                        onValueChange = { fraction ->
                            isSeeking = true
                            seekValue = fraction
                            controlsVisible = true
                            interactionTick = System.currentTimeMillis()
                        },
                        onValueChangeFinished = {
                            if (durationMs > 0) onSeek((seekValue * durationMs).toLong())
                            isSeeking = false
                            interactionTick = System.currentTimeMillis()
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = AHColors.Primary,
                            activeTrackColor = AHColors.Primary,
                            inactiveTrackColor = Color(0x55FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth().height(28.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = formatTime(positionMs), color = Color(0xCCFFFFFF), fontSize = 11.sp)
                        Text(text = formatTime(durationMs), color = Color(0xCCFFFFFF), fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VideoSeekButton("↺ 10") {
                            interactionTick = System.currentTimeMillis()
                            val newPos = (positionMs - 10_000).coerceAtLeast(0)
                            onSeek(newPos)
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AHColors.Primary)
                                .clickable {
                                    interactionTick = System.currentTimeMillis()
                                    onPlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPlaying) "❚❚" else "▶",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        VideoSeekButton("10 ↻") {
                            interactionTick = System.currentTimeMillis()
                            val newPos = (positionMs + 10_000).coerceAtMost(durationMs)
                            onSeek(newPos)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoSeekButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0)
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else
        "$m:${s.toString().padStart(2, '0')}"
}