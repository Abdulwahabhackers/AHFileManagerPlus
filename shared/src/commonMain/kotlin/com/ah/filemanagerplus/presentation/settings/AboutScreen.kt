package com.ah.filemanagerplus.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.presentation.components.AHAppBar
import com.ah.filemanagerplus.presentation.theme.AHColors
import java.awt.Desktop
import java.net.URI
import java.util.Properties

private const val APP_VERSION = "1.0.0"
private const val APP_BUILD = "2026.10"
private const val GITHUB_URL = "https://github.com/HACKERS-AH/AHFileManagerPlus"

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AHAppBar(
            title = "حول البرنامج",
            onBackClick = onBack,
            onMenuClick = onMenuClick
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================== HERO ====================
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4285F4), Color(0xFF0D47A1))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("AH", fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "AH File Manager Plus",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "مدير الملفات +",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AHColors.Primary.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "🏆  من إنتاج HACKERS AH",
                    color = AHColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            StorySection()

            Spacer(Modifier.height(16.dp))

            WhySection()

            Spacer(Modifier.height(16.dp))

            FeaturesSection()

            Spacer(Modifier.height(16.dp))

            UsageSection()

            Spacer(Modifier.height(16.dp))

            TeamSection()

            Spacer(Modifier.height(16.dp))

            SystemInfoSection()

            Spacer(Modifier.height(16.dp))

            GitHubButton()

            Spacer(Modifier.height(12.dp))

            CopySystemInfoButton()

            Spacer(Modifier.height(24.dp))

            Text(
                "© 2026 HACKERS AH — جميع الحقوق محفوظة",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "صُنع بـ ❤ بواسطة فريق HACKERS AH",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ==================== Sections ====================

@Composable
private fun StorySection() {
    AboutCard(
        icon = "📖",
        title = "القصة",
        titleColor = AHColors.Primary
    ) {
        Text(
            "بدأت الفكرة من ملاحظة بسيطة: مديري الملفات على الكمبيوتر إما معقدة جداً أو بسيطة جداً. لا يوجد توازن حقيقي.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "أردنا صنع تطبيق:\n" +
            "• بسيط في الشكل — احترافي في القوة\n" +
            "• يدعم العربية بالكامل\n" +
            "• سريع ومستقر\n" +
            "• يجمع أفضل ميزات التطبيقات العالمية",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun WhySection() {
    AboutCard(
        icon = "🎯",
        title = "لماذا هذا التطبيق؟",
        titleColor = AHColors.Primary
    ) {
        WhyRow("👎", "معظم المديرين إما معقدون أو محدودون")
        WhyRow("👎", "دعم العربية في كثير منها ضعيف")
        WhyRow("👎", "تفقد ملفاتك بين برامج كثيرة")
        WhyRow("✅", "حل شامل: تصفح + تحليل + عارضات + ضغط")
        WhyRow("✅", "واجهة أنيقة مستوحاة من أفضل التطبيقات")
        WhyRow("✅", "بحث سريع، تصنيف ذكي، أداء سلس")
    }
}

@Composable
private fun WhyRow(icon: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun FeaturesSection() {
    AboutCard(
        icon = "✨",
        title = "الميزات الكاملة",
        titleColor = AHColors.Primary
    ) {
        FeatureLine("🗂️", "تصفح احترافي", "قائمة، شبكة، Breadcrumb بأيقونات")
        FeatureLine("📊", "تحليل التخزين", "أكبر الملفات والمجلدات + أشرطة ملوّنة")
        FeatureLine("🔍", "بحث شامل", "في الأسماء والمحتوى، متدفق")
        FeatureLine("⭐", "تصنيفات ذكية", "صور، فيديو، صوت، وثائق، تنزيلات")
        FeatureLine("📦", "ضغط وفك", "ZIP, 7z, TAR + AES-256")
        FeatureLine("👁️", "معاينة الأرشيف", "تصفح محتوى ZIP قبل الفك")
        FeatureLine("🖼️", "عارض صور", "شاشة كاملة + تنقل سلس")
        FeatureLine("🎬", "مشغل فيديو", "تحكم كامل + إخفاء تلقائي")
        FeatureLine("🎵", "مشغل موسيقى", "Mini + Full Player + سرعة")
        FeatureLine("📄", "عارض نصوص", "أرقام أسطر + دعم كبير")
        FeatureLine("🗑️", "عمليات آمنة", "سلة محذوفات + تأكيد")
        FeatureLine("⭐", "مفضلة + حديثة", "احفظ ووصول سريع")
        FeatureLine("🌓", "وضع داكن/فاتح", "تلقائي مع النظام")
        FeatureLine("🎨", "RTL كامل", "تجربة عربية أصلية")
    }
}

@Composable
private fun FeatureLine(icon: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AHColors.Primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsageSection() {
    AboutCard(
        icon = "📘",
        title = "كيف تستخدم البرنامج",
        titleColor = AHColors.Primary
    ) {
        UsageStep("1", "افتح التطبيق → اختر قرصاً أو تصنيفاً")
        UsageStep("2", "انقر مجلداً للدخول، أو ملفاً لفتحه")
        UsageStep("3", "Right-click على أي عنصر → قائمة عمليات كاملة")
        UsageStep("4", "زر 📊 في الأعلى → تحليل التخزين")
        UsageStep("5", "زر ☰ → Drawer بالمفضلة والحديثة")
        UsageStep("6", "زر 🔍 → بحث فوري داخل المجلد")
        UsageStep("7", "Right-click على ZIP → استخراج / معاينة")
    }
}

@Composable
private fun UsageStep(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(AHColors.Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TeamSection() {
    AboutCard(
        icon = "🏆",
        title = "الفريق",
        titleColor = AHColors.Primary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AHColors.PrimaryLight, AHColors.Primary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("AH", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "HACKERS AH",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = AHColors.Primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "فريق تطوير التطبيقات والأدوات",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "💪 الجهد المبذول:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "• مئات الساعات من التطوير والاختبار\n" +
            "• تصميم مستوحى من تطبيقات عالمية\n" +
            "• تحسين مستمر للأداء والاستقرار\n" +
            "• دعم كامل للعربية (RTL)\n" +
            "• 60+ ميزة متكاملة",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SystemInfoSection() {
    AboutCard(
        icon = "⚙️",
        title = "معلومات النظام",
        titleColor = AHColors.Primary
    ) {
        InfoLine("الإصدار", APP_VERSION)
        InfoLine("البناء", APP_BUILD)
        InfoLine("نظام التشغيل", System.getProperty("os.name") ?: "?")
        InfoLine("إصدار النظام", System.getProperty("os.version") ?: "?")
        InfoLine("المعمارية", System.getProperty("os.arch") ?: "?")
        InfoLine("Java", System.getProperty("java.version") ?: "?")
        InfoLine("التقنية", "Kotlin Multiplatform")
        InfoLine("الواجهة", "Compose Desktop")
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GitHubButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                try {
                    Desktop.getDesktop().browse(URI(GITHUB_URL))
                } catch (e: Exception) {}
            }
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "🔗  رابط المشروع على GitHub",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AHColors.Primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                GITHUB_URL,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CopySystemInfoButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AHColors.Primary)
            .clickable { copySystemInfo() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "📋  نسخ معلومات النظام",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AboutCard(
    icon: String,
    title: String,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun copySystemInfo() {
    try {
        val props = Properties()
        props.setProperty("App", "AH File Manager Plus")
        props.setProperty("Version", APP_VERSION)
        props.setProperty("Build", APP_BUILD)
        props.setProperty("OS", System.getProperty("os.name") ?: "?")
        props.setProperty("OS Version", System.getProperty("os.version") ?: "?")
        props.setProperty("Arch", System.getProperty("os.arch") ?: "?")
        props.setProperty("Java", System.getProperty("java.version") ?: "?")
        props.setProperty("Java Vendor", System.getProperty("java.vendor") ?: "?")

        val sb = StringBuilder()
        sb.appendLine("=== AH File Manager Plus — معلومات النظام ===")
        props.forEach { k, v -> sb.appendLine("$k: $v") }
        sb.appendLine("التاريخ: ${java.time.LocalDateTime.now()}")

        val selection = java.awt.datatransfer.StringSelection(sb.toString())
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    } catch (e: Exception) {
        println("[About] copy error: ${e.message}")
    }
}