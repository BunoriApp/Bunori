package com.halovoid.bunori.ui.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.domain.models.Novel
import com.halovoid.bunori.domain.models.SearchItem
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.request.RequestViewModel
import com.halovoid.bunori.ui.feature.request.components.CompactSearchResultCard
import com.halovoid.bunori.ui.feature.request.components.SearchResultCard
import com.halovoid.bunori.ui.feature.request.components.SourceHeader

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    requestViewModel: RequestViewModel,
    onBack: () -> Unit,
    onNavigateToRequest: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    initialSource: String? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember(initialSource) { mutableStateOf(initialSource) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val isCompactMode by viewModel.searchCompactView.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val libraryUrls by requestViewModel.libraryUrls.collectAsStateWithLifecycle()
    val failedExtensions by viewModel.failedExtensions.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(initialSource) {
        if (initialSource != null) {
            viewModel.setSelectedSource(initialSource)
        }
    }

    val failedSearchSources = remember(searchState) {
        (searchState as? SearchState.Searching)?.sourceStates
            ?.filterValues { it is SourceSearchStatus.Error }
            ?.keys?.toList() ?: emptyList()
    }
    val allFailedSources = remember(failedExtensions, failedSearchSources) {
        (failedExtensions + failedSearchSources).distinct()
    }

    BackHandler {
        searchQuery = ""
        viewModel.resetState()
        onBack()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Space-Efficient Full-Width Search Bar with Embedded Source Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSearchFocused) BrandAccent.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            viewModel.resetState()
                            onBack()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Embedded Source Filter Box
                    selectedSource?.let { source ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandAccent.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BrandAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        selectedSource = null
                                        viewModel.setSelectedSource(null)
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear source filter",
                                        tint = BrandAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (selectedSource != null) "Search in $selectedSource..." else "Search for novels...",
                                color = SecondaryText.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = PrimaryText,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(BrandAccent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.search(searchQuery.trim(), selectedSource)
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { isSearchFocused = it.isFocused }
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                viewModel.resetState()
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Small Note at top if any source failed to load (NOT IN RED)
            if (allFailedSources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SecondaryText.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (allFailedSources.size == 1) {
                                "${allFailedSources.first()} could not be reached"
                            } else {
                                "${allFailedSources.size} sources could not be reached"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 3. Search Results Sub-Header & Controls Bar (Grid / List view toggle)
            if (searchState is SearchState.Searching) {
                val state = searchState as SearchState.Searching
                val allDone = state.sourceStates.all { it.value !is SourceSearchStatus.Loading }
                val totalNovels = state.sourceStates.values.sumOf {
                    (it as? SourceSearchStatus.Success)?.items?.size ?: 0
                }
                val totalSources = state.sourceStates.size
                val completedSources = state.sourceStates.values.count { it !is SourceSearchStatus.Loading }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!allDone) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = BrandAccent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = if (selectedSource != null) "Searching $selectedSource..." else "Searching ($completedSources/$totalSources sources)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = if (totalNovels > 0) {
                                    if (selectedSource != null) "$totalNovels results in $selectedSource" else "$totalNovels results across $totalSources sources"
                                } else {
                                    "Search completed"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.setSearchCompactView(!isCompactMode) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompactMode) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = if (isCompactMode) "Switch to Comfortable View" else "Switch to Compact View",
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Search Content
            Box(modifier = Modifier.weight(1f)) {
                when (val state = searchState) {
                    is SearchState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = SecondaryText.copy(alpha = 0.25f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Search across all sources",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryText.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Enter a title or keyword to find novels in real-time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    is SearchState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = SecondaryText, modifier = Modifier.padding(16.dp))
                        }
                    }
                    is SearchState.Searching -> {
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
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = SecondaryText.copy(alpha = 0.3f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No results found for \"${state.query}\"", color = SecondaryText)
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
                                                    text = "Could not reach source",
                                                    color = SecondaryText.copy(alpha = 0.6f),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(vertical = 4.dp)
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
