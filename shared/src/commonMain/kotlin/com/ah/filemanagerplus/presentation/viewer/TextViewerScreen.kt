package com.ah.filemanagerplus.presentation.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.data.model.FileItem
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.theme.AHColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

data class TextViewerState(
    val lines: List<String> = emptyList(),
    val isTruncated: Boolean = false,
    val errorMessage: String? = null
)

private const val MAX_LINES = 10_000
private const val MAX_FILE_SIZE = 5L * 1024 * 1024  // 5 MB

@Composable
fun TextViewerScreen(
    file: FileItem,
    onClose: () -> Unit
) {
    val state by produceState<TextViewerState>(
        initialValue = TextViewerState(),
        key1 = file.path
    ) {
        value = withContext(Dispatchers.IO) {
            loadTextFile(file.path)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AHAppBar(
            title = file.name,
            onBackClick = onClose
        )

        // شريط المعلومات
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = file.path,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = file.readableSize(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when {
            state.errorMessage != null -> ErrorView(state.errorMessage!!)
            state.lines.isEmpty() -> EmptyView()
            else -> TextContent(
                lines = state.lines,
                isTruncated = state.isTruncated
            )
        }
    }
}

@Composable
private fun TextContent(
    lines: List<String>,
    isTruncated: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isTruncated) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AHColors.Primary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "⚠ الملف كبير — يتم عرض أول $MAX_LINES سطر فقط",
                    fontSize = 11.sp,
                    color = AHColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA)),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                TextLine(lineNumber = index + 1, text = line)
            }
        }
    }
}

@Composable
private fun TextLine(lineNumber: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
    ) {
        // رقم السطر
        Box(
            modifier = Modifier
                .width(50.dp)
                .padding(end = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "$lineNumber",
                fontSize = 11.sp,
                color = Color(0xFF999999),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal
            )
        }

        // فاصل عمودي
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(Color(0xFFE0E0E0))
        )

        Spacer(modifier = Modifier.width(10.dp))

        // نص السطر
        Text(
            text = if (text.isEmpty()) " " else text,
            fontSize = 13.sp,
            color = Color(0xFF212121),
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "الملف فارغ",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "⚠", fontSize = 48.sp, color = Color(0xFFD32F2F))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== Logic ====================

private fun loadTextFile(path: String): TextViewerState {
    return try {
        val f = File(path)
        if (!f.exists()) return TextViewerState(errorMessage = "الملف غير موجود")
        if (!f.isFile) return TextViewerState(errorMessage = "ليس ملفاً")
        if (f.length() > MAX_FILE_SIZE) {
            return TextViewerState(
                errorMessage = "الملف أكبر من ${MAX_FILE_SIZE / 1024 / 1024} MB — لا يمكن عرضه"
            )
        }

        val bytes = f.readBytes()
        val charset = detectCharset(bytes)

        val allLines = String(bytes, charset).split("\r\n", "\n", "\r")
        val truncated = allLines.size > MAX_LINES
        val lines = if (truncated) allLines.take(MAX_LINES) else allLines

        TextViewerState(lines = lines, isTruncated = truncated)
    } catch (e: Exception) {
        TextViewerState(errorMessage = "فشل القراءة: ${e.message}")
    }
}

private fun detectCharset(bytes: ByteArray): Charset {
    // BOM detection (UTF-8)
    if (bytes.size >= 3 &&
        bytes[0] == 0xEF.toByte() &&
        bytes[1] == 0xBB.toByte() &&
        bytes[2] == 0xBF.toByte()
    ) {
        return Charsets.UTF_8
    }
    // افتراضي: UTF-8
    return Charsets.UTF_8
}