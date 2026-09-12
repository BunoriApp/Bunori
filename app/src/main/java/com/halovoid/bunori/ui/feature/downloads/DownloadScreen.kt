package com.halovoid.bunori.ui.feature.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.data.db.entities.RequestType
import com.halovoid.bunori.ui.core.components.MutedEmptyState
import com.halovoid.bunori.ui.core.components.ScreenHeader
import com.halovoid.bunori.ui.core.theme.DarkBackground
import com.halovoid.bunori.ui.core.theme.PrimaryText
import com.halovoid.bunori.ui.feature.request.components.FilterBottomSheet
import com.halovoid.bunori.ui.feature.request.components.RequestActionHandler
import com.halovoid.bunori.ui.feature.request.components.requestHistorySection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    onRequestClick: (String) -> Unit,
    onGroupClick: (RequestType) -> Unit
) {
    val requestHistory by viewModel.requestHistory.collectAsStateWithLifecycle()
    val cancellingRequestIds by viewModel.cancellingRequestIds.collectAsStateWithLifecycle()
    val activeActionIds by viewModel.activeActionIds.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf<RequestType?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    RequestActionHandler(
        onResolveCloudflare = { id, url -> viewModel.resolveCloudflare(id, url) }
    ) { onSecurityClick ->
        Scaffold(
            containerColor = DarkBackground
        ) { innerPadding ->
            val filteredHistory = remember(requestHistory, filterType) {
                if (filterType == null) requestHistory
                else requestHistory.filter { it.type == filterType }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                ScreenHeader(
                    title = "Downloads",
                    subtitle = if (requestHistory.isNotEmpty()) "${filteredHistory.size} items" else null,
                    actions = {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = PrimaryText
                            )
                        }
                    }
                )

                if (showFilterMenu) {
                    FilterBottomSheet(
                        currentFilter = filterType,
                        onDismiss = { showFilterMenu = false },
                        onFilterSelected = { selected ->
                            filterType = selected
                            showFilterMenu = false
                        }
                    )
                }

                if (filteredHistory.isEmpty()) {
                    MutedEmptyState(
                        title = "No Downloads Yet",
                        description = "Monitor and manage all your background tasks here. From fetching metadata to downloading chapters for offline reading, every request's status can be tracked in real-time.",
                        icon = Icons.Outlined.DownloadForOffline,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        requestHistorySection(
                            requestHistory = filteredHistory,
                            onRequestClick = onRequestClick,
                            onGroupClick = onGroupClick,
                            onReplay = { viewModel.replayRequest(it) },
                            onCancel = { viewModel.cancelRequest(it) },
                            onContinue = { viewModel.resumeRequest(it) },
                            onSecurityClick = onSecurityClick,
                            cancellingRequestIds = cancellingRequestIds,
                            activeActionIds = activeActionIds,
                            allowAction = true
                        )
                    }
                }
            }
        }
    }
}
