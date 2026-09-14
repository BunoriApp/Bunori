package com.halovoid.bunori.ui.feature.novel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.domain.models.Batch
import com.halovoid.bunori.ui.core.components.AppTopBar
import com.halovoid.bunori.ui.core.theme.DarkBackground
import com.halovoid.bunori.ui.core.theme.PrimaryText
import com.halovoid.bunori.ui.core.theme.SecondaryText
import com.halovoid.bunori.ui.feature.request.components.RequestActionHandler
import com.halovoid.bunori.ui.feature.request.components.RequestCard

@Composable
fun NovelActivityScreen(
    batches: List<Batch>,
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
    onReplay: (String) -> Unit,
    onCancel: (String) -> Unit,
    onContinue: (String) -> Unit,
    onResolveCloudflare: (String, String) -> Unit,
    cancellingRequestIds: Set<String>,
    activeActionIds: Set<String>
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Activity",
                onBack = onBack
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        RequestActionHandler(
            onResolveCloudflare = onResolveCloudflare
        ) { onSecurityClick ->
            if (batches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No activity yet", style = MaterialTheme.typography.titleMedium, color = PrimaryText)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "History of crawl batches will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(batches.sortedByDescending { it.createdAt }, key = { it.id }) { request ->
                        RequestCard(
                            batch = request,
                            onClick = { onRequestClick(request.id) },
                            onReplay = { onReplay(request.id) },
                            onCancel = { onCancel(request.id) },
                            onContinue = { onContinue(request.id) },
                            onSecurityClick = { onSecurityClick(request) },
                            isCancelling = cancellingRequestIds.contains(request.id),
                            isActionPending = activeActionIds.contains(request.id),
                            allowAction = true
                        )
                    }
                }
            }
        }
    }
}
