package com.ah.filemanagerplus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.presentation.theme.AHColors

/**
 * شريط الحافظة (Clipboard Bar) — يظهر في كل الشاشات عندما يوجد محتوى في الحافظة.
 * على عكس شريط التحديد، هذا الشريط يبقى ثابتاً حتى يُلغى أو يُنفّذ اللصق.
 */
@Composable
fun ClipboardBottomBar(
    actionLabel: String,
    count: Int,
    canPaste: Boolean,
    onCancel: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(AHColors.AppBarLight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // إلغاء
        BottomButton(
            icon = "✕",
            label = "إلغاء",
            tint = Color.White,
            onClick = onCancel
        )

        // لصق (مع عدد العناصر ونوع العملية)
        BottomButton(
            icon = "📋",
            label = "لصق ($actionLabel $count)",
            tint = if (canPaste) AHColors.PrimaryLight else Color(0x66FFFFFF),
            onClick = { if (canPaste) onPaste() }
        )
    }
}

@Composable
private fun BottomButton(
    icon: String,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp, color = tint)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}