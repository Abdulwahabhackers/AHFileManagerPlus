package com.ah.filemanagerplus.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.presentation.theme.AHColors

private const val TOTAL_PAGES = 6

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // زر التخطي
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentPage < TOTAL_PAGES - 1) {
                    TextButton(onClick = onFinish) {
                        Text(
                            "تخطي",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // محتوى الصفحة
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(tween(300)) { it } + fadeIn())
                                .togetherWith(slideOutHorizontally(tween(300)) { -it } + fadeOut())
                        } else {
                            (slideInHorizontally(tween(300)) { -it } + fadeIn())
                                .togetherWith(slideOutHorizontally(tween(300)) { it } + fadeOut())
                        }
                    },
                    label = "onboarding"
                ) { page ->
                    when (page) {
                        0 -> PageWelcome()
                        1 -> PageStory()
                        2 -> PageFeatures()
                        3 -> PageUsage()
                        4 -> PageTeam()
                        5 -> PageFinish()
                    }
                }
            }

            // المؤشرات
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(TOTAL_PAGES) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPage) AHColors.Primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // الأزرار
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("← السابق", fontSize = 14.sp)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (currentPage < TOTAL_PAGES - 1) currentPage++
                        else onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AHColors.Primary),
                    modifier = Modifier.weight(1.5f).height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        if (currentPage < TOTAL_PAGES - 1) "التالي" else "🚀 ابدأ الآن",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==================== Page 1: Welcome ====================

@Composable
private fun PageWelcome() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AHColors.PrimaryLight, AHColors.Primary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("AH", fontSize = 72.sp, fontWeight = FontWeight.Black, color = Color.White)
        }

        Spacer(Modifier.height(36.dp))

        Text(
            "مرحباً بك في",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "AH File Manager Plus",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = AHColors.Primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "مدير الملفات + للأندرويد\nالآن بين يديك على الكمبيوتر",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ==================== Page 2: Story ====================

@Composable
private fun PageStory() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AHColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💡", fontSize = 40.sp)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "لماذا AH File Manager Plus؟",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = AHColors.Primary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "💭 القصة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "بدأت الفكرة عندما لاحظنا أن مديري الملفات على الكمبيوتر إما:\n" +
                    "• معقدة وصعبة الاستخدام\n" +
                    "• أو بسيطة جداً وتفتقر للميزات\n" +
                    "• أو لا تدعم العربية بشكل صحيح\n" +
                    "• أو بطيئة وغير مستقرة",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = AHColors.Primary.copy(alpha = 0.10f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "🎯 الهدف",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AHColors.Primary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "صنعنا تطبيقاً يجمع بين:\n" +
                    "✓ البساطة والاحترافية\n" +
                    "✓ دعم كامل للعربية (RTL)\n" +
                    "✓ ميزات متقدمة في إدارة الملفات\n" +
                    "✓ أداء فائق وسرعة استجابة\n" +
                    "✓ تصميم مستوحى من أفضل التطبيقات العالمية",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ==================== Page 3: Features ====================

@Composable
private fun PageFeatures() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            "✨ الميزات",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = AHColors.Primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "كل ما تحتاجه في مكان واحد",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        FeatureCard("🗂️", "تصفح احترافي", "قائمة + شبكة، Breadcrumb، أيقونات ذكية")
        FeatureCard("📊", "تحليل التخزين", "أكبر الملفات والمجلدات مع أشرطة ملوّنة")
        FeatureCard("🔍", "بحث شامل", "في الأسماء + داخل المجلدات + متدفق")
        FeatureCard("⭐", "تصنيفات ذكية", "صور، فيديو، صوت، وثائق، تنزيلات")
        FeatureCard("📦", "ضغط وفك ضغط", "ZIP, 7z, TAR, GZ مع AES-256")
        FeatureCard("🖼️", "عارضات مدمجة", "صور، فيديو، نصوص، موسيقى")
        FeatureCard("🎵", "مشغل موسيقى", "Mini Player + Full Player + سرعة")
        FeatureCard("🎬", "مشغل فيديو", "بأزرار تحكم كاملة + إخفاء تلقائي")
        FeatureCard("🖥️", "تصميم أنيق", "وضع فاتح/داكن + RTL كامل")
    }
}

@Composable
private fun FeatureCard(symbol: String, title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AHColors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== Page 4: Usage ====================

@Composable
private fun PageUsage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            "📖 كيف تستخدمه؟",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = AHColors.Primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "خطوات بسيطة للبدء",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        StepCard("1", "تصفح ملفاتك", "اختر قرصاً من الرئيسية، أو تصنيفاً (صور/فيديو). انقر على أي مجلد للدخول، أو ملف لفتحه.")
        StepCard("2", "استخدم قائمة السياق", "Right-click على أي ملف أو مجلد → قائمة عمليات كاملة (نسخ، نقل، حذف، إعادة تسمية، ضغط...).")
        StepCard("3", "حلّل التخزين", "اضغط 📊 في الشريط العلوي → شاشة تحليل بأشرطة ملوّنة تُظهر أكبر الملفات والمجلدات.")
        StepCard("4", "ضغط وفك ضغط", "Right-click على مجلد → 'ضغط إلى ZIP'. للفك: انقر على أي ZIP → معاينة داخله.")
        StepCard("5", "استخدم Drawer", "اضغط ☰ لفتح النافذة الجانبية → الأقراص + المفضلة + الملفات الحديثة.")

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepCard(number: String, title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AHColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    number,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ==================== Page 5: Team ====================

@Composable
private fun PageTeam() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AHColors.PrimaryLight, AHColors.Primary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("AH", fontSize = 60.sp, fontWeight = FontWeight.Black, color = Color.White)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "من تطوير",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "HACKERS AH",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = AHColors.Primary
        )

        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🏆 فريق متخصص",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "نطوّر أدوات وتطبيقات برمجية احترافية\n" +
                    "هدفنا تقديم حلول بسيطة وقوية\n" +
                    "تناسب المستخدم العربي والعالمي",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = AHColors.Primary.copy(alpha = 0.10f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "💪 الجهد المبذول",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AHColors.Primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "• مئات الساعات من التطوير والاختبار\n" +
                    "• تصميم مستوحى من تطبيقات عالمية\n" +
                    "• تحسين مستمر للأداء والاستقرار\n" +
                    "• دعم كامل للغة العربية",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

// ==================== Page 6: Finish ====================

@Composable
private fun PageFinish() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(AHColors.Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎉", fontSize = 80.sp)
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "جاهز للبدء!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = AHColors.Primary
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "كل شيء مُعد لك\nابدأ بإدارة ملفاتك باحترافية",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(28.dp))

        Text(
            "صُنع بـ ❤ بواسطة HACKERS AH",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}