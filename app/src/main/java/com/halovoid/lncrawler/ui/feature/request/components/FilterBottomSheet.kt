package com.halovoid.lncrawler.ui.feature.request.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.ui.core.components.AppBottomSheet
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetDivider
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetGroup
import com.halovoid.lncrawler.ui.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: RequestType?,
    onDismiss: () -> Unit,
    onFilterSelected: (RequestType?) -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Filter Results"
    ) {
        AppBottomSheetGroup {
            ListItem(
                headlineContent = { Text("All Downloads", color = PrimaryText) },
                trailingContent = {
                    if (currentFilter == null) Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onFilterSelected(null) }
            )
            AppBottomSheetDivider()

            RequestType.entries.forEach { type ->
                val label = when (type) {
                    RequestType.NOVEL_METADATA -> "Metadata"
                    RequestType.CHAPTER -> "Chapters"
                    RequestType.ARTIFACT -> "Exports"
                    RequestType.RANGE_DOWNLOAD -> "Downloads"
                    RequestType.BACKUP -> "Backups"
                }
                ListItem(
                    headlineContent = { Text(label, color = PrimaryText) },
                    trailingContent = {
                        if (currentFilter == type) Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onFilterSelected(type) }
                )
                if (type != RequestType.entries.last()) {
                    AppBottomSheetDivider()
                }
            }
        }
    }
}
