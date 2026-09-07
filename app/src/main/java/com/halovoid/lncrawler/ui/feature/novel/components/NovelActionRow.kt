package com.halovoid.lncrawler.ui.feature.novel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.ui.core.theme.*

@Composable
fun NovelActionRow(
    activityExists: Boolean,
    isActivityRunning: Boolean,
    artifactsExist: Boolean,
    downloadEnabled: Boolean = true,
    onActivityClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onArtifactsClick: () -> Unit,
    onWebViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionItem(
            icon = Icons.Default.History,
            label = "Activity",
            onClick = onActivityClick,
            enabled = activityExists,
            isRunning = isActivityRunning
        )
        ActionItem(
            icon = Icons.Default.Download,
            label = "Download",
            onClick = onDownloadClick,
            enabled = downloadEnabled
        )
        ActionItem(
            icon = Icons.Default.Inventory2,
            label = "Artifacts",
            onClick = onArtifactsClick,
            enabled = artifactsExist
        )
        ActionItem(
            icon = Icons.Default.Language,
            label = "WebView",
            onClick = onWebViewClick
        )
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isRunning: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) PrimaryText else SecondaryText.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
            
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    strokeWidth = 2.dp,
                    color = BrandAccent
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) PrimaryText else SecondaryText.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
