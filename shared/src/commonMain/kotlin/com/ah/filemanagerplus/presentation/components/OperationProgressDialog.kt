package com.ah.filemanagerplus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ah.filemanagerplus.core.filesystem.OperationProgress
import com.ah.filemanagerplus.core.filesystem.OperationType
import com.ah.filemanagerplus.data.OperationJob
import com.ah.filemanagerplus.presentation.theme.AHColors

@Composable
fun OperationsDialog(
    operations: List<OperationJob>,
    onCancelOperation: (String) -> Unit,
    onHideAll: () -> Unit
) {
    Dialog(onDismissRequest = { /* لا يُغلق */ }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.width(560.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // العنوان
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (operations.size == 1) operationTitle(operations.first().type)
                               else "العمليات الجارية (${operations.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // قائمة العمليات
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    operations.forEachIndexed { index, op ->
                        OperationCard(
                            job = op,
                            onCancel = { onCancelOperation(op.id) }
                        )
                        if (index < operations.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // زر الإخفاء
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onHideAll) {
                        Text(
                            text = "إخفاء",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationCard(
    job: OperationJob,
    onCancel: () -> Unit
) {
    val p: OperationProgress = job.progress

    Column(modifier = Modifier.fillMaxWidth()) {
        // رأس البطاقة: النوع + النسبة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = operationTitle(job.type),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (job.isPreparing) "..." else "${p.percent}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AHColors.Primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // اسم الملف الحالي
        Text(
            text = if (job.isPreparing) "جارٍ إعداد..." else p.currentFile.ifBlank { "..." },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // شريط التقدم
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val frac = if (job.isPreparing) 0f else p.percent / 100f
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac.coerceIn(0f, 1f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AHColors.Primary)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // السطر 1: الحجم / السرعة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatBytes(p.bytesProcessed)} / ${formatBytes(p.totalBytes)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (p.bytesPerSecond > 0) {
                Text(
                    text = "${formatBytes(p.bytesPerSecond)}/s",
                    fontSize = 11.sp,
                    color = AHColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // السطر 2: الملفات / الوقت / إلغاء
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${p.filesProcessed} / ${p.totalFiles}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(p.elapsedMillis),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onCancel,
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "إلغاء",
                    color = AHColors.Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==================== Helpers ====================

private fun operationTitle(type: OperationType): String = when (type) {
    OperationType.COPY -> "نسخ"
    OperationType.MOVE -> "نقل"
    OperationType.DELETE -> "حذف"
    OperationType.EXTRACT -> "استخراج"
    OperationType.COMPRESS -> "ضغط"
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

private fun formatDuration(millis: Long): String {
    val secs = (millis / 1000).coerceAtLeast(0)
    val m = secs / 60
    val s = secs % 60
    return "$m:${s.toString().padStart(2, '0')}"
}