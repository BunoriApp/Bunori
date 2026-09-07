package com.halovoid.lncrawler.ui.feature.novel.components.artifact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.ui.core.theme.*

@Composable
fun ExportFormatItem(
    format: ExportFormat,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (selected) {
                    PrimaryAccent.copy(alpha = 0.85f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(enabled = true, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = format.extension.uppercase(),
                color = if (selected) Color.White else PrimaryAccent,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
        Icon(
            imageVector = if (selected) {
                Icons.Default.Check
            } else {
                Icons.Default.Add
            },
            contentDescription = null,
            tint = if (selected) Color.White else PrimaryText
        )
    }
}
