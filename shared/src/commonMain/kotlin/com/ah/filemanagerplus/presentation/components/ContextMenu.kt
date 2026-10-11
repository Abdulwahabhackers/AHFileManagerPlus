package com.ah.filemanagerplus.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup

data class ContextMenuItem(
    val icon: String,
    val label: String,
    val enabled: Boolean = true,
    val isDivider: Boolean = false,
    val onClick: () -> Unit = {}
)

@Composable
fun AHContextMenu(
    items: List<ContextMenuItem>,
    offsetX: Int,
    offsetY: Int,
    onDismiss: () -> Unit
) {
    Popup(
        offset = IntOffset(offsetX, offsetY),
        onDismissRequest = onDismiss
    ) {
        Surface(
            color = Color(0xFF2D2D2D),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .padding(vertical = 4.dp)
            ) {
                items.forEach { item ->
                    if (item.isDivider) {
                        HorizontalDivider(
                            color = Color(0x33FFFFFF),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        ContextMenuRow(item, onDismiss)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    item: ContextMenuItem,
    onDismiss: () -> Unit
) {
    val textColor = if (item.enabled) Color(0xFFE0E0E0) else Color(0x66FFFFFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled) {
                item.onClick()
                onDismiss()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = item.icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = item.label, color = textColor, fontSize = 14.sp)
    }
}