package com.halovoid.bunori.ui.feature.novel

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halovoid.bunori.data.db.entities.JobStatus
import com.halovoid.bunori.data.db.entities.JobType
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.ui.ViewModelFactory
import com.halovoid.bunori.ui.core.components.ConfirmDeleteDialog
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.downloads.components.DownloadRangeDialog
import com.halovoid.bunori.ui.feature.novel.components.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import kotlinx.coroutines.launch

sealed interface NovelDetailDialogState {
    data object ConfirmDelete : NovelDetailDialogState
    data object DownloadRange : NovelDetailDialogState
    data object FilterSheet : NovelDetailDialogState
    data object NovelDetails : NovelDetailDialogState
    data object SourceFilterSheet : NovelDetailDialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelUrl: String,
    onRequestClick: (String) -> Unit,
    onChapterClick: (String, Int) -> Unit,
    onActivityClick: () -> Unit,
    onArtifactsClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val factory = remember { ViewModelFactory(context.applicationContext as Application) }
    val viewModel: NovelDetailViewModel = viewModel(factory = factory)
    
    val novel by viewModel.novel.collectAsState()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val downloadFilter by viewModel.downloadFilter.collectAsStateWithLifecycle()
    val sortState by viewModel.sortState.collectAsStateWithLifecycle()
    val chapterRange by viewModel.chapterRange.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedChapterIds by viewModel.selectedChapterIds.collectAsStateWithLifecycle()
    val selectedSources by viewModel.selectedSources.collectAsStateWithLifecycle()
    val availableSources by viewModel.availableSources.collectAsStateWithLifecycle()

    androidx.activity.compose.BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }
    val listState = rememberLazyListState()
    val density = LocalResources.current.displayMetrics.density
    val heroHeightPx = remember { (280 * density).toInt() }

    val isTopBarOpaque by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val showTitleInTopBar by remember {
        derivedStateOf {
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            firstItemIndex > 0 || (firstItemIndex == 0 && firstItemOffset > heroHeightPx)
        }
    }

    var descriptionExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val requestHistory by viewModel.rootRequests.collectAsStateWithLifecycle()

    val ongoingStatuses = remember {
        setOf(
            JobStatus.PENDING,
            JobStatus.RUNNING,
            JobStatus.CANCELLING,
            JobStatus.PAUSED,
            JobStatus.BLOCKED
        )
    }
    
    val downloadingChapters = remember(requestHistory) {
        val ids = mutableSetOf<Int>()
        val ranges = mutableListOf<ClosedRange<Int>>()
        
        requestHistory.filter { it.rstatus in ongoingStatuses }.forEach { req ->
            val meta = req.parsedMetadata
            if (req.type == JobType.CHAPTER) {
                meta.chapterId?.let { ids.add(it) }
            } else if (req.type == JobType.RANGE_DOWNLOAD) {
                val start = meta.startIndex
                val end = meta.endIndex
                if (start != null && end != null) {
                    ranges.add(start..end)
                }
            }
        }
        Pair(ids, ranges)
    }

    var activeDialog by remember { mutableStateOf<NovelDetailDialogState?>(null) }

    val isFilterActive = downloadFilter != DownloadFilter.ALL
    val isSourceFilterActive = availableSources.isNotEmpty() && selectedSources.size < availableSources.size
    val isSortModified = sortState.type != SortType.CHAPTER_NUMBER || sortState.order != SortOrder.ASCENDING

    LaunchedEffect(novelUrl) {
        viewModel.loadNovel(novelUrl)
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (novel == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            val currentNovel = novel!!
            Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        coroutineScope.launch {
                            isRefreshing = true
                            viewModel.fetchNovelMetadata(currentNovel)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    state = pullRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 48.dp),
                            containerColor = DarkSurface,
                            color = BrandAccent
                        )
                    }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                    item {
                        NovelHeroSection(novel = currentNovel)
                    }

                    item {
                        NovelMetadataTable(
                            novel = currentNovel,
                            onClick = { activeDialog = NovelDetailDialogState.NovelDetails }
                        )
                    }

                    item {
                        NovelActionRow(
                            activityExists = requestHistory.isNotEmpty(),
                            isActivityRunning = requestHistory.any { it.rstatus in ongoingStatuses },
                            artifactsExist = chapters.isNotEmpty(),
                            downloadEnabled = chapters.isNotEmpty(),
                            onActivityClick = onActivityClick,
                            onDownloadClick = { activeDialog = NovelDetailDialogState.DownloadRange },
                            onArtifactsClick = onArtifactsClick,
                            onWebViewClick = {
                                val intent = Intent(Intent.ACTION_VIEW, currentNovel.url.toUri())
                                context.startActivity(intent)
                            }
                        )
                    }

                    val activeRequest = requestHistory.find { it.rstatus in ongoingStatuses }
                    if (activeRequest != null) {
                        item {
                            ActiveRequestCard(
                                batch = activeRequest,
                                onClick = { onRequestClick(activeRequest.id) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        NovelSynopsisSection(
                            novel = currentNovel,
                            isExpanded = descriptionExpanded,
                            onExpandClick = { descriptionExpanded = !descriptionExpanded }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = null,
                                tint = SecondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${chapters.size} Chapters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                        }
                    }

                    novelTableOfContents(
                        chapters = chapters,
                        downloadingChapters = downloadingChapters,
                        isSelectionMode = isSelectionMode,
                        selectedChapterIds = selectedChapterIds,
                        onFetchChapter = { viewModel.fetchChapter(currentNovel, it) },
                        onDeleteChapter = { viewModel.deleteChapter(it) },
                        onReplayChapter = { viewModel.replayChapter(currentNovel, it) },
                        onChapterClick = { onChapterClick(currentNovel.url, it.id) },
                        onChapterLongClick = { viewModel.selectChapter(it.id) },
                        onChapterToggleSelect = { viewModel.toggleChapterSelection(it.id) }
                    )
                }
            }

            NovelTopBar(
                    novel = currentNovel,
                    isOpaque = isTopBarOpaque,
                    showTitle = showTitleInTopBar,
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedChapterIds.size,
                    onBack = onBack,
                    onClearSelection = { viewModel.clearSelection() },
                    onMarkAsRead = { viewModel.markSelectedChaptersRead(true) },
                    onMarkAsUnread = { viewModel.markSelectedChaptersRead(false) },
                    onSelectAll = { viewModel.selectAllChapters(chapters) },
                    onUnselectAll = { viewModel.clearSelection() },
                    onFilterClick = { activeDialog = NovelDetailDialogState.FilterSheet },
                    isFilterActive = isFilterActive || isSortModified,
                    onSourceFilterClick = { activeDialog = NovelDetailDialogState.SourceFilterSheet },
                    isSourceFilterActive = isSourceFilterActive,
                    onRefreshMetadata = { viewModel.fetchNovelMetadata(currentNovel) },
                    onDeleteNovel = { activeDialog = NovelDetailDialogState.ConfirmDelete }
                )
            }

            when (activeDialog) {
                is NovelDetailDialogState.ConfirmDelete -> {
                    ConfirmDeleteDialog(
                        title = "Delete request?",
                        message = "This will permanently remove \"${currentNovel.title}\" from your request history. This action cannot be undone.",
                        onConfirm = {
                            activeDialog = null
                            viewModel.deleteNovelPermanently(currentNovel)
                            onBack()
                        },
                        onDismiss = { activeDialog = null }
                    )
                }
                is NovelDetailDialogState.DownloadRange -> {
                    val minIndex = chapters.minOfOrNull { it.index.toFloat() } ?: 1f
                    val maxIndex = chapters.maxOfOrNull { it.index.toFloat() } ?: currentNovel.chapters.size.toFloat().coerceAtLeast(1f)
                    val activeSources = if (selectedSources.isNotEmpty()) {
                        selectedSources.toList()
                    } else {
                        availableSources
                    }
                    DownloadRangeDialog(
                        initialRange = chapterRange,
                        minChapterIndex = minIndex,
                        maxChapterIndex = maxIndex,
                        sources = activeSources,
                        onConfirm = { range ->
                            viewModel.updateChapterRange(range)
                            viewModel.fetchRange(currentNovel)
                            activeDialog = null
                        },
                        onDismiss = { activeDialog = null }
                    )
                }
                is NovelDetailDialogState.FilterSheet -> {
                    ChapterFilterSortSheet(
                        downloadFilter = downloadFilter,
                        sortState = sortState,
                        onSetFilter = { viewModel.setDownloadFilter(it) },
                        onToggleAlphabetical = { viewModel.toggleAlphabeticalSort() },
                        onToggleChapterNumber = { viewModel.toggleChapterNumberSort() },
                        onDismiss = { activeDialog = null }
                    )
                }
                is NovelDetailDialogState.NovelDetails -> {
                    NovelDetailsBottomSheet(
                        novel = currentNovel,
                        onDismiss = { activeDialog = null }
                    )
                }
                is NovelDetailDialogState.SourceFilterSheet -> {
                    SourceFilterBottomSheet(
                        availableSources = availableSources,
                        selectedSources = selectedSources,
                        onToggleSource = { viewModel.toggleSourceSelection(it) },
                        onDismiss = { activeDialog = null }
                    )
                }
                null -> Unit
            }
        }
    }
}
