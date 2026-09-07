package com.halovoid.lncrawler.ui.screens.request

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.halovoid.lncrawler.data.repository.UpdateRepository
import com.halovoid.lncrawler.domain.models.SearchItem
import com.halovoid.lncrawler.ui.components.RequestActionHandler
import com.halovoid.lncrawler.ui.components.ScreenHeader
import com.halovoid.lncrawler.ui.screens.crawler.CrawlerScreen
import com.halovoid.lncrawler.ui.screens.crawler.CrawlerViewModel
import com.halovoid.lncrawler.ui.screens.crawler.SyncState
import com.halovoid.lncrawler.ui.components.AppBottomSheet
import com.halovoid.lncrawler.ui.components.AppBottomSheetDivider
import com.halovoid.lncrawler.ui.components.AppBottomSheetGroup
import com.halovoid.lncrawler.ui.theme.*
import com.halovoid.lncrawler.ui.screens.search.GlobalSearchViewModel
import com.halovoid.lncrawler.ui.screens.search.GlobalSearchState
import com.halovoid.lncrawler.ui.screens.search.SourceSearchStatus
import com.halovoid.lncrawler.ui.ViewModelFactory
import android.app.Application
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.itemsIndexed
import com.halovoid.lncrawler.domain.models.Novel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halovoid.lncrawler.data.db.entities.RequestType

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
    val libraryUrls by viewModel.libraryUrls.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
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
                                onSearchActiveChange = { isSearchActive = it },
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

@Composable
fun CompactSearchResultCard(
    item: SearchItem,
    isInLibrary: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceVariant)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (item.imageUrl.isNullOrBlank()) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    tint = SecondaryText.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp).align(Alignment.Center)
                )
            }

            if (isInLibrary) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        color = BrandAccent,
                        shape = CircleShape,
                        modifier = Modifier.size(10.dp),
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {}
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = buildString {
                    append(item.source)
                    if (!item.description.isNullOrBlank()) {
                        append(" · ")
                        append(item.description)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SecondaryText.copy(alpha = 0.2f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun ManualRequestContent(
    viewModel: RequestViewModel,
    searchUrl: String?,
    libraryUrls: Set<String>,
    onNavigateToPreview: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit
) {
    var urlInput by remember { mutableStateOf(searchUrl ?: "") }
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
    ) {
        item {
            Text(
                text = "Request a Novel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Can't find what you're looking for? Submit the novel's URL and we'll add it to the indexing queue.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter novel page URL", color = SecondaryText, fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BrandAccent,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (!isLoading) {
                            val crawlerName = viewModel.validateUrl(urlInput)
                            if (crawlerName != null) {
                                if (libraryUrls.contains(urlInput)) {
                                    onNavigateToDetail(crawlerName, urlInput)
                                } else {
                                    viewModel.pushToRedis(urlInput) {
                                        viewModel.setPreviewUrl(urlInput)
                                        viewModel.setPreviewNovel(null) // We don't have metadata yet
                                        onNavigateToPreview()
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading && urlInput.isNotBlank(),
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BrandAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Submit",
                            tint = if (urlInput.isNotBlank()) PrimaryText else SecondaryText.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text(
                text = "How requesting works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            StepItem(
                number = "01",
                title = "Submit a novel URL",
                description = "Enter the novel's page URL with supported source. You can continue on with adding the novel to your library"
            )
            Spacer(modifier = Modifier.height(20.dp))
            StepItem(
                number = "02",
                title = "We process the request",
                description = "Your request is queued and processed by our server. We take a note if the url can be indexed and if it is possible we index it as soon as possible"
            )
            Spacer(modifier = Modifier.height(20.dp))
            StepItem(
                number = "03",
                title = "It gets indexed",
                description = "Once indexed, it becomes available through Search. Now someone who someday needs to look for the same novel will have easy access to the novel."
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StepItem(number: String, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = BrandAccent.copy(alpha = 0.3f), // Using BrandAccent for step numbers
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun SourceHeader(source: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Text(
            text = "$count results",
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryText
        )
    }
}

@Composable
fun SearchResultCard(
    item: SearchItem,
    isInLibrary: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp) // Smaller width for horizontal scroll scannability
            .clickable { onClick() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = null
                )
                
                if (item.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = SecondaryText.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                if (isInLibrary) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Surface(
                            color = BrandAccent,
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp),
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                Icons.Default.CollectionsBookmark,
                                contentDescription = "In Library",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium, // Smaller font for smaller card
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: com.halovoid.lncrawler.data.db.entities.RequestType?,
    onDismiss: () -> Unit,
    onFilterSelected: (com.halovoid.lncrawler.data.db.entities.RequestType?) -> Unit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestSearchContent(
    viewModel: GlobalSearchViewModel,
    requestViewModel: RequestViewModel,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onNavigateToRequest: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryUrls by requestViewModel.libraryUrls.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    var isCompactMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val activeSearch = searchState !is GlobalSearchState.Idle || isFocused || searchQuery.isNotEmpty()

    LaunchedEffect(activeSearch) {
        onSearchActiveChange(activeSearch)
    }

    androidx.activity.compose.BackHandler(enabled = activeSearch) {
        searchQuery = ""
        viewModel.resetState()
        onSearchActiveChange(false)
        keyboardController?.hide()
    }

    val searchBarHeight by animateDpAsState(
        targetValue = if (isSearchActive) 56.dp else 64.dp,
        label = "SearchBarHeight"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .then(if (isSearchActive) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 24.dp)
    ) {
        AnimatedVisibility(
            visible = !isSearchActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Search all sources",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Search across all installed crawler plugins in real-time. This fetches results directly from the source websites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (isSearchActive) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Search Bar Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(searchBarHeight)
                    .clip(RoundedCornerShape(if (isSearchActive) 12.dp else 32.dp))
                    .background(DarkSurface)
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isSearchActive) PrimaryText else SecondaryText,
                    modifier = Modifier.size(20.dp)
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                    placeholder = { Text("Search for novels...", color = SecondaryText, fontSize = 16.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BrandAccent,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SecondaryText)
                            }
                        }
                    }
                )

                if (isSearchActive || searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.search(searchQuery)
                                keyboardController?.hide()
                            } else {
                                viewModel.resetState()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isNotBlank()) {
                                Icons.AutoMirrored.Filled.ArrowForward
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = if (searchQuery.isNotBlank()) "Search" else "Back to Idle",
                            tint = if (searchQuery.isNotBlank()) PrimaryText else SecondaryText
                        )
                    }
                }
            }

            if (isSearchActive && searchState is GlobalSearchState.Searching) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { isCompactMode = !isCompactMode }) {
                    Icon(
                        imageVector = if (isCompactMode) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = if (isCompactMode) "Comfortable View" else "Compact View",
                        tint = PrimaryText
                    )
                }
            }
        }

        if (!isSearchActive) {
            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SecondaryText.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search fetches results in real-time from active crawlers. If a novel is not found, you can submit a manual crawler request.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (val state = searchState) {
                    is GlobalSearchState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = ErrorRed, modifier = Modifier.padding(16.dp))
                        }
                    }
                    is GlobalSearchState.Searching -> {
                        val allDone = state.sourceStates.all { it.value !is SourceSearchStatus.Loading }
                        val allEmpty = state.sourceStates.all {
                            val status = it.value
                            status is SourceSearchStatus.Success && status.items.isEmpty()
                        }

                        if (allDone && allEmpty) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("No results found", color = SecondaryText)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Did not find your novel? Try ", color = SecondaryText, fontSize = 14.sp)
                                    TextButton(
                                        onClick = onNavigateToRequest,
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Requesting", color = BrandAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = if (isCompactMode) Arrangement.spacedBy(4.dp) else Arrangement.spacedBy(24.dp)
                            ) {
                                state.sourceStates.forEach { (source, status) ->
                                    val count = when (status) {
                                        is SourceSearchStatus.Success -> status.items.size
                                        else -> 0
                                    }

                                    item(key = "header_$source") {
                                        SourceHeader(source = source, count = count)
                                    }

                                    when (status) {
                                        is SourceSearchStatus.Loading -> {
                                            item(key = "loading_$source") {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = BrandAccent,
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                            }
                                        }
                                        is SourceSearchStatus.Error -> {
                                            item(key = "error_$source") {
                                                Text(
                                                    text = "Error: ${status.message}",
                                                    color = ErrorRed,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                        is SourceSearchStatus.Success -> {
                                            if (status.items.isEmpty()) {
                                                item(key = "empty_$source") {
                                                    Text(
                                                        text = "No results found",
                                                        color = SecondaryText.copy(alpha = 0.5f),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                }
                                            } else {
                                                if (isCompactMode) {
                                                    itemsIndexed(status.items, key = { index, item -> "${source}_${item.url}_$index" }) { index, item ->
                                                        val isInLibrary = libraryUrls.contains(item.url)
                                                        CompactSearchResultCard(
                                                            item = item,
                                                            isInLibrary = isInLibrary,
                                                            onClick = {
                                                                handleSearchResultClick(
                                                                    item = item,
                                                                    isInLibrary = isInLibrary,
                                                                    onNavigateToDetail = onNavigateToDetail,
                                                                    requestViewModel = requestViewModel,
                                                                    onNavigateToPreview = onNavigateToPreview
                                                                )
                                                            }
                                                        )
                                                    }
                                                } else {
                                                    item(key = "row_$source") {
                                                        LazyRow(
                                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                            contentPadding = PaddingValues(bottom = 8.dp)
                                                        ) {
                                                            itemsIndexed(status.items, key = { index, item -> "${source}_${item.url}_$index" }) { index, item ->
                                                                val isInLibrary = libraryUrls.contains(item.url)
                                                                SearchResultCard(
                                                                    item = item,
                                                                    isInLibrary = isInLibrary,
                                                                    onClick = {
                                                                        handleSearchResultClick(
                                                                            item = item,
                                                                            isInLibrary = isInLibrary,
                                                                            onNavigateToDetail = onNavigateToDetail,
                                                                            requestViewModel = requestViewModel,
                                                                            onNavigateToPreview = onNavigateToPreview
                                                                        )
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (allDone && !allEmpty) {
                                    item(key = "asking_request") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Did not find your novel? Try ", color = SecondaryText, fontSize = 14.sp)
                                            TextButton(
                                                onClick = onNavigateToRequest,
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Requesting", color = BrandAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

private fun handleSearchResultClick(
    item: SearchItem,
    isInLibrary: Boolean,
    onNavigateToDetail: (String, String) -> Unit,
    requestViewModel: RequestViewModel,
    onNavigateToPreview: () -> Unit
) {
    if (isInLibrary) {
        onNavigateToDetail(item.source, item.url)
    } else {
        requestViewModel.setPreviewUrl(item.url)
        requestViewModel.setPreviewNovel(
            Novel(
                url = item.url,
                title = item.title,
                description = item.description,
                coverUrl = item.imageUrl,
                coverHttpsUrl = item.imageUrl,
                crawlerName = item.source
            )
        )
        onNavigateToPreview()
    }
}
