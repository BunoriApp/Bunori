package com.halovoid.bunori.ui.feature.crawler

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.api.core.crawler.Crawler
import com.halovoid.bunori.data.repository.DEFAULT_EXTENSION_REPO_URL
import com.halovoid.bunori.ui.core.components.MutedEmptyState
import com.halovoid.bunori.ui.core.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlerScreen(
    viewModel: CrawlerViewModel,
    onBack: () -> Unit = {},
    showHeader: Boolean = true
) {
    val extensionItems by viewModel.extensionItems.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val updatesCount by viewModel.updatesCount.collectAsStateWithLifecycle()
    val currentRepoUrl by viewModel.repoUrl.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRepoDialog by remember { mutableStateOf(false) }

    // File picker for sideloading .bext packages
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.installFromUri(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showRepoDialog) {
        RepoUrlDialog(
            initialUrl = currentRepoUrl,
            onDismiss = { showRepoDialog = false },
            onSave = { newUrl ->
                viewModel.setRepoUrl(newUrl)
                showRepoDialog = false
            }
        )
    }

    @Composable
    fun ExtensionListContent(modifier: Modifier = Modifier) {
        val updates = remember(extensionItems) {
            extensionItems.filter { it.hasUpdate }.sortedBy { it.name.lowercase() }
        }
        val installed = remember(extensionItems) {
            extensionItems.filter { it.isInstalled && !it.hasUpdate }.sortedBy { it.name.lowercase() }
        }
        val available = remember(extensionItems) {
            extensionItems.filter { !it.isInstalled }.sortedBy { it.name.lowercase() }
        }

        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Error banner for catalog fetching
            if (catalogState is CatalogState.Error) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    color = ErrorRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (catalogState as CatalogState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.refreshCatalog() }) {
                            Text("Retry", color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (extensionItems.isEmpty() && catalogState !is CatalogState.Loading) {
                MutedEmptyState(
                    title = "No Extensions Found",
                    description = "No extensions found in the repository. Check repository settings in Advanced Settings or pull to refresh.",
                    icon = Icons.Default.Extension,
                    modifier = Modifier.weight(1f)
                )
            } else if (extensionItems.isEmpty() && catalogState is CatalogState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandAccent)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. UPDATES SECTION (Shown only if updates are available)
                    if (updates.isNotEmpty()) {
                        item(key = "section_header_updates") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Updates",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandAccent
                                    )
                                    Surface(
                                        color = BrandAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${updates.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (updates.size > 1) {
                                    TextButton(
                                        onClick = { viewModel.updateAll() },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Update All",
                                            color = BrandAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        items(updates, key = { "update_${it.id}" }) { item ->
                            ExtensionItemCard(
                                item = item,
                                onInstall = { item.repoEntry?.let { viewModel.installExtension(it) } },
                                onUpdate = { item.repoEntry?.let { viewModel.installExtension(it) } },
                                onUninstall = { viewModel.uninstallExtension(item.id) }
                            )
                        }

                        item(key = "spacer_updates") {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // 2. INSTALLED SECTION (Shown only for installed items with NO updates)
                    if (installed.isNotEmpty()) {
                        item(key = "section_header_installed") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Installed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Surface(
                                    color = DarkSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${installed.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        items(installed, key = { "installed_${it.id}" }) { item ->
                            ExtensionItemCard(
                                item = item,
                                onInstall = { item.repoEntry?.let { viewModel.installExtension(it) } },
                                onUpdate = {},
                                onUninstall = { viewModel.uninstallExtension(item.id) }
                            )
                        }

                        item(key = "spacer_installed") {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // 3. AVAILABLE SECTION (All remaining downloadable extensions)
                    if (available.isNotEmpty()) {
                        item(key = "section_header_available") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Available",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryText
                                )
                                Surface(
                                    color = DarkSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${available.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        items(available, key = { "available_${it.id}" }) { item ->
                            ExtensionItemCard(
                                item = item,
                                onInstall = { item.repoEntry?.let { viewModel.installExtension(it) } },
                                onUpdate = {},
                                onUninstall = {}
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHeader) {
        Scaffold(
            containerColor = DarkBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                var showMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryText
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Extensions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = "${extensionItems.count { it.isInstalled }} installed • ${extensionItems.size} total",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                    }

                    // Refresh catalog
                    if (catalogState is CatalogState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(20.dp),
                            color = BrandAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshCatalog() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Catalog",
                                tint = BrandAccent
                            )
                        }
                    }

                    // Overflow menu for advanced extension options
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = SecondaryText
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Install from File (.bext)", color = PrimaryText) },
                                onClick = {
                                    showMenu = false
                                    filePickerLauncher.launch("*/*")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = BrandAccent
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Repository URL", color = PrimaryText) },
                                onClick = {
                                    showMenu = false
                                    showRepoDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = BrandAccent
                                    )
                                }
                            )
                        }
                    }
                }

                ExtensionListContent()
            }
        }
    } else {
        // Embedded within RequestScreen (Browse tab)
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                var showMenu by remember { mutableStateOf(false) }

                // Secondary action bar for embedded tab
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${extensionItems.count { it.isInstalled }} installed",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (catalogState is CatalogState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(16.dp),
                                color = BrandAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = { viewModel.refreshCatalog() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = BrandAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Install from File (.bext)", color = PrimaryText) },
                                    onClick = {
                                        showMenu = false
                                        filePickerLauncher.launch("*/*")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = BrandAccent
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Repository URL", color = PrimaryText) },
                                    onClick = {
                                        showMenu = false
                                        showRepoDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = BrandAccent
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                ExtensionListContent()
            }
        }
    }
}

@Composable
fun ExtensionItemCard(
    item: ExtensionUiItem,
    onInstall: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.hasUpdate) BrandAccent.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(42.dp),
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = if (item.isInstalled) BrandAccent else SecondaryText.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Language Chip
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.lang.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val versionText = when {
                        item.hasUpdate -> "v${item.installedVersion} → v${item.repoVersion}"
                        item.installedVersion != null -> "v${item.installedVersion}"
                        item.repoVersion != null -> "v${item.repoVersion}"
                        else -> "v1"
                    }

                    Text(
                        text = versionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.hasUpdate) BrandAccent else SecondaryText
                    )

                    if (item.baseUrl.isNotEmpty()) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        Text(
                            text = item.baseUrl.removePrefix("https://").removePrefix("http://"),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            if (item.isActionInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = BrandAccent,
                    strokeWidth = 2.dp
                )
            } else if (item.hasUpdate) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUninstall,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Uninstall",
                            tint = SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Button(
                        onClick = onUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Update", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else if (item.isInstalled) {
                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Uninstall",
                        tint = SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = BrandAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RepoUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var urlText by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extension Repository URL") },
        text = {
            Column {
                Text(
                    text = "Specify the raw URL to the repository index.json.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    singleLine = true,
                    label = { Text("Repository Index URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { urlText = DEFAULT_EXTENSION_REPO_URL },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Reset to Default", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(urlText.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Backward-compatible fallback for legacy references
@Composable
fun CrawlerItem(crawler: Crawler) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = DarkSurfaceVariant,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = SecondaryText.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = crawler.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                Text(
                    text = crawler.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, crawler.baseUrl.toUri())
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Website",
                    tint = SecondaryText
                )
            }
        }
    }
}
