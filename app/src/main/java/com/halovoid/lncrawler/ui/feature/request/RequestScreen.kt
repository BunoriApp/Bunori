package com.halovoid.lncrawler.ui.feature.request

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.core.components.ScreenHeader
import com.halovoid.lncrawler.ui.core.theme.*
import com.halovoid.lncrawler.ui.feature.crawler.CrawlerScreen
import com.halovoid.lncrawler.ui.feature.crawler.CrawlerViewModel
import com.halovoid.lncrawler.ui.feature.crawler.SyncState
import com.halovoid.lncrawler.ui.feature.request.components.ManualRequestContent
import com.halovoid.lncrawler.ui.feature.request.components.RequestActionHandler
import com.halovoid.lncrawler.ui.feature.request.components.RequestSearchContent
import com.halovoid.lncrawler.ui.feature.search.GlobalSearchViewModel

enum class RequestTab {
    SEARCH, CRAWLERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    onNavigateToPreview: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToRequest: () -> Unit,
    viewModel: RequestViewModel,
    crawlerViewModel: CrawlerViewModel,
    searchUrl: String? = null
) {
    var selectedTab by remember { mutableStateOf(RequestTab.SEARCH) }
    var isSearchActive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val globalSearchViewModel: GlobalSearchViewModel = viewModel(
        factory = remember { ViewModelFactory(context.applicationContext as Application) }
    )
    val isCrawlerUpdateAvailable by crawlerViewModel.isUpdateAvailable.collectAsStateWithLifecycle()

    RequestActionHandler(
        onResolveCloudflare = { id, url -> viewModel.resolveCloudflare(id, url) }
    ) {
        Scaffold(
            containerColor = DarkBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        ScreenHeader(
                            title = "Browse",
                            actions = {
                                if (selectedTab == RequestTab.CRAWLERS) {
                                    val syncState by crawlerViewModel.syncState.collectAsStateWithLifecycle()
                                    val showSyncOption by crawlerViewModel.showSyncOption.collectAsStateWithLifecycle()

                                    if (syncState is SyncState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.padding(end = 12.dp).size(20.dp),
                                            color = BrandAccent,
                                            strokeWidth = 2.dp
                                        )
                                    } else if (showSyncOption) {
                                        IconButton(onClick = { crawlerViewModel.syncCrawlers() }) {
                                            Icon(
                                                Icons.Default.Sync,
                                                contentDescription = "Sync Crawlers",
                                                tint = BrandAccent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        TabRow(
                            selectedTabIndex = selectedTab.ordinal,
                            containerColor = Color.Transparent,
                            contentColor = BrandAccent,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                    color = BrandAccent,
                                    height = 3.dp
                                )
                            },
                            divider = {},
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .height(48.dp)
                        ) {
                            Tab(
                                selected = selectedTab == RequestTab.SEARCH,
                                onClick = { selectedTab = RequestTab.SEARCH },
                                text = {
                                    Text(
                                        "Search",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selectedTab == RequestTab.SEARCH) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == RequestTab.SEARCH) PrimaryText else SecondaryText
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == RequestTab.CRAWLERS,
                                onClick = { selectedTab = RequestTab.CRAWLERS },
                                text = {
                                    BadgedBox(
                                        badge = {
                                            if (isCrawlerUpdateAvailable) {
                                                Badge(
                                                    containerColor = BrandAccent,
                                                    contentColor = Color.White
                                                ) {
                                                    Text("1", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    ) {
                                        Text(
                                            "Crawlers",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (selectedTab == RequestTab.CRAWLERS) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == RequestTab.CRAWLERS) PrimaryText else SecondaryText
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        RequestTab.SEARCH -> {
                            RequestSearchContent(
                                viewModel = globalSearchViewModel,
                                requestViewModel = viewModel,
                                isSearchActive = isSearchActive,
                                onSearchActiveChange = { active -> isSearchActive = active },
                                onNavigateToRequest = onNavigateToRequest,
                                onNavigateToPreview = onNavigateToPreview,
                                onNavigateToDetail = onNavigateToDetail
                            )
                        }
                        RequestTab.CRAWLERS -> {
                            CrawlerScreen(
                                viewModel = crawlerViewModel,
                                showHeader = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualRequestScreen(
    viewModel: RequestViewModel,
    searchUrl: String?,
    onBack: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit
) {
    val libraryUrls by viewModel.libraryUrls.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Novel", color = PrimaryText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ManualRequestContent(
                viewModel = viewModel,
                searchUrl = searchUrl,
                libraryUrls = libraryUrls,
                onNavigateToPreview = onNavigateToPreview,
                onNavigateToDetail = onNavigateToDetail
            )
        }
    }
}
