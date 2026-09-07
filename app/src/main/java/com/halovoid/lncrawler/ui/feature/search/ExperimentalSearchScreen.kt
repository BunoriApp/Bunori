package com.halovoid.lncrawler.ui.feature.search

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.domain.models.SearchItem
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.core.theme.*
import com.halovoid.lncrawler.ui.feature.request.RequestViewModel
import com.halovoid.lncrawler.ui.feature.request.components.CompactSearchResultCard
import com.halovoid.lncrawler.ui.feature.request.components.SearchResultCard
import com.halovoid.lncrawler.ui.feature.request.components.SourceHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalSearchScreen(
    searchViewModel: SearchViewModel,
    requestViewModel: RequestViewModel,
    onBack: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit
) {
    val libraryUrls by requestViewModel.libraryUrls.collectAsStateWithLifecycle()
    val searchState by searchViewModel.searchState.collectAsStateWithLifecycle()
    var isCompactMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isSearching = searchState !is SearchState.Idle

    val searchBarHeight by animateDpAsState(
        targetValue = if (isSearching) 56.dp else 64.dp,
        label = "SearchBarHeight"
    )

    Scaffold(
        containerColor = DarkBackground
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
                
                Text(
                    text = "Experimental Search",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                if (isSearching) {
                    IconButton(onClick = { isCompactMode = !isCompactMode }) {
                        Icon(
                            imageVector = if (isCompactMode) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = if (isCompactMode) "Comfortable View" else "Compact View",
                            tint = PrimaryText
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                AnimatedVisibility(
                    visible = !isSearching,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search the index",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Find novels across supported sources. Results are served from our index, not fetched directly from the source websites.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                if (isSearching) {
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
                            .clip(RoundedCornerShape(if (isSearching) 12.dp else 32.dp))
                            .background(DarkSurface)
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isSearching) PrimaryText else SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )

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

                        if (isSearching || searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        searchViewModel.search(searchQuery)
                                        keyboardController?.hide()
                                    } else {
                                        searchViewModel.resetState()
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
                }

                if (!isSearching) {
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
                            text = "The index is still growing. Some novels may not be available yet. If you do not find your favourite novel create a request for it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText.copy(alpha = 0.6f),
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        when (val state = searchState) {
                            is SearchState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = PrimaryAccent)
                                }
                            }
                            is SearchState.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Error: ${state.message}", color = ErrorRed, modifier = Modifier.padding(16.dp))
                                }
                            }
                            is SearchState.Success -> {
                                if (state.response.results.isEmpty()) {
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
                                                onClick = onBack,
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
                                        state.response.results.forEach { (source, items) ->
                                            item {
                                                SourceHeader(source = source, count = items.size)
                                            }

                                            if (isCompactMode) {
                                                itemsIndexed(items, key = { index, item -> "${source}_${item.url}_$index" }) { index, item ->
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
                                                        itemsIndexed(items, key = { index, item -> "${source}_${item.url}_$index" }) { index, item ->
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

                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Did not find your novel? Try ", color = SecondaryText, fontSize = 14.sp)
                                                TextButton(
                                                    onClick = onBack,
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Requesting", color = BrandAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
