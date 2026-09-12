package com.halovoid.bunori.ui.feature.novel.components.artifact

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.ui.core.theme.*

@Composable
fun ArtifactExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit,
) {
    var selectedFormat by remember {
        mutableStateOf(ExportFormat.EPUB)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = null,
                    tint = PrimaryAccent
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Export Artifact",
                    color = PrimaryText
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SecondaryText
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Select the format you want to export.",
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExportFormat.entries.forEach { format ->
                    ExportFormatItem(
                        format = format,
                        selected = selectedFormat == format,
                        onClick = {
                            selectedFormat = format
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(selectedFormat)
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAccent
                )
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = PrimaryText
                )
            }
        }
    )
}
