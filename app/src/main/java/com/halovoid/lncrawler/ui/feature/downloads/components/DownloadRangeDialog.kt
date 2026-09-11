package com.halovoid.lncrawler.ui.feature.downloads.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.ui.core.theme.BrandAccent
import com.halovoid.lncrawler.ui.core.theme.PrimaryAccent
import com.halovoid.lncrawler.ui.core.theme.SecondaryText

@Composable
fun DownloadRangeDialog(
    initialRange: ClosedFloatingPointRange<Float>,
    minChapterIndex: Float = 1f,
    maxChapterIndex: Float,
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
