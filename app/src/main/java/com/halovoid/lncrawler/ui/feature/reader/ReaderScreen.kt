package com.halovoid.lncrawler.ui.feature.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.ui.core.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

@OptIn(ExperimentalMaterial3Api::class)
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
    val listState = rememberLazyListState()
    
    var isControlsVisible by remember { mutableStateOf(true) }
    var hasScrolledToInitial by remember { mutableStateOf(false) }

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

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(isControlsVisible) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isControlsVisible) {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context.findActivity()
            val window = activity?.window
            if (window != null) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = isControlsVisible,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentChapter?.title ?: "Reader",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground.copy(alpha = 0.9f))
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isControlsVisible) innerPadding.calculateTopPadding() else 0.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    isControlsVisible = !isControlsVisible
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
                            onReload = { viewModel.reloadChapter(it) }
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !isControlsVisible,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                val currentNum by viewModel.currentChapterNumber.collectAsStateWithLifecycle()
                val totalNum by viewModel.totalChapters.collectAsStateWithLifecycle()
                if (totalNum > 0 && currentNum > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
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
}

@Composable
fun ChapterContent(
    loadedChapter: LoadedChapter,
    onReload: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
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

        val isError = loadedChapter.paragraph.size == 1 && 
                loadedChapter.paragraph.first().contains("Couldn't load", ignoreCase = true)
        val isEmpty = loadedChapter.paragraph.isEmpty()

        if (isError || isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isError) loadedChapter.paragraph.first() else "No content found for this chapter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onReload(loadedChapter.chapter.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Reload Chapter", color = Color.White)
                    }
                }
            }
        } else {
            loadedChapter.paragraph.forEach { para ->
                Text(
                    text = para,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 34.sp,
                        letterSpacing = 0.5.sp,
                        fontSize = 19.sp
                    ),
                    color = PrimaryText.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 28.dp)
                )
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
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

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
