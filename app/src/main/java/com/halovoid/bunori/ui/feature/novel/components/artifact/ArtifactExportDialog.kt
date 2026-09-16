package com.halovoid.bunori.ui.feature.novel.components.artifact

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.domain.models.Chapter
import com.halovoid.bunori.ui.core.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtifactExportDialog(
    availableSources: List<String> = emptyList(),
    initialSelectedSources: Set<String> = emptySet(),
    allChapters: List<Chapter> = emptyList(),
    crawlerName: String = "",
    onDismiss: () -> Unit,
    onExport: (ExportFormat, Set<String>) -> Unit,
) {
    var selectedFormat by remember {
        mutableStateOf(ExportFormat.EPUB)
    }

    var selectedSources by remember(availableSources, initialSelectedSources) {
        mutableStateOf(
            if (initialSelectedSources.isNotEmpty()) initialSelectedSources
            else if (availableSources.isNotEmpty()) availableSources.toSet()
            else emptySet()
        )
    }

    val matchingChapters = remember(allChapters, selectedSources, crawlerName, availableSources) {
        if (availableSources.isEmpty()) {
            allChapters
        } else if (selectedSources.isEmpty()) {
            emptyList()
        } else {
            allChapters.filter { chapter ->
                val eff = if (chapter.scanlationSource.isBlank() || chapter.scanlationSource == "NotProvided" || chapter.scanlationSource == "Not Provided") {
                    crawlerName
                } else {
                    chapter.scanlationSource
                }
                selectedSources.contains(chapter.scanlationSource) || selectedSources.contains(eff)
            }
        }
    }

    val totalCount = matchingChapters.size
    val downloadedCount = remember(matchingChapters) {
        matchingChapters.count { it.isDownloaded }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Format Selection
                Column {
                    Text(
                        text = "Format",
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExportFormat.entries.forEach { format ->
                        ExportFormatItem(
                            format = format,
                            selected = selectedFormat == format,
                            onClick = {
                                selectedFormat = format
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Section 2: Source Toggle
                if (availableSources.isNotEmpty()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sources to Export",
                                color = PrimaryText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (availableSources.size > 1) {
                                TextButton(
                                    onClick = {
                                        selectedSources = if (selectedSources.size == availableSources.size) {
                                            emptySet()
                                        } else {
                                            availableSources.toSet()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = if (selectedSources.size == availableSources.size) "Deselect All" else "Select All",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandAccent
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableSources.forEach { source ->
                                val isSelected = selectedSources.contains(source)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSources = if (isSelected) {
                                            selectedSources - source
                                        } else {
                                            selectedSources + source
                                        }
                                    },
                                    label = { Text(source) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandAccent.copy(alpha = 0.2f),
                                        selectedLabelColor = BrandAccent,
                                        selectedLeadingIconColor = BrandAccent,
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = SecondaryText
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 3: Total chapter count according to selected sources
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (downloadedCount == totalCount && totalCount > 0) SuccessGreen else BrandAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Total Chapters: $totalCount",
                                color = PrimaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (totalCount == 0) {
                                Text(
                                    text = if (selectedSources.isEmpty() && availableSources.isNotEmpty()) "No source selected" else "No chapters available",
                                    color = SecondaryText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (downloadedCount < totalCount) {
                                Text(
                                    text = "$downloadedCount downloaded (${totalCount - downloadedCount} missing)",
                                    color = WarningAmber,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "All $downloadedCount chapters downloaded",
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSources = if (availableSources.isEmpty()) emptySet() else selectedSources
                    onExport(selectedFormat, finalSources)
                },
                enabled = totalCount > 0 && (availableSources.isEmpty() || selectedSources.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent
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

// Overload for backward compatibility
@Composable
fun ArtifactExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit,
) {
    ArtifactExportDialog(
        availableSources = emptyList(),
        initialSelectedSources = emptySet(),
        allChapters = emptyList(),
        crawlerName = "",
        onDismiss = onDismiss,
        onExport = { format, _ -> onExport(format) }
    )
}
