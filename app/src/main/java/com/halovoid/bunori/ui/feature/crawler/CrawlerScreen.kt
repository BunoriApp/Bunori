package com.halovoid.bunori.ui.feature.crawler

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.api.core.crawler.Crawler
import com.halovoid.bunori.ui.core.components.MutedEmptyState
import com.halovoid.bunori.ui.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlerScreen(
    viewModel: CrawlerViewModel,
    onBack: () -> Unit = {},
    showHeader: Boolean = true
) {
    val crawlers by viewModel.crawlers.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsStateWithLifecycle()
    val showSyncOption by viewModel.showSyncOption.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncState.Success -> {
                snackbarHostState.showSnackbar((syncState as SyncState.Success).message)
                viewModel.resetSyncState()
            }
            is SyncState.Error -> {
                snackbarHostState.showSnackbar((syncState as SyncState.Error).error)
                viewModel.resetSyncState()
            }
            else -> {}
        }
    }

    @Composable
    fun CrawlerContent(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            if (syncState is SyncState.Incompatible) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = ErrorRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "App Update Required",
                            style = MaterialTheme.typography.labelLarge,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The latest crawlers require Bunori version ${(syncState as SyncState.Incompatible).minVersion} or higher.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            }

            if (isUpdateAvailable && syncState !is SyncState.Incompatible) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = BrandAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandAccent.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Updates Available",
                                style = MaterialTheme.typography.labelLarge,
                                color = BrandAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "New sources are ready.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                        TextButton(
                            onClick = { viewModel.syncCrawlers() },
                            colors = ButtonDefaults.textButtonColors(contentColor = BrandAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (crawlers.isEmpty() && syncState !is SyncState.Loading) {
                MutedEmptyState(
                    title = "No Sources Available",
                    description = "You don't have any crawler sources installed yet. Sync with the remote repository to fetch the latest supported light novel sources.",
                    icon = Icons.Default.Extension,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(crawlers, key = { it.baseUrl }) { crawler ->
                        CrawlerItem(crawler)
                    }
                }
            }
        }
    }

    if (showHeader) {
        Scaffold(
            containerColor = DarkBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryText
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Sources",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        if (crawlers.isNotEmpty()) {
                            Text(
                                text = "${crawlers.size} sources available",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                        }
                    }

                    if (syncState is SyncState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 12.dp).size(20.dp),
                            color = BrandAccent,
                            strokeWidth = 2.dp
                        )
                    } else if (showSyncOption) {
                        IconButton(onClick = { viewModel.syncCrawlers() }) {
                            Icon(
                                Icons.Default.Sync, 
                                contentDescription = "Sync Crawlers", 
                                tint = BrandAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                CrawlerContent()
            }
        }
    } else {
        CrawlerContent()
    }
}

@Composable
fun CrawlerItem(crawler: Crawler) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = DarkSurfaceVariant,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = SecondaryText.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = crawler.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val concurrency = crawler.config.runnerConcurrency
                    val (speedText, speedColor) = when {
                        concurrency < 2 -> "Slow" to WarningAmber
                        concurrency > 4 -> "Fast" to SuccessGreen
                        else -> "Average" to SecondaryText
                    }

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(speedColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = speedText,
                        style = MaterialTheme.typography.labelSmall,
                        color = speedColor.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
                
                Text(
                    text = crawler.baseUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/"),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = crawler.language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, crawler.baseUrl.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open in browser",
                    tint = SecondaryText.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
