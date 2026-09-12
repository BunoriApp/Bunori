package com.halovoid.bunori.ui.feature.request.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.domain.models.Novel
import com.halovoid.bunori.domain.models.SearchItem
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.request.RequestViewModel
import com.halovoid.bunori.ui.feature.search.SearchState
import com.halovoid.bunori.ui.feature.search.SearchViewModel
import com.halovoid.bunori.ui.feature.search.SourceSearchStatus

/**
 * the screen for searching, a child to the request screen
 *  isSearchActive - used for deciding when to go full screen for search and when to show just the box
 *  isCompactMode - this is for toggling between grid and compact mode
 *  upon cancelling the request the search query is made blank and the user can search again
 *
 *  this mostly handles the animation part of the search screen + result fetching
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestSearchContent(
    viewModel: SearchViewModel,
    requestViewModel: RequestViewModel,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onNavigateToRequest: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val isCompactMode by viewModel.searchCompactView.collectAsStateWithLifecycle()

    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val libraryUrls by requestViewModel.libraryUrls.collectAsStateWithLifecycle()

    val isSearching = searchState is SearchState.Searching
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchActive) {
        searchQuery = ""
        viewModel.resetState()
        onSearchActiveChange(false)
        focusManager.clearFocus()
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
                Icon(Icons.Default.Search, contentDescription = null, tint = if (isSearchActive) PrimaryText else SecondaryText, modifier = Modifier.size(20.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
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
                                onSearchActiveChange(true)
                                viewModel.search(searchQuery)
                                keyboardController?.hide()
                            } else {
                                searchQuery = ""
                                onSearchActiveChange(false)
                                keyboardController?.hide()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isNotBlank()) {
                                Icons.AutoMirrored.Filled.ArrowForward
                            } else {
                                Icons.Default.Cancel
                            },
                            contentDescription = if (searchQuery.isNotBlank()) "Search" else "Back to Idle",
                            tint = if (searchQuery.isNotBlank()) PrimaryText else SecondaryText
                        )
                    }
                }
            }

            if (isSearchActive && searchState is SearchState.Searching) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.setSearchCompactView(!isCompactMode) }) {
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
                    is SearchState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = ErrorRed, modifier = Modifier.padding(16.dp))
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