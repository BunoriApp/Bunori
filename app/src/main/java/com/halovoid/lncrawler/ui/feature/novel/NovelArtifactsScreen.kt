package com.halovoid.lncrawler.ui.feature.novel

import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.core.components.ExportWarningDialog
import com.halovoid.lncrawler.ui.core.theme.*
import com.halovoid.lncrawler.ui.feature.novel.components.artifact.ArtifactCard
import com.halovoid.lncrawler.ui.feature.novel.components.artifact.ArtifactExportDialog
import com.halovoid.lncrawler.ui.feature.novel.components.artifact.ExportFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelArtifactsScreen(
    novel: Novel?,
    artifacts: List<Artifact>,
    onBack: () -> Unit,
    onDownload: (Artifact) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val factory = remember { ViewModelFactory(context.applicationContext as Application) }
    val viewModel: NovelDetailViewModel = viewModel(factory = factory)

    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val chapterRange by viewModel.chapterRange.collectAsStateWithLifecycle()

    var selectedArtifact by remember { mutableStateOf<Artifact?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportWarning by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let { destUri ->
            selectedArtifact?.let { artifact ->
                viewModel.copyArtifactToUri(
                    artifact = artifact,
                    destinationUri = destUri,
                    onComplete = { resultUri ->
                        if (resultUri != null) {
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Exported: ${artifact.artifactName}",
                                    actionLabel = "OPEN",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(resultUri, "application/epub+zip")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with"))
                                }
                            }
                        }
                    },
                    onFileMissing = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Original file not found.",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artifacts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Artifact")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = PrimaryText,
                    navigationIconContentColor = PrimaryText,
                    actionIconContentColor = PrimaryText
                )
            )
        },
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (artifacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = SecondaryText.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No artifacts yet", style = MaterialTheme.typography.titleMedium, color = PrimaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generated EPUB files will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
                    ) {
                        Text("Create Artifact")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(artifacts, key = { it.id }) { artifact ->
                    ArtifactCard(
                        artifact = artifact,
                        onOpen = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(it.artifactDestination.toUri(), "application/epub+zip")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No app found to open EPUB")
                                }
                            }
                        },
                        onDownload = {
                            selectedArtifact = artifact
                            exportLauncher.launch(artifact.artifactName)
                            onDownload(artifact)
                        }
                    )
                }
            }
        }

        if (showExportDialog) {
            val start = chapterRange.start.toInt()
            val end = chapterRange.endInclusive.toInt()
            val rangeChapters = chapters.filter { it.index in start..end }
            val downloadedCount = rangeChapters.count { it.fileLocation?.startsWith("content://") == true }

            ArtifactExportDialog(
                onDismiss = { showExportDialog = false },
                onExport = { format ->
                    showExportDialog = false
                    if (downloadedCount < rangeChapters.size) {
                        pendingExportFormat = format
                        showExportWarning = true
                    } else {
                        novel?.let { viewModel.startBackgroundExport(it, format) }
                        onBack()
                    }
                }
            )
        }

        if (showExportWarning && pendingExportFormat != null) {
            val start = chapterRange.start.toInt()
            val end = chapterRange.endInclusive.toInt()
            val rangeChapters = chapters.filter { it.index in start..end }
            val downloadedCount = rangeChapters.count { it.fileLocation?.startsWith("content://") == true }

            ExportWarningDialog(
                totalSelected = rangeChapters.size,
                downloadedCount = downloadedCount,
                onDownloadFirst = {
                    showExportWarning = false
                    novel?.let { viewModel.fetchRange(it) }
                },
                onExportAnyway = {
                    showExportWarning = false
                    novel?.let { viewModel.startBackgroundExport(it, pendingExportFormat!!) }
                    onBack()
                },
                onDismiss = {
                    showExportWarning = false
                }
            )
        }
    }
}
