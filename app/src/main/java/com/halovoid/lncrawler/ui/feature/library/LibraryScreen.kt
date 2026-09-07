package com.halovoid.lncrawler.ui.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.ui.core.components.AppBottomSheet
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetDivider
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetGroup
import com.halovoid.lncrawler.ui.core.components.MutedEmptyState
import com.halovoid.lncrawler.ui.core.components.ScreenHeader
import com.halovoid.lncrawler.ui.core.theme.*
import com.halovoid.lncrawler.ui.feature.library.components.NovelCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNovelClick: (String, String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: LibraryViewModel
) {
    val novels by viewModel.novels.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf("Any") }
    
    var isSearching by remember { mutableStateOf(false) }

    val filteredNovels by remember(novels, searchQuery, selectedDomain) {
        derivedStateOf {
            novels.filter {
                (selectedDomain == "Any" || it.crawlerName == selectedDomain) &&
                        it.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val domains = listOf("Any") + novels.map { it.crawlerName }.distinct()

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            ScreenHeader(
                title = "Novels",
                subtitle = if (novels.isNotEmpty()) "${filteredNovels.size} novels" else null,
                isExpanded = isSearching,
                expandedContent = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search library...", fontSize = 14.sp) },
                        leadingIcon = { 
                            IconButton(onClick = { 
                                isSearching = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        singleLine = true
                    )
                },
                actions = {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryText)
                    }
                    
                    var showFilter by remember { mutableStateOf(false) }
                    IconButton(onClick = { showFilter = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedDomain != "Any") BrandAccent else PrimaryText
                        )
                    }

                    if (showFilter) {
                        LibraryFilterBottomSheet(
                            selected = selectedDomain,
                            options = domains,
                            onDismiss = { showFilter = false },
                            onSelected = { selected ->
                                selectedDomain = selected
                                showFilter = false
                            }
                        )
                    }
                }
            )

            if (filteredNovels.isEmpty()) {
                MutedEmptyState(
                    title = "Your Library is Empty",
                    description = "Manage your personal light novel collection here. You can track your reading progress and organize your favorites. This page will come alive once you add novels from the browse section.",
                    icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNovels, key = { it.url }) { novel ->
                        NovelCard(
                            novel = novel,
                            onClick = { onNovelClick(novel.crawlerName, novel.url) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterBottomSheet(
    selected: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Filter Sources"
    ) {
        AppBottomSheetGroup {
            options.forEachIndexed { index, option ->
                ListItem(
                    headlineContent = { Text(option, color = PrimaryText) },
                    trailingContent = {
                        if (selected == option) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSelected(option) }
                )
                if (index < options.lastIndex) {
                    AppBottomSheetDivider()
                }
            }
        }
    }
}
