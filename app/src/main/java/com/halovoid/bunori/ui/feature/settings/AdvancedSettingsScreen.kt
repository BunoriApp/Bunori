package com.halovoid.bunori.ui.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.ui.core.components.AppTopBar
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.crawler.RepoUrlDialog
import kotlinx.coroutines.launch

@Composable
fun AdvancedSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val appBeta by viewModel.betaModeApp.collectAsStateWithLifecycle()
    val crawlerBeta by viewModel.betaModeCrawlers.collectAsStateWithLifecycle()
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
                title = "Advanced Settings",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(text = "Extensions")

            SettingsRow(
                title = "Extension Repository URL",
                subtitle = if (extensionRepoUrl.isBlank()) "Default repository" else extensionRepoUrl,
                icon = Icons.Outlined.Link,
                onClick = { showRepoDialog = true }
            )

            SettingsRow(
                title = "Install Extension from File",
                subtitle = "Load a local .bext package from device storage",
                icon = Icons.Outlined.FolderOpen,
                onClick = { filePickerLauncher.launch("*/*") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(text = "Release Channels")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setBetaModeApp(!appBeta) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Beta App Releases",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Get notified of pre-release application builds (early features/unstable).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
                Switch(
                    checked = appBeta,
                    onCheckedChange = { viewModel.setBetaModeApp(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryText,
                        checkedTrackColor = BrandAccent,
                        uncheckedThumbColor = SecondaryText,
                        uncheckedTrackColor = DarkBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setBetaModeCrawlers(!crawlerBeta) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Beta Crawler Bundle",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Get notified of pre-release crawler parser scripts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
                Switch(
                    checked = crawlerBeta,
                    onCheckedChange = { viewModel.setBetaModeCrawlers(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryText,
                        checkedTrackColor = BrandAccent,
                        uncheckedThumbColor = SecondaryText,
                        uncheckedTrackColor = DarkBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
