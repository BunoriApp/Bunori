package com.halovoid.bunori.ui.feature.downloads.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.ui.core.theme.BrandAccent
import com.halovoid.bunori.ui.core.theme.PrimaryAccent
import com.halovoid.bunori.ui.core.theme.SecondaryText

@Composable
fun DownloadRangeDialog(
    initialRange: ClosedFloatingPointRange<Float>,
    minChapterIndex: Float = 1f,
    maxChapterIndex: Float,
    sources: List<String> = emptyList(),
    onConfirm: (ClosedFloatingPointRange<Float>) -> Unit,
    onDismiss: () -> Unit
) {
    var currentRange by remember { mutableStateOf(initialRange) }
    val rangeSpan = (maxChapterIndex - minChapterIndex).toInt()
    val steps = if (rangeSpan > 1) rangeSpan - 1 else 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Download Range") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    "Select the chapter range to download:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                RangeSlider(
                    value = currentRange,
                    onValueChange = { currentRange = it },
                    valueRange = minChapterIndex..maxChapterIndex,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAccent,
                        activeTrackColor = PrimaryAccent
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Start: ${currentRange.start.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Text(
                        "End: ${currentRange.endInclusive.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }

                if (sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Sources info",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Downloading from ${sources.size} ${if (sources.size == 1) "source" else "sources"}:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                                Text(
                                    text = sources.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentRange) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
