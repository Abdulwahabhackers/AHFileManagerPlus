package com.ah.filemanagerplus.presentation.browser

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.theme.AHColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val THUMBNAIL_LOADER: ThumbnailLoader by lazy { createThumbnailLoader() }

private val IMAGE_EXTS_LOCAL = setOf(
    "jpg", "jpeg", "png", "gif", "bmp", "webp", "ico", "tiff", "heic"
)

@Composable
fun FileGridItem(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRightClick: (Int, Int) -> Unit
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    val bgColor = if (isSelected) AHColors.Primary.copy(alpha = 0.20f) else Color.Transparent

    // جلب الصورة المصغرة (إذا ممكن)
    val thumbnailPath = remember(file.path, file.isDirectory) {
        if (file.isDirectory) {
            THUMBNAIL_LOADER.findFirstImageInFolder(file.path)
        } else if (file.extension.lowercase() in IMAGE_EXTS_LOCAL) {
            file.path
        } else null
    }

    val bitmap = produceState<ImageBitmap?>(
        initialValue = null,
        key1 = thumbnailPath
    ) {
        value = if (thumbnailPath != null) {
            withContext(Dispatchers.IO) {
                THUMBNAIL_LOADER.loadImage(thumbnailPath)
            }
        } else null
    }.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .onGloballyPositioned { itemPosition = it.positionInRoot() }
            .pointerInput(file.path) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press &&
                            event.buttons.isSecondaryPressed
                        ) {
                            val change = event.changes.first()
                            val absolute = itemPosition + change.position
                            onRightClick(absolute.x.toInt(), absolute.y.toInt())
                            change.consume()
                        }
                    }
                }
            }
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(gridIconBackground(file)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(text = gridIconSymbol(file), fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = file.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (file.isHidden)
                MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (file.isDirectory) "${file.itemCount} عنصر"
                   else file.readableSize(),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun gridIconBackground(file: FileItem): Color {
    if (file.isDirectory) return AHColors.CategoryFolder.copy(alpha = 0.15f)
    val ext = file.extension.lowercase()
    return when (ext) {
        "zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz" ->
            Color(0xFFFBC02D).copy(alpha = 0.20f)
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> AHColors.CategoryImages.copy(alpha = 0.15f)
        "mp4", "mkv", "avi", "mov", "webm" -> AHColors.CategoryVideo.copy(alpha = 0.15f)
        "mp3", "wav", "ogg", "m4a", "flac" -> AHColors.CategoryAudio.copy(alpha = 0.15f)
        "pdf", "doc", "docx", "txt", "md" -> AHColors.CategoryDocuments.copy(alpha = 0.15f)
        "exe", "msi", "apk" -> AHColors.CategoryApps.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun gridIconSymbol(file: FileItem): String {
    if (file.isDirectory) return "📁"
    val ext = file.extension.lowercase()
    return when (ext) {
        "zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz" -> "📦"
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "🖼️"
        "mp4", "mkv", "avi", "mov", "webm" -> "🎬"
        "mp3", "wav", "ogg", "m4a", "flac" -> "🎵"
        "pdf" -> "📕"
        "doc", "docx" -> "📘"
        "txt", "md" -> "📄"
        "exe", "msi" -> "⚙️"
        "apk" -> "🤖"
        else -> "📄"
    }
}