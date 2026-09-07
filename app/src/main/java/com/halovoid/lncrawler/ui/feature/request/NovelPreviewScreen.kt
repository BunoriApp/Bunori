package com.halovoid.lncrawler.ui.feature.request

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.core.components.AppBottomSheet
import com.halovoid.lncrawler.ui.core.theme.*
import com.halovoid.lncrawler.ui.feature.novel.components.NovelSynopsisSection

@Composable
fun NovelPreviewScreen(
    viewModel: RequestViewModel,
    onBack: () -> Unit,
    onConfirm: (Novel) -> Unit,
    onCrawlManually: (String, String, String) -> Unit
) {
    val novel by viewModel.novelPreview.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val previewUrl by viewModel.previewUrl.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val similarNovels by viewModel.similarNovels.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.addSuccess.collect {
            onBack()
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
        error = error,
        onBack = onBack,
        onConfirm = onConfirm,
        onCrawlManually = {
            if (previewUrl != null) {
                onCrawlManually(novel?.crawlerName ?: "", previewUrl!!, novel?.title ?: previewUrl!!)
            }
        }
    )

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
            modifier = Modifier.fillMaxWidth().height(56.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelPreviewContent(
    novel: Novel?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConfirm: (Novel) -> Unit,
    onCrawlManually: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                },
                actions = {
                    if (novel != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, novel.url.toUri())
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Language, contentDescription = "View Source", tint = PrimaryText)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && novel == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    CircularProgressIndicator(color = BrandAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching novel details...", color = SecondaryText)
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(onClick = onBack) {
                        Text("Cancel", color = SecondaryText)
                    }
                }
            } else if (novel != null) {
                var isSynopsisExpanded by remember { mutableStateOf(false) }
                var coverModel by remember(novel.coverUrl, novel.coverHttpsUrl) {
                    mutableStateOf(novel.coverUrl ?: novel.coverHttpsUrl)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = BrandAccent,
                            trackColor = Color.Transparent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.12f),
                            contentScale = ContentScale.Crop,
                            onError = {
                                if (coverModel != novel.coverHttpsUrl) {
                                    coverModel = novel.coverHttpsUrl
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            DarkBackground.copy(alpha = 0.7f),
                                            DarkBackground
                                        )
                                    )
                                )
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(top = 96.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            AsyncImage(
                                model = coverModel,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(170.dp)
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

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = novel.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = novel.author ?: "Unknown Author",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SecondaryText,
                                textAlign = TextAlign.Center
                            )

                            if (novel.crawlerName.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.padding(top = 12.dp),
                                    color = DarkSurfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = novel.crawlerName.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        if (error != null) {
                             Surface(
                                 modifier = Modifier
                                     .padding(horizontal = 24.dp)
                                     .padding(bottom = 24.dp),
                                 color = ErrorRed.copy(alpha = 0.1f),
                                 shape = RoundedCornerShape(8.dp),
                                 border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                             ) {
                                 Text(
                                     text = error,
                                     modifier = Modifier.padding(12.dp),
                                     color = ErrorRed,
                                     style = MaterialTheme.typography.bodySmall
                                 )
                             }
                        }

                        NovelSynopsisSection(
                            novel = novel,
                            isExpanded = isSynopsisExpanded,
                            onExpandClick = { isSynopsisExpanded = !isSynopsisExpanded }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onConfirm(novel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add to Library",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                "Maybe Later",
                                color = SecondaryText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    if (error != null) {
                        Text(error, color = ErrorRed, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    Button(
                        onClick = onCrawlManually,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurface,
                            contentColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Crawl Manually",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
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
    }
}
