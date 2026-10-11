package com.ah.filemanagerplus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ah.filemanagerplus.presentation.theme.AHColors

/**
 * شريط التنقل — 🏠 > C: > مجلد > مجلد فرعي
 */
@Composable
fun Breadcrumb(
    driveLetter: String,
    segments: List<String>,
    usedPercent: Int,
    onHomeClick: () -> Unit,
    onDriveClick: () -> Unit,
    onSegmentClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🏠
            BreadcrumbItem(
                text = "🏠",
                onClick = onHomeClick,
                isFirst = true
            )

            BreadcrumbSeparator()

            // 💾 C:
            BreadcrumbItem(
                text = "💾 $driveLetter",
                onClick = onDriveClick,
                isLast = segments.isEmpty()
            )

            // باقي المستويات
            segments.forEachIndexed { index, segment ->
                BreadcrumbSeparator()
                BreadcrumbItem(
                    text = segment,
                    onClick = { onSegmentClick(index) },
                    isLast = index == segments.size - 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 90% مستخدم
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$usedPercent%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BreadcrumbItem(
    text: String,
    onClick: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isLast) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

@Composable
private fun BreadcrumbSeparator() {
    Text(
        text = "›",
        fontSize = 14.sp,
        color = AHColors.OnSurfaceVariantLight,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}