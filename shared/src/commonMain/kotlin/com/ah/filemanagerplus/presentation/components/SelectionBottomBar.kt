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

data class BottomBarAction(
    val icon: String,
    val label: String,
    val tint: Color = Color.White,
    val onClick: () -> Unit
)

@Composable
fun SelectionBottomBar(
    actions: List<BottomBarAction>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF1A1A1A)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        actions.forEach { action ->
            BottomBarButton(action)
        }
    }
}

@Composable
private fun BottomBarButton(action: BottomBarAction) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = action.onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = action.icon, fontSize = 20.sp, color = action.tint)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = action.label,
            fontSize = 11.sp,
            color = action.tint.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium
        )
    }
}