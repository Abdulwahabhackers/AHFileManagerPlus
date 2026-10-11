package com.ah.filemanagerplus.presentation.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.ah.filemanagerplus.core.filesystem.OperationType
import com.ah.filemanagerplus.data.OperationJob
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.theme.AHColors

@Composable
fun OperationsScreen(
    operations: List<OperationJob>,
    onBack: () -> Unit,
    onCancelOperation: (String) -> Unit,
    onRemoveOperation: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AHAppBar(
                title = if (operations.isEmpty()) "العمليات"
                        else "العمليات (${operations.size})",
                onBackClick = onBack
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            if (operations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد عمليات جارية",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(operations, key = { it.id }) { job ->
                        OperationCard(
                            job = job,
                            onCancel = { onCancelOperation(job.id) },
                            onRemove = { onRemoveOperation(job.id) }
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
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    val p = job.progress
    val typeLabel = operationTitle(job.type)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // السطر الأول: نوع العملية + النسبة/الحالة
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = typeLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                if (job.isFinished) {
                    Text(
                        text = if (job.errorMessage != null) "فشلت" else "تمت ✓",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (job.errorMessage != null)
                            Color(0xFFD32F2F) else AHColors.Primary
                    )
                } else {
                    Text(
                        text = if (job.isPreparing) "..." else "${p.percent}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AHColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // اسم الملف الحالي
            Text(
                text = if (job.isFinished)
                    (job.errorMessage ?: "اكتملت العملية بنجاح")
                else if (job.isPreparing) "جارٍ إعداد..."
                else p.currentFile.ifBlank { "..." },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // شريط التقدم
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val frac = when {
                    job.isFinished && job.errorMessage == null -> 1f
                    job.isPreparing -> 0f
                    else -> p.percent / 100f
                }
                val barColor = if (job.isFinished && job.errorMessage != null)
                    Color(0xFFD32F2F) else AHColors.Primary
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // السطر: الحجم / السرعة
            if (!job.isFinished) {
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

                Spacer(modifier = Modifier.height(4.dp))

                // السطر: الملفات / الوقت
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${p.filesProcessed} / ${p.totalFiles}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (p.elapsedMillis > 0) {
                        Text(
                            text = formatDuration(p.elapsedMillis),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // الأزرار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (job.isFinished) {
                    // زر إزالة من القائمة
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onRemove)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "إزالة",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // زر إلغاء واضح
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AHColors.Primary.copy(alpha = 0.12f))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✕",
                                fontSize = 14.sp,
                                color = AHColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إلغاء",
                                fontSize = 13.sp,
                                color = AHColors.Primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
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