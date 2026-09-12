package com.halovoid.bunori.ui.feature.novel.components.artifact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.bunori.ui.core.theme.*

@Composable
fun ArtifactExportButton(
    onExport: (ExportFormat) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable { showDialog = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.GridView,
            contentDescription = null,
            tint = PrimaryAccent,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Export Artifact",
                color = PrimaryText,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Create an artifact in another format",
                color = SecondaryText,
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SecondaryText
        )
    }

    if (showDialog) {
        ArtifactExportDialog(
            onDismiss = { showDialog = false },
            onExport = {
                showDialog = false
                onExport(it)
            }
        )
    }
}

enum class ExportFormat(
    val extension: String,
    val mimeType: String
) {
    EPUB(
        extension = "epub",
        mimeType = "application/epub+zip"
    ),
    PDF(
        extension = "pdf",
        mimeType = "application/pdf"
    )
}
