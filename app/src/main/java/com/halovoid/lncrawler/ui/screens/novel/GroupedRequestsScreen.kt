package com.halovoid.lncrawler.ui.screens.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.components.*
import com.halovoid.lncrawler.ui.theme.BrandAccent
import com.halovoid.lncrawler.ui.theme.DarkBackground
import com.halovoid.lncrawler.ui.theme.DarkSurface
import com.halovoid.lncrawler.ui.theme.PrimaryAccent
import com.halovoid.lncrawler.ui.theme.PrimaryText
import com.halovoid.lncrawler.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupedRequestsScreen(
    type: RequestType,
    requests: List<Request>,
    allRequests: List<Request>,
    statusFilters: Map<RequestStatus, FilterState>,
    onStatusFilterChange: (RequestStatus, FilterState) -> Unit,
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
    onReplay: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    onContinue: (String) -> Unit = {},
    onResolveCloudflare: (String, String) -> Unit = { _, _ -> },
    cancellingRequestIds: Set<String> = emptySet(),
    activeActionIds: Set<String> = emptySet(),
    allowAction: Boolean = false
) {
    val filteredRequests = requests.filter { it.type == type }
    val unfilteredRequestsForType = remember(allRequests, type) {
        allRequests.filter { it.type == type }
    }
    var showFilterMenu by remember { mutableStateOf(false) }
    val isFilterActive = statusFilters.values.any { it != FilterState.NONE }

    RequestActionHandler(
        onResolveCloudflare = onResolveCloudflare
    ) { onSecurityClick ->
        val title = when (type) {
            RequestType.NOVEL_METADATA -> "Metadata"
            RequestType.CHAPTER -> "Chapter Downloads"
            RequestType.ARTIFACT -> "Exports"
            RequestType.RANGE_DOWNLOAD -> "Downloads"
            RequestType.BACKUP -> "Backups"
        }

        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold, color = PrimaryText) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (isFilterActive) BrandAccent else PrimaryText
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                requestHistorySection(
                    requestHistory = filteredRequests,
                    onRequestClick = onRequestClick,
                    onGroupClick = {},
                    onReplay = onReplay,
                    onCancel = onCancel,
                    onContinue = onContinue,
                    onSecurityClick = onSecurityClick,
                    cancellingRequestIds = cancellingRequestIds,
                    activeActionIds = activeActionIds,
                    allowAction = allowAction,
                    forceUngrouped = true
                )
            }
        }
    }

    if (showFilterMenu) {
        RequestFilterSheet(
            allRequests = unfilteredRequestsForType,
            statusFilters = statusFilters,
            onStatusFilterChange = onStatusFilterChange,
            onDismiss = { showFilterMenu = false }
        )
    }
}

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
