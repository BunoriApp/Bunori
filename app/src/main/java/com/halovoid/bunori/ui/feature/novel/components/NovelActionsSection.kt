package com.halovoid.bunori.ui.feature.novel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.novel.components.artifact.ArtifactExportButton
import com.halovoid.bunori.ui.feature.novel.components.artifact.ExportFormat

@Composable
fun NovelActionsSection(
    onFetchMetadata: () -> Unit,
    onShowDownloadRange: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onFetchMetadata,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = PrimaryText
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = SecondaryText)
                Spacer(Modifier.width(8.dp))
                Text("Refresh", fontSize = 13.sp)
            }

            Button(
                onClick = onShowDownloadRange,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download", fontSize = 13.sp)
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        ArtifactExportButton(onExport = onExport)
    }
}

@Composable
fun ActionRow(icon: ImageVector, title: String, subtext: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = PrimaryText, fontWeight = FontWeight.Medium)
            Text(subtext, color = SecondaryText, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryText.copy(alpha = 0.5f))
    }
}
