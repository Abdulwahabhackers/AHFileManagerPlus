package com.ah.filemanagerplus.presentation.archive

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ah.filemanagerplus.core.archive.ArchiveFormat
import com.ah.filemanagerplus.core.archive.CompressionLevel
import com.ah.filemanagerplus.presentation.theme.AHColors

data class ArchiveConfig(
    val name: String,
    val format: ArchiveFormat,
    val level: CompressionLevel,
    val password: String?,
    val deleteAfter: Boolean
)

@Composable
fun CreateArchiveDialog(
    defaultName: String,
    onCancel: () -> Unit,
    onCreate: (ArchiveConfig) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    var format by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var level by remember { mutableStateOf(CompressionLevel.NORMAL) }
    var usePassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var deleteAfter by remember { mutableStateOf(false) }

    var showFormatMenu by remember { mutableStateOf(false) }
    var showLevelMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.width(520.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // العنوان
                Text(
                    text = "إنشاء أرشيف",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // اسم الأرشيف
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الأرشيف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // الصيغة + المستوى (صف واحد)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // الصيغة
                    DropdownField(
                        label = "تنسيق الأرشيف",
                        value = format.label,
                        modifier = Modifier.weight(1f),
                        onClick = { showFormatMenu = true }
                    )
                    // المستوى
                    DropdownField(
                        label = "مستوى الضغط",
                        value = level.label,
                        modifier = Modifier.weight(1f),
                        onClick = { showLevelMenu = true }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // خيار كلمة السر
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { usePassword = !usePassword }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = usePassword,
                        onCheckedChange = { usePassword = it },
                        colors = CheckboxDefaults.colors(checkedColor = AHColors.Primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حماية بكلمة سر (AES-256)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (usePassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة السر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // حذف الملفات الأصلية
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteAfter = !deleteAfter }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = deleteAfter,
                        onCheckedChange = { deleteAfter = it },
                        colors = CheckboxDefaults.colors(checkedColor = AHColors.Primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حذف الملفات الأصلية بعد الضغط",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // الأزرار
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = "إلغاء",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreate(
                                    ArchiveConfig(
                                        name = name,
                                        format = format,
                                        level = level,
                                        password = if (usePassword && password.isNotBlank())
                                            password else null,
                                        deleteAfter = deleteAfter
                                    )
                                )
                            }
                        }
                    ) {
                        Text(
                            text = "إنشاء",
                            color = AHColors.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // قائمة الصيغ
    if (showFormatMenu) {
        DropdownMenuDialog(
            title = "تنسيق الأرشيف",
            options = ArchiveFormat.values().map { it.label to (it == format) },
            onSelect = { idx ->
                format = ArchiveFormat.values()[idx]
                showFormatMenu = false
            },
            onDismiss = { showFormatMenu = false }
        )
    }

    // قائمة المستويات
    if (showLevelMenu) {
        DropdownMenuDialog(
            title = "مستوى الضغط",
            options = CompressionLevel.values().map { it.label to (it == level) },
            onSelect = { idx ->
                level = CompressionLevel.values()[idx]
                showLevelMenu = false
            },
            onDismiss = { showLevelMenu = false }
        )
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "▼",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DropdownMenuDialog(
    title: String,
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.width(300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                options.forEachIndexed { idx, (label, selected) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelect(idx) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(AHColors.Primary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}