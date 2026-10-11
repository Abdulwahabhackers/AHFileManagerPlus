package com.ah.filemanagerplus.presentation.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ah.filemanagerplus.data.model.InstalledProgram
import com.ah.filemanagerplus.presentation.theme.AHColors

@Composable
fun ProgramDetailsDialog(
    program: InstalledProgram,
    onDismiss: () -> Unit,
    onBackup: () -> Unit,
    onOpenFolder: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.width(560.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = program.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoRow("الإصدار", program.version.ifBlank { "—" })
                        InfoRow("الناشر", program.publisher.ifBlank { "—" })
                        InfoRow("تاريخ التثبيت", formatDate(program.installDate))
                        InfoRow("الحجم", formatBytes(program.estimatedSize))
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                InfoBlock("مسار التثبيت", program.installLocation.ifBlank { "—" })
                Spacer(Modifier.height(10.dp))
                InfoBlock("أمر إلغاء التثبيت", program.uninstallString.ifBlank { "—" })
                Spacer(Modifier.height(10.dp))
                InfoBlock("مسار الأيقونة", program.iconPath ?: "—")

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "إغلاق",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    TextButton(onClick = onOpenFolder) {
                        Text(
                            "فتح المجلد",
                            color = AHColors.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    TextButton(onClick = onBackup) {
                        Text(
                            "💾 نسخ احتياطي",
                            color = AHColors.Primary,
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}

private fun formatDate(raw: String): String {
    if (raw.length != 8) return raw.ifBlank { "—" }
    return try {
        "${raw.substring(6, 8)}/${raw.substring(4, 6)}/${raw.substring(0, 4)}"
    } catch (e: Exception) {
        raw
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
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

private fun dec(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "0"
    val s = (v * 100).toLong()
    val w = s / 100
    val f = (s % 100).toInt()
    return if (f == 0) w.toString() else "$w.${f.toString().padStart(2, '0').trimEnd('0')}"
}