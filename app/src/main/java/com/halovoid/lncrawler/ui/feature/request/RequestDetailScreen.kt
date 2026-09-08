package com.halovoid.lncrawler.ui.feature.request

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.core.components.AppTopBar
import com.halovoid.lncrawler.ui.core.components.SecurityCheckDialog
import com.halovoid.lncrawler.ui.core.platform.rememberFileExportLauncher
import com.halovoid.lncrawler.ui.core.theme.*
import com.halovoid.lncrawler.ui.feature.novel.components.artifact.ArtifactCard
import com.halovoid.lncrawler.ui.feature.request.components.RequestCard
import com.halovoid.lncrawler.ui.feature.request.components.requestHistorySection
import kotlinx.coroutines.launch

@Composable
fun RequestDetailScreen(
    requestId: String?,
    onBackClick: () -> Unit,
    onGroupClick: (RequestType) -> Unit,
    onRequestClick: (String) -> Unit
) {
    if (requestId == null) {
        onBackClick()
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val factory = remember { ViewModelFactory(context.applicationContext as android.app.Application) }
    val viewModel: RequestDetailViewModel = viewModel(factory = factory)

    val record by viewModel.getRequest(requestId).collectAsState(initial = null)
    val linkedRequests by viewModel.linkedRequests.collectAsStateWithLifecycle()
    val cancellingRequestIds by viewModel.cancellingRequestIds.collectAsStateWithLifecycle()
    val activeActionIds by viewModel.activeActionIds.collectAsStateWithLifecycle()
    val chapterMetadata by viewModel.chapterMetadata.collectAsState()
    val artifactMetadata by viewModel.artifactMetadata.collectAsState()

    var securityDialogRequest by remember { mutableStateOf<Request?>(null) }

    if (securityDialogRequest != null) {
        SecurityCheckDialog(
            novelName = securityDialogRequest!!.name,
            onConfirm = {
                val req = securityDialogRequest!!
                securityDialogRequest = null
                viewModel.resolveCloudflare(req.id, req.url ?: req.novelUrl)
            },
            onDismiss = { securityDialogRequest = null }
        )
    }

    val launchFileExport = rememberFileExportLauncher(mimeType = "*/*") { uri ->
        if (artifactMetadata != null) {
            viewModel.copyArtifactToUri(
                artifact = artifactMetadata!!,
                destinationUri = uri,
                onComplete = { resultUri ->
                    if (resultUri != null) {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Exported: ${artifactMetadata!!.artifactName}",
                                actionLabel = "OPEN",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                val mimeType = if (artifactMetadata!!.artifactName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "application/epub+zip"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(resultUri, mimeType)
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
                            message = "Original file not found. It may have been removed or deleted.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }

    LaunchedEffect(requestId) {
        viewModel.setRequestId(requestId)
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "Request Details",
                onBack = onBackClick
            )
        }
    ) { innerPadding ->
        if (record == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            val currentRecord = record!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    RequestCard(
                        request = currentRecord,
                        onClick = { /* Already here */ },
                        onReplay = { viewModel.replayRequest(currentRecord.id) },
                        onCancel = { viewModel.cancelRequest(currentRecord.id) },
                        onContinue = { viewModel.resumeRequest(currentRecord.id) },
                        onSecurityClick = { securityDialogRequest = currentRecord },
                        isCancelling = cancellingRequestIds.contains(currentRecord.id),
                        isActionPending = activeActionIds.contains(currentRecord.id)
                    )
                }

                if (currentRecord.type == RequestType.CHAPTER && chapterMetadata != null) {
                    item {
                        Column {
                            Text(
                                "Chapter Metadata",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryText,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            MetadataTable(
                                mapOf(
                                    "Title" to chapterMetadata!!.title,
                                    "Serial" to chapterMetadata!!.index.toString(),
                                    "URL" to chapterMetadata!!.url,
                                    "Novel URL" to chapterMetadata!!.novelUrl
                                )
                            )
                        }
                    }
                }

                if (currentRecord.type == RequestType.ARTIFACT && artifactMetadata != null) {
                    item {
                        ArtifactCard(
                            artifact = artifactMetadata!!,
                            onOpen = {
                                val mimeType = if (it.artifactName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "application/epub+zip"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(it.artifactDestination.toUri(), mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Open with"))
                                } catch (e: Exception) {
                                    scope.launch {
                                        val docType = if (it.artifactName.endsWith(".pdf", ignoreCase = true)) "PDF" else "EPUB"
                                        snackbarHostState.showSnackbar("No app found to open $docType")
                                    }
                                }
                            },
                            onDownload = {
                                launchFileExport(it.artifactName)
                            }
                        )
                    }
                }

                if (linkedRequests.isNotEmpty()) {
                    item {
                        Text(
                            "Linked Requests",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    requestHistorySection(
                        requestHistory = linkedRequests,
                        onRequestClick = onRequestClick,
                        onGroupClick = onGroupClick,
                        onReplay = { viewModel.replayRequest(it) },
                        onCancel = { viewModel.cancelRequest(it) },
                        onContinue = { viewModel.resumeRequest(it) },
                        onSecurityClick = { securityDialogRequest = it },
                        cancellingRequestIds = cancellingRequestIds,
                        activeActionIds = activeActionIds,
                        allowAction = true
                    )
                }
            }
        }
    }
}

@Composable
fun MetadataTable(data: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(8.dp)
    ) {
        data.forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = key, color = SecondaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(text = value, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
            }
            if (key != data.keys.last()) {
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }
    }
}
