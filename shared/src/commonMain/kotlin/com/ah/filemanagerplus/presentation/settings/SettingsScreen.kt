package com.ah.filemanagerplus.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.core.utils.createDesktopShortcut
import com.ah.filemanagerplus.core.utils.deleteDesktopShortcut
import com.ah.filemanagerplus.core.utils.desktopShortcutExists
import com.ah.filemanagerplus.data.AppSettings
import com.ah.filemanagerplus.data.SizeUnitMode
import com.ah.filemanagerplus.data.ThemeMode
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.theme.AHColors

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onResetOnboarding: () -> Unit,
    onMenuClick: () -> Unit
) {
    var shortcutExists by remember { mutableStateOf(desktopShortcutExists()) }
    var lastMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AHAppBar(
            title = "الإعدادات",
            onBackClick = onBack,
            onMenuClick = onMenuClick
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("المظهر")

            SettingRow(
                title = "الوضع",
                subtitle = when (settings.themeMode) {
                    ThemeMode.LIGHT -> "فاتح"
                    ThemeMode.DARK -> "داكن"
                    ThemeMode.SYSTEM -> "حسب النظام"
                },
                onClick = {
                    val next = when (settings.themeMode) {
                        ThemeMode.LIGHT -> ThemeMode.DARK
                        ThemeMode.DARK -> ThemeMode.SYSTEM
                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                    }
                    onSettingsChange(settings.copy(themeMode = next))
                }
            )

            SectionHeader("القياسات")

            SettingRow(
                title = "وحدة الحجم",
                subtitle = when (settings.sizeUnit) {
                    SizeUnitMode.AUTO -> "تلقائي (KB/MB/GB)"
                    SizeUnitMode.MB -> "ميغابايت فقط"
                    SizeUnitMode.GB -> "غيغابايت فقط"
                },
                onClick = {
                    val next = when (settings.sizeUnit) {
                        SizeUnitMode.AUTO -> SizeUnitMode.MB
                        SizeUnitMode.MB -> SizeUnitMode.GB
                        SizeUnitMode.GB -> SizeUnitMode.AUTO
                    }
                    onSettingsChange(settings.copy(sizeUnit = next))
                }
            )

            SectionHeader("سطح المكتب")

            SettingRow(
                title = if (shortcutExists)
                    "اختصار موجود على سطح المكتب"
                else
                    "إنشاء اختصار على سطح المكتب",
                subtitle = if (shortcutExists)
                    "اضغط لحذف الاختصار"
                else
                    "افتح التطبيق بنقرة واحدة",
                onClick = {
                    if (shortcutExists) {
                        val ok = deleteDesktopShortcut()
                        if (ok) {
                            shortcutExists = false
                            lastMessage = "✅ تم حذف الاختصار"
                        } else {
                            lastMessage = "❌ فشل حذف الاختصار"
                        }
                    } else {
                        val ok = createDesktopShortcut()
                        if (ok) {
                            shortcutExists = true
                            lastMessage = "✅ تم إنشاء الاختصار"
                        } else {
                            lastMessage = "❌ فشل إنشاء الاختصار"
                        }
                    }
                }
            )

            lastMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = if (msg.startsWith("✅")) AHColors.Primary
                                else MaterialTheme.colorScheme.error
                    )
                }
            }

            SectionHeader("السلوك")

            ToggleRow(
                title = "إظهار الملفات المخفية افتراضياً",
                checked = settings.showHiddenByDefault,
                onChange = { onSettingsChange(settings.copy(showHiddenByDefault = it)) }
            )

            ToggleRow(
                title = "استخدام سلة المحذوفات افتراضياً",
                subtitle = "عند الحذف، اذهب لسلة المحذوفات بدل الحذف الدائم",
                checked = settings.useTrashByDefault,
                onChange = { onSettingsChange(settings.copy(useTrashByDefault = it)) }
            )

            ToggleRow(
                title = "انتقالات متحركة",
                checked = settings.animateTransitions,
                onChange = { onSettingsChange(settings.copy(animateTransitions = it)) }
            )

            SectionHeader("معلومات")

            SettingRow(
                title = "إعادة عرض الترحيب",
                subtitle = "شاهد صفحات التعريف مرة أخرى",
                onClick = onResetOnboarding
            )

            SettingRow(
                title = "حول البرنامج",
                subtitle = "الفريق، الإصدار، التفاصيل",
                onClick = onOpenAbout
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = AHColors.Primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AHColors.Primary)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}