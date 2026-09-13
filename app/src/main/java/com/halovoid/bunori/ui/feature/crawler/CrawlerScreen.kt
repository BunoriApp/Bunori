package com.halovoid.bunori.ui.feature.crawler

import android.content.Intent
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.bunori.api.core.crawler.Crawler
import com.halovoid.bunori.data.repository.DEFAULT_EXTENSION_REPO_URL
import com.halovoid.bunori.ui.core.components.SourceIcon
import com.halovoid.bunori.ui.core.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlerScreen(
    viewModel: CrawlerViewModel,
    onBack: () -> Unit = {},
    showHeader: Boolean = true,
    searchQuery: String = "",
    onNavigateToExtensionSettings: (() -> Unit)? = null,
    onNavigateToExtensionInfo: ((String) -> Unit)? = null
) {
    val extensionItems by viewModel.extensionItems.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItemForDetails by remember { mutableStateOf<ExtensionUiItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (selectedItemForDetails != null) {
        val detailItem = selectedItemForDetails!!
        ExtensionDetailDialog(
            item = detailItem,
            onDismiss = { selectedItemForDetails = null },
            onUninstall = {
                selectedItemForDetails = null
                viewModel.uninstallExtension(detailItem.id)
            }
        )
    }

    @Composable
    fun ExtensionListContent(modifier: Modifier = Modifier) {
        val filteredItems = remember(extensionItems, searchQuery) {
            if (searchQuery.isBlank()) {
                extensionItems
            } else {
                val q = searchQuery.trim().lowercase()
                extensionItems.filter {
                    it.name.lowercase().contains(q) ||
                    it.lang.lowercase().contains(q) ||
                    it.baseUrl.lowercase().contains(q)
                }
            }
        }

        val installing = remember(filteredItems) {
            filteredItems.filter { it.isActionInProgress }.sortedBy { it.name.lowercase() }
        }
        val updates = remember(filteredItems) {
            filteredItems.filter { it.hasUpdate && !it.isActionInProgress }.sortedBy { it.name.lowercase() }
        }
        val installed = remember(filteredItems) {
            filteredItems.filter { it.isInstalled && !it.hasUpdate && !it.isActionInProgress }.sortedBy { it.name.lowercase() }
        }
        val available = remember(filteredItems) {
            filteredItems.filter { !it.isInstalled && !it.hasUpdate && !it.isActionInProgress }.sortedBy { it.name.lowercase() }
        }

        // Group available extensions: "Multi" first if present, then alphabetical language groups
        val availableGroups = remember(available) {
            available.groupBy { item ->
                when (item.lang.lowercase()) {
                    "all", "multi" -> "Multi"
                    "en" -> "English"
                    else -> item.lang.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            }.toList().sortedWith(compareBy { (group, _) ->
                if (group == "Multi") "0" else "1_$group"
            })
        }

        Box(modifier = modifier.fillMaxSize()) {
            if (extensionItems.isEmpty() && catalogState is CatalogState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandAccent)
                }
            } else if (filteredItems.isEmpty()) {
                // Empty state without fetch failed errors - prompt to visit extension settings
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = DarkSurfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Extension,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp),
                                tint = SecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No results found" else "No extensions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty())
                                "Try searching with a different keyword"
                            else
                                "Add the source from the extension settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isEmpty() && onNavigateToExtensionSettings != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToExtensionSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Extension settings", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // 0. INSTALLING & UPDATING SECTION
                    if (installing.isNotEmpty()) {
                        item(key = "section_header_installing") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Installing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandAccent
                                )
                                Surface(
                                    color = BrandAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${installing.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        items(installing, key = { "installing_${it.id}" }) { item ->
                            ExtensionRow(
                                item = item,
                                onActionClick = {},
                                onItemClick = {}
                            )
                        }
                    }

                    // 1. UPDATES PENDING SECTION
                    if (updates.isNotEmpty()) {
                        item(key = "section_header_updates") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Updates pending",
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

                                TextButton(
                                    onClick = { viewModel.updateAll() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Update all",
                                        color = BrandAccent,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        items(updates, key = { "update_${it.id}" }) { item ->
                            ExtensionRow(
                                item = item,
                                onActionClick = { item.repoEntry?.let { viewModel.installExtension(it) } },
                                onItemClick = {
                                    if (onNavigateToExtensionInfo != null) {
                                        onNavigateToExtensionInfo(item.id)
                                    } else {
                                        selectedItemForDetails = item
                                    }
                                }
                            )
                        }
                    }

                    // 2. INSTALLED SECTION
                    if (installed.isNotEmpty()) {
                        item(key = "section_header_installed") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
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
                            ExtensionRow(
                                item = item,
                                onActionClick = {
                                    if (onNavigateToExtensionInfo != null) {
                                        onNavigateToExtensionInfo(item.id)
                                    } else {
                                        selectedItemForDetails = item
                                    }
                                },
                                onItemClick = {
                                    if (onNavigateToExtensionInfo != null) {
                                        onNavigateToExtensionInfo(item.id)
                                    } else {
                                        selectedItemForDetails = item
                                    }
                                }
                            )
                        }
                    }

                    // 3. MULTI & AVAILABLE LANGUAGE SECTIONS
                    if (availableGroups.isNotEmpty()) {
                        availableGroups.forEach { (groupTitle, groupItems) ->
                            item(key = "section_header_avail_$groupTitle") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = groupTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryText
                                    )
                                    Surface(
                                        color = DarkSurfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${groupItems.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            items(groupItems, key = { "avail_${it.id}" }) { item ->
                                ExtensionRow(
                                    item = item,
                                    onActionClick = { item.repoEntry?.let { viewModel.installExtension(it) } },
                                    onItemClick = { selectedItemForDetails = item }
                                )
                            }
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

                    if (onNavigateToExtensionSettings != null) {
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
                                    text = { Text("Extension settings", color = PrimaryText) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToExtensionSettings()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
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
    } else {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            ExtensionListContent(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun ExtensionRow(
    item: ExtensionUiItem,
    onActionClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isActionInProgress, onClick = onItemClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Extension icon: 42dp rounded with image loading and fallback
        SourceIcon(
            model = item.iconModel,
            fallbackText = item.name,
            size = 42.dp,
            shape = RoundedCornerShape(10.dp),
            contentPadding = 6.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Name and metadata
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val langText = if (item.lang.equals("all", ignoreCase = true)) "Multi" else item.lang.uppercase()
            val versionText = when {
                item.hasUpdate -> "v${item.installedVersion} → v${item.repoVersion}"
                item.installedVersion != null -> "v${item.installedVersion}"
                item.repoVersion != null -> "v${item.repoVersion}"
                else -> "v1.0.0"
            }
            val is18Plus = item.name.contains("18+") || item.baseUrl.contains("18+")
            val ageRatingText = if (is18Plus) " • 18+" else ""
            val statusPrefix = if (item.isActionInProgress) {
                if (item.isInstalled) "Updating • " else "Installing • "
            } else ""
            val metadata = "$statusPrefix$langText • $versionText$ageRatingText"

            Text(
                text = metadata,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isActionInProgress || item.hasUpdate) BrandAccent else SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Action icon / button
        if (item.isActionInProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = BrandAccent
            )
        } else if (item.hasUpdate) {
            Button(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Update",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (item.isInstalled) {
            IconButton(
                onClick = onActionClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Extension Settings",
                    tint = SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            IconButton(
                onClick = onActionClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Extension",
                    tint = SecondaryText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ExtensionDetailDialog(
    item: ExtensionUiItem,
    onDismiss: () -> Unit,
    onUninstall: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SourceIcon(
                    model = item.iconModel,
                    fallbackText = item.name,
                    size = 42.dp,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = 6.dp
                )
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    val ver = item.installedVersion ?: item.repoVersion ?: "1.0.0"
                    Text(
                        text = "v$ver • ${item.lang.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (item.baseUrl.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, item.baseUrl.toUri())
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Website",
                                style = MaterialTheme.typography.labelMedium,
                                color = SecondaryText
                            )
                            Text(
                                text = item.baseUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Website",
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (item.isInstalled) {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    Button(
                        onClick = {
                            onDismiss()
                            onUninstall()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.15f),
                            contentColor = ErrorRed
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uninstall", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryText)
            }
        }
    )
}

// Deprecated fallback preserved for backward compatibility
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
                    text = "Specify the repository URL (e.g. index.min.json).",
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
            TextButton(onClick = { onSave(urlText.trim()) }) {
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
