package com.halovoid.lncrawler.ui.feature.library.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.halovoid.lncrawler.ui.core.components.AppBottomSheet
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetDivider
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetGroup
import com.halovoid.lncrawler.ui.core.theme.BrandAccent
import com.halovoid.lncrawler.ui.core.theme.PrimaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterBottomSheet(
    selected: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Filter Sources"
    ) {
        AppBottomSheetGroup {
            options.forEachIndexed { index, option ->
                ListItem(
                    headlineContent = { Text(option, color = PrimaryText) },
                    trailingContent = {
                        if (selected == option) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSelected(option) }
                )
                if (index < options.lastIndex) {
                    AppBottomSheetDivider()
                }
            }
        }
    }
}
