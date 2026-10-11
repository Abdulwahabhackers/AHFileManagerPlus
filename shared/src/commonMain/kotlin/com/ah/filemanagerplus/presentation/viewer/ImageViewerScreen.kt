package com.ah.filemanagerplus.presentation.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.browser.ThumbnailLoader
import com.ah.filemanagerplus.presentation.browser.createThumbnailLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO

private val VIEWER_LOADER: ThumbnailLoader by lazy { createThumbnailLoader() }

@Composable
fun ImageViewerScreen(
    images: List<FileItem>,
    currentIndex: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val current = images.getOrNull(currentIndex) ?: return

    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = current.path) {
        value = withContext(Dispatchers.IO) {
            loadImageFullSize(current.path)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // ==================== الصورة ====================
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = current.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 80.dp, vertical = 80.dp),
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

        // ==================== الشريط العلوي ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0x88000000))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", color = Color.White, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${currentIndex + 1} من ${images.size}  •  ${current.readableSize()}",
                    color = Color(0xAAFFFFFF),
                    fontSize = 11.sp
                )
            }
        }

        // ==================== أزرار التنقل ====================
        if (images.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
                    .clickable(onClick = onPrev),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "‹", color = Color.White, fontSize = 36.sp)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "›", color = Color.White, fontSize = 36.sp)
            }
        }

        // ==================== الشريط السفلي ====================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0x88000000))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = current.path,
                color = Color(0x99FFFFFF),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * يحمّل الصورة بحجمها الكامل من القرص (بحد أقصى 20 MB).
 */
private suspend fun loadImageFullSize(path: String): ImageBitmap? =
    withContext(Dispatchers.IO) {
        try {
            val f = File(path)
            if (!f.exists() || !f.isFile) return@withContext null
            if (f.length() > 20L * 1024 * 1024) return@withContext null
            ImageIO.read(f)?.toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }