package com.halovoid.bunori.ui.feature.novel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.ui.core.components.AppBottomSheet
import com.halovoid.bunori.ui.core.components.AppBottomSheetDivider
import com.halovoid.bunori.ui.core.components.AppBottomSheetGroup
import com.halovoid.bunori.ui.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceFilterBottomSheet(
    availableSources: List<String>,
    selectedSources: Set<String>,
    onToggleSource: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Scanlator Sources"
    ) {
        AppBottomSheetGroup {
            if (availableSources.isEmpty()) {
                Text(
                    text = "No sources available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                )
            } else {
                availableSources.forEach { source ->
                    val isSelected = selectedSources.contains(source) || selectedSources.isEmpty()
                    ListItem(
                        headlineContent = {
                            Text(
                                text = source,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryText,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryText else SecondaryText
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onToggleSource(source) }
                    )
                    AppBottomSheetDivider()
                }
            }
        }
    }
}
