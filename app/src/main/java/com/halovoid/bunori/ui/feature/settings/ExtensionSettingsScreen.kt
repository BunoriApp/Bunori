package com.halovoid.bunori.ui.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.ui.core.components.AppTopBar
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.crawler.RepoUrlDialog
import kotlinx.coroutines.launch

@Composable
fun ExtensionSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val extensionRepoUrl by viewModel.extensionRepoUrl.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showRepoDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.installExtensionFromUri(it) { _, message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    if (showRepoDialog) {
        RepoUrlDialog(
            initialUrl = extensionRepoUrl,
            onDismiss = { showRepoDialog = false },
            onSave = { newUrl ->
                viewModel.setExtensionRepoUrl(newUrl)
                showRepoDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Extensions",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(text = "Repositories")

            SettingsRow(
                title = "Repository URL",
                subtitle = if (extensionRepoUrl.isBlank()) "Default repository" else extensionRepoUrl,
                icon = Icons.Outlined.Link,
                onClick = { showRepoDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(text = "Local Extensions")

            SettingsRow(
                title = "Install from File",
                subtitle = "Load a local .bext extension package",
                icon = Icons.Outlined.FolderOpen,
                onClick = { filePickerLauncher.launch("*/*") }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
