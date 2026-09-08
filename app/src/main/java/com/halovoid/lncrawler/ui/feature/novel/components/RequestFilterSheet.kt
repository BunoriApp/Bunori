package com.halovoid.lncrawler.ui.feature.novel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.core.components.AppBottomSheet
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetDivider
import com.halovoid.lncrawler.ui.core.theme.DarkSurface
import com.halovoid.lncrawler.ui.core.theme.PrimaryText
import com.halovoid.lncrawler.ui.core.theme.SecondaryText
import com.halovoid.lncrawler.ui.feature.novel.FilterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFilterSheet(
    allRequests: List<Request>,
    statusFilters: Map<RequestStatus, FilterState>,
    onStatusFilterChange: (RequestStatus, FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val availableStatuses = remember(allRequests) {
        allRequests.map { it.status }.distinct().sortedBy { it.name }
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Filter Status"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            availableStatuses.forEachIndexed { index, rstatus ->
                val currentState = statusFilters[rstatus] ?: FilterState.NONE
                ListItem(
                    headlineContent = { Text(rstatus.name.lowercase().replaceFirstChar { it.uppercase() }, color = PrimaryText) },
                    leadingContent = {
                        ThreeStateCheckbox(state = currentState)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onStatusFilterChange(rstatus, currentState.next())
                    }
                )
                if (index < availableStatuses.lastIndex) {
                    AppBottomSheetDivider()
                }
            }
        }
    }
}

@Composable
fun ThreeStateCheckbox(state: FilterState) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            FilterState.NONE -> {
                Icon(
                    imageVector = Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
            FilterState.INCLUDE -> {
                Icon(
                    imageVector = Icons.Default.CheckBox,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DarkSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
            FilterState.EXCLUDE -> {
                Icon(
                    imageVector = Icons.Default.CheckBox,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = DarkSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
