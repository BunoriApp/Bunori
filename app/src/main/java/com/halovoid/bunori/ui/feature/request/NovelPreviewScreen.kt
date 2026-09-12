package com.halovoid.bunori.ui.feature.request

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.halovoid.bunori.domain.models.Chapter
import com.halovoid.bunori.domain.models.Novel
import com.halovoid.bunori.ui.core.components.AppBottomSheet
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.novel.components.NovelDetailsBottomSheet
import com.halovoid.bunori.ui.feature.novel.components.NovelHeroSection
import com.halovoid.bunori.ui.feature.novel.components.NovelMetadataTable
import com.halovoid.bunori.ui.feature.novel.components.NovelSynopsisSection
import com.halovoid.bunori.ui.feature.request.components.ChapterPreviewReaderSheet
import com.halovoid.bunori.ui.feature.request.components.NovelPreviewActionRow

@Composable
fun NovelPreviewScreen(
    viewModel: RequestViewModel,
    onBack: () -> Unit,
    onConfirm: (Novel) -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    onCrawlManually: (String, String, String) -> Unit
) {
    val novel by viewModel.novelPreview.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isChaptersLoading by viewModel.isChaptersLoading.collectAsStateWithLifecycle()
    val readingChapter by viewModel.readingChapter.collectAsStateWithLifecycle()
    val chapterContent by viewModel.chapterContent.collectAsStateWithLifecycle()
    val isChapterContentLoading by viewModel.isChapterContentLoading.collectAsStateWithLifecycle()
    val previewUrl by viewModel.previewUrl.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val similarNovels by viewModel.similarNovels.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is RequestUiEvent.NavigateToDetail -> {
                    onNavigateToDetail(event.crawlerName, event.novelUrl)
                }
                is RequestUiEvent.NavigateBack -> {
                    onBack()
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(previewUrl) {
        if (previewUrl != null) {
            viewModel.fetchNovelPreview(previewUrl!!)
        }
    }

    NovelPreviewContent(
        novel = novel,
        isLoading = isLoading,
        isChaptersLoading = isChaptersLoading,
        error = error,
        onBack = onBack,
        onConfirm = onConfirm,
        onChapterClick = { chapter ->
            novel?.let { viewModel.openChapter(chapter, it.crawlerName) }
        },
        onCrawlManually = {
            if (previewUrl != null) {
                onCrawlManually(novel?.crawlerName ?: "", previewUrl!!, novel?.title ?: previewUrl!!)
            }
        }
    )

    val currentReadingChapter = readingChapter
    if (currentReadingChapter != null) {
        ChapterPreviewReaderSheet(
            chapter = currentReadingChapter,
            isLoading = isChapterContentLoading,
            contentHtml = chapterContent,
            onDismiss = { viewModel.closeChapter() }
        )
    }

    if (similarNovels.isNotEmpty()) {
        SimilarityBottomSheet(
            similarNovels = similarNovels,
            onDismiss = { viewModel.clearSimilarNovels() },
            onConfirmAnyway = {
                novel?.let { viewModel.saveNovel(it) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarityBottomSheet(
    similarNovels: List<Novel>,
    onDismiss: () -> Unit,
    onConfirmAnyway: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        title = "Similar Novels Found",
        subtitle = "You already have similar novels in your library. Do you still want to add this one?"
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(similarNovels) { novel ->
                SimilarNovelCard(novel)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onConfirmAnyway,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Proceed Anyway", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel", color = SecondaryText)
        }
    }
}

@Composable
fun SimilarNovelCard(novel: Novel) {
    var coverModel by remember(novel.coverUrl, novel.coverHttpsUrl) {
        mutableStateOf(novel.coverUrl ?: novel.coverHttpsUrl)
    }
    Column(
        modifier = Modifier.width(120.dp)
    ) {
        AsyncImage(
            model = coverModel,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface),
            contentScale = ContentScale.Crop,
            onError = {
                if (coverModel != novel.coverHttpsUrl) {
                    coverModel = novel.coverHttpsUrl
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = novel.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = novel.author ?: "Unknown Author",
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NovelPreviewContent(
    novel: Novel?,
    isLoading: Boolean,
    isChaptersLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConfirm: (Novel) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onCrawlManually: () -> Unit
) {
    val context = LocalContext.current

    if (novel == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(color = BrandAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fetching novel details...",
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Cancel", color = SecondaryText)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    if (error != null) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            color = ErrorRed,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Button(
                        onClick = onCrawlManually,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurface,
                            contentColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Crawl Manually", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Go Back",
                            color = SecondaryText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        return
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

    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                NovelHeroSection(novel = novel)
            }

            item {
                NovelMetadataTable(
                    novel = novel,
                    onClick = { showDetailsSheet = true }
                )
            }

            item {
                NovelPreviewActionRow(
                    isAdding = isLoading,
                    onAddClick = { onConfirm(novel) },
                    onWebViewClick = {
                        val intent = Intent(Intent.ACTION_VIEW, novel.url.toUri())
                        context.startActivity(intent)
                    }
                )
            }

            if (error != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        color = ErrorRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = error,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (!novel.description.isNullOrBlank()) {
                item {
                    NovelSynopsisSection(
                        novel = novel,
                        isExpanded = isSynopsisExpanded,
                        onExpandClick = { isSynopsisExpanded = !isSynopsisExpanded }
                    )
                }
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
                        text = if (isChaptersLoading && novel.chapters.isEmpty()) "Chapters" else "${novel.chapters.size} Chapters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
            }

            if (isChaptersLoading && novel.chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = BrandAccent,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading chapters...",
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else if (novel.chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No chapters found in preview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                    }
                }
            } else {
                items(
                    items = novel.chapters,
                    key = { chapter ->
                        if (chapter.id != 0) chapter.id else "${chapter.index}_${chapter.url.ifBlank { chapter.title }}"
                    }
                ) { chapter ->
                    ChapterPreviewRow(
                        chapter = chapter,
                        onClick = { onChapterClick(chapter) }
                    )
                    HorizontalDivider(
                        color = BorderColor.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Top Bar Overlay
        NovelPreviewTopBar(
            title = novel.title,
            isOpaque = isTopBarOpaque,
            showTitle = showTitleInTopBar,
            isLoading = isLoading,
            onBack = onBack
        )
    }

    if (showDetailsSheet) {
        NovelDetailsBottomSheet(
            novel = novel,
            onDismiss = { showDetailsSheet = false }
        )
    }
}

@Composable
fun NovelPreviewTopBar(
    title: String,
    isOpaque: Boolean,
    showTitle: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isOpaque) DarkBackground else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "PreviewTopBarBackground"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryText
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showTitle,
                        enter = fadeIn(animationSpec = tween(durationMillis = 250)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 200))
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    if (!showTitle) {
                        Surface(
                            color = DarkSurfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "PREVIEW",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandAccent,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = BrandAccent,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
private fun ChapterPreviewRow(
    chapter: Chapter,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chapter ${chapter.index}",
                color = PrimaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            val hasTitle = chapter.title.isNotBlank() && chapter.title != "Chapter ${chapter.index}"
            val hasSource = chapter.scanlationSource.isNotBlank() && chapter.scanlationSource != "NotProvided"

            if (hasTitle || hasSource) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasTitle) {
                        Text(
                            text = chapter.title,
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (hasSource) {
                        if (hasTitle) {
                            Text(
                                text = " • ",
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = chapter.scanlationSource,
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = "Read Chapter",
            tint = SecondaryText.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
