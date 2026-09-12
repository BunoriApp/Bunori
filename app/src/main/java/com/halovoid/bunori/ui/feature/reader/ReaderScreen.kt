package com.halovoid.bunori.ui.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.ui.core.platform.SystemBarHandler
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.reader.components.TableOfContentsSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

/**
 * Same overall shell and windowing behavior as before (continuous scroll,
 * center-chapter detection, immersive mode), plus:
 * - a Table of Contents button in the top bar (jump-to-chapter),
 * - paragraph/block selection (long-press to start, tap to extend/clear),
 *   as a foundation for future bookmarking/highlighting/notes features.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    novelUrl: String,
    initialChapterId: Int,
    onBack: () -> Unit,
    viewModel: ReaderViewModel
) {
    val window by viewModel.window.collectAsStateWithLifecycle()
    val currentChapter by viewModel.currentChapter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val tocChapters by viewModel.tocChapters.collectAsStateWithLifecycle()
    val scrollRequest by viewModel.scrollRequest.collectAsStateWithLifecycle()
    val selectedBlockIds by viewModel.selectedBlockIds.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var isControlsVisible by remember { mutableStateOf(true) }
    var hasScrolledToInitial by remember { mutableStateOf(false) }
    var isTocVisible by remember { mutableStateOf(false) }

    val selectionModeActive = selectedBlockIds.isNotEmpty()

    SystemBarHandler(isSystemBarsVisible = isControlsVisible)

    LaunchedEffect(novelUrl, initialChapterId) {
        viewModel.start(novelUrl, initialChapterId)
    }

    LaunchedEffect(window) {
        if (window.isNotEmpty() && !hasScrolledToInitial) {
            val index = window.indexOfFirst { it.chapter.id == initialChapterId }
            if (index != -1) {
                listState.scrollToItem(index)
                hasScrolledToInitial = true
            }
        }
    }

    // Jumping from the Table of Contents scrolls once the target chapter
    // has actually loaded into the window.
    LaunchedEffect(scrollRequest, window) {
        val request = scrollRequest ?: return@LaunchedEffect
        val index = window.indexOfFirst { it.chapter.id == request.chapterId }
        if (index != -1) {
            listState.scrollToItem(index)
            viewModel.consumeScrollRequest(request.token)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .mapNotNull { visibleItems ->
                if (visibleItems.isEmpty()) return@mapNotNull null
                val viewportCenter = (listState.layoutInfo.viewportEndOffset + listState.layoutInfo.viewportStartOffset) / 2
                val centerItem = visibleItems.minByOrNull {
                    val itemCenter = it.offset + it.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                centerItem?.key as? Int
            }
            .distinctUntilChanged()
            .collect { chapterId ->
                viewModel.onCenterChapterChanged(chapterId)
            }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ReaderTopBar(
                    title = currentChapter?.title ?: "Reader",
                    selectionCount = selectedBlockIds.size,
                    onBack = onBack,
                    onOpenToc = { isTocVisible = true },
                    onClearSelection = { viewModel.clearSelection() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isControlsVisible) innerPadding.calculateTopPadding() else 0.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (selectionModeActive) viewModel.clearSelection()
                    else isControlsVisible = !isControlsVisible
                }
        ) {
            if (isLoading && window.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryAccent
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 32.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(window, key = { it.chapter.id }) { loadedChapter ->
                        ChapterContent(
                            loadedChapter = loadedChapter,
                            selectedBlockIds = selectedBlockIds,
                            selectionModeActive = selectionModeActive,
                            onToggleSelect = viewModel::toggleBlockSelection,
                            onBackgroundTap = { isControlsVisible = !isControlsVisible },
                            onReload = viewModel::reloadChapter
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isControlsVisible && !selectionModeActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                val currentNum by viewModel.currentChapterNumber.collectAsStateWithLifecycle()
                val totalNum by viewModel.totalChapters.collectAsStateWithLifecycle()
                if (totalNum > 0 && currentNum > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "$currentNum/$totalNum",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (isTocVisible) {
        TableOfContentsSheet(
            chapters = tocChapters,
            currentChapterId = currentChapter?.id,
            onChapterSelected = { chapterId -> viewModel.jumpToChapter(chapterId) },
            onDismiss = { isTocVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    selectionCount: Int,
    onBack: () -> Unit,
    onOpenToc: () -> Unit,
    onClearSelection: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = if (selectionCount > 0) "$selectionCount selected" else title, maxLines = 1)
        },
        navigationIcon = {
            IconButton(onClick = if (selectionCount > 0) onClearSelection else onBack) {
                Icon(
                    imageVector = if (selectionCount > 0) Icons.Filled.Close else Icons.Filled.ArrowBack,
                    contentDescription = if (selectionCount > 0) "Clear selection" else "Back"
                )
            }
        },
        actions = {
            if (selectionCount == 0) {
                IconButton(onClick = onOpenToc) {
                    Icon(imageVector = Icons.Filled.List, contentDescription = "Table of contents")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
            titleContentColor = PrimaryText,
            navigationIconContentColor = PrimaryText,
            actionIconContentColor = PrimaryText
        )
    )
}

@Composable
private fun ChapterContent(
    loadedChapter: LoadedChapter,
    selectedBlockIds: Set<String>,
    selectionModeActive: Boolean,
    onToggleSelect: (String) -> Unit,
    onBackgroundTap: () -> Unit,
    onReload: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(
                    modifier = Modifier.width(60.dp),
                    color = PrimaryAccent.copy(alpha = 0.3f),
                    thickness = 2.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${loadedChapter.chapter.title} started",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryAccent.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
            }
        }

        loadedChapter.document.blocks.forEach { block ->
            BlockItem(
                block = block,
                isSelected = block.id in selectedBlockIds,
                selectionModeActive = selectionModeActive,
                onToggleSelect = onToggleSelect,
                onBackgroundTap = onBackgroundTap,
                onReloadChapter = { onReload(loadedChapter.chapter.id) }
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${loadedChapter.chapter.title} ended",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.width(60.dp),
                    color = SecondaryText.copy(alpha = 0.2f),
                    thickness = 2.dp
                )
            }
        }
    }
}