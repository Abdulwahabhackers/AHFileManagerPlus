package com.ah.filemanagerplus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ah.filemanagerplus.core.filesystem.ConflictAction
import com.ah.filemanagerplus.core.filesystem.ConflictDecision
import com.ah.filemanagerplus.core.filesystem.ConflictInfo
import com.ah.filemanagerplus.presentation.theme.AHColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConflictDialog(
    info: ConflictInfo,
    onResolve: (ConflictDecision) -> Unit
) {
    var applyToAll by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { /* لا يُغلق */ }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.width(560.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // العنوان
                Text(
                    text = "${info.sourceName} موجود بالفعل. هل تريد استبداله؟",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // جدول مقارنة (للملفات فقط)
                if (!info.isDirectory) {
                    ComparisonTable(info = info)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // خيار "تنطبق على الجميع"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyToAll = !applyToAll }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it },
                        colors = CheckboxDefaults.colors(checkedColor = AHColors.Primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (info.isDirectory) "تنطبق على جميع المجلدات"
                               else "تنطبق على جميع الملفات",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // الأزرار
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (info.isDirectory) {
                        // مجلد: دمج / تخطي / إلغاء
                        PrimaryButton(
                            label = "دمج",
                            onClick = { onResolve(ConflictDecision(ConflictAction.MERGE, applyToAll)) }
                        )
                        PrimaryButton(
                            label = "تخطي",
                            onClick = { onResolve(ConflictDecision(ConflictAction.SKIP, applyToAll)) }
                        )
                        SecondaryButton(
                            label = "إلغاء",
                            onClick = { onResolve(ConflictDecision(ConflictAction.CANCEL, false)) }
                        )
                    } else {
                        // ملف: استبدال / إعادة تسمية / تخطي / إلغاء
                        PrimaryButton(
                            label = "استبدال",
                            onClick = { onResolve(ConflictDecision(ConflictAction.REPLACE, applyToAll)) }
                        )
                        PrimaryButton(
                            label = "إعادة تسمية",
                            onClick = { onResolve(ConflictDecision(ConflictAction.RENAME, applyToAll)) }
                        )
                        PrimaryButton(
                            label = "تخطي",
                            onClick = { onResolve(ConflictDecision(ConflictAction.SKIP, applyToAll)) }
                        )
                        SecondaryButton(
                            label = "إلغاء",
                            onClick = { onResolve(ConflictDecision(ConflictAction.CANCEL, false)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonTable(info: ConflictInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // الهدف (يمين)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "الهدف",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatSize(info.targetSize),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDate(info.targetModified),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // فاصل
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(80.dp)
                .background(MaterialTheme.colorScheme.outline)
        )

        // المصدر (يسار)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "المصدر",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatSize(info.sourceSize),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDate(info.sourceModified),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AHColors.Primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== Helpers ====================

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
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

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return try {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(timestamp))
    } catch (e: Exception) { "" }
}