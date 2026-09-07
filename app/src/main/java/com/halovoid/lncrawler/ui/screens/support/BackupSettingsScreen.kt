package com.halovoid.lncrawler.ui.screens.support

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.api.backup.BackupService
import com.halovoid.lncrawler.api.backup.RestoreService
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import com.halovoid.lncrawler.ui.components.AppBottomSheet
import com.halovoid.lncrawler.ui.components.AppBottomSheetDivider
import com.halovoid.lncrawler.ui.components.AppBottomSheetGroup
import com.halovoid.lncrawler.ui.theme.*
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject

data class BackupMetadata(
    val lastBackupTime: String,
    val fileSize: String,
    val contentsSummary: String,
    val count: Int
)

fun getLatestBackupMetadata(context: Context): BackupMetadata {
    val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backup")
    val files = backupDir.listFiles { _, name -> name.endsWith(".lnbak") }?.sortedByDescending { it.lastModified() }
    if (files.isNullOrEmpty()) {
        return BackupMetadata("No backup created yet", "N/A", "", 0)
    }
    val latest = files.first()
    val timeStr = android.text.format.DateFormat.format("MMM dd, yyyy, h:mm a", latest.lastModified()).toString()
    val sizeMb = latest.length() / (1024 * 1024.toFloat())
    val sizeStr = if (sizeMb < 1) "${latest.length() / 1024} KB" else String.format("%.1f MB", sizeMb)
    
    var summary = ""
    try {
        ZipFile(latest).use { zip ->
            val entry = zip.getEntry("manifest.json")
            if (entry != null) {
                val text = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val contents = json.optJSONObject("contents")
                if (contents != null) {
                    val included = mutableListOf<String>()
                    if (contents.optBoolean("database", false)) included.add("Database")
                    if (contents.optBoolean("chapters", false)) included.add("Chapters")
                    if (contents.optBoolean("covers", false)) included.add("Covers")
                    if (contents.optBoolean("artifacts", false)) included.add("Artifacts")
                    summary = included.joinToString(" · ")
                }
            }
        }
    } catch (_: Exception) {}

    return BackupMetadata(timeStr, sizeStr, summary, files.size)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportUri by PreferenceRepository.getInstance(context).exportFolderUri.collectAsStateWithLifecycle(initialValue = null)
    val storageLocation = exportUri?.toString() ?: File(context.getExternalFilesDir(null) ?: context.filesDir, "backup").absolutePath

    var metadata by remember { mutableStateOf(getLatestBackupMetadata(context)) }

    var showCreateBottomSheet by remember { mutableStateOf(false) }
    var backupDatabase by remember { mutableStateOf(true) }
    var backupChapters by remember { mutableStateOf(true) }
    var backupCovers by remember { mutableStateOf(true) }
    var backupArtifacts by remember { mutableStateOf(false) }

    var backupFrequency by remember { mutableStateOf("Off") }
    var showFrequencyMenu by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val success = RestoreService(context).restoreBackup(uri)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (success) "Backup restored successfully! Please restart the app" else "Failed to restore backup."
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Actions Section
            SectionHeader(text = "Backup Actions")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateBottomSheet = true }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Backup,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Create Backup",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Create a backup of your library and downloaded content",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { restoreLauncher.launch(arrayOf("*/*")) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Restore Backup",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Restore your library from an existing backup",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Backup Status Section
            SectionHeader(text = "Backup Status")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(text = "Last Backup", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = PrimaryText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = metadata.lastBackupTime, style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                    if (metadata.contentsSummary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = metadata.contentsSummary, style = MaterialTheme.typography.bodySmall, color = BrandAccent)
                    }
                }

                Column {
                    Text(text = "Backup Size", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = PrimaryText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = metadata.fileSize, style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                }

                Column {
                    Text(text = "Retained Backups", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = PrimaryText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${metadata.count} of 3 rolling backups stored", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 3. Automatic Backup Section
            SectionHeader(text = "Automatic Backup")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFrequencyMenu = true }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Backup Frequency",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Keep up to 3 rolling backups",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
                Text(
                    text = backupFrequency,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandAccent,
                    fontWeight = FontWeight.Bold
                )

                DropdownMenu(
                    expanded = showFrequencyMenu,
                    onDismissRequest = { showFrequencyMenu = false }
                ) {
                    listOf("Off", "Daily", "Weekly", "Monthly").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                backupFrequency = option
                                showFrequencyMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 4. Storage Section
            SectionHeader(text = "Storage")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Backup Location",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = storageLocation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Bottom Modal Sheet for Create Backup Options
        if (showCreateBottomSheet) {
            AppBottomSheet(
                onDismiss = { showCreateBottomSheet = false },
                title = "Create Backup",
                subtitle = "Select what to include in the backup archive."
            ) {
                AppBottomSheetGroup {
                    ListItem(
                        headlineContent = { Text("Database", color = PrimaryText) },
                        supportingContent = { Text("Library metadata, reading progress, settings", color = SecondaryText) },
                        trailingContent = { Checkbox(checked = backupDatabase, onCheckedChange = { backupDatabase = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { backupDatabase = !backupDatabase }
                    )
                    AppBottomSheetDivider()
                    ListItem(
                        headlineContent = { Text("Chapters / Novels", color = PrimaryText) },
                        supportingContent = { Text("Downloaded novel/chapter content", color = SecondaryText) },
                        trailingContent = { Checkbox(checked = backupChapters, onCheckedChange = { backupChapters = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { backupChapters = !backupChapters }
                    )
                    AppBottomSheetDivider()
                    ListItem(
                        headlineContent = { Text("Covers", color = PrimaryText) },
                        supportingContent = { Text("Downloaded cover images", color = SecondaryText) },
                        trailingContent = { Checkbox(checked = backupCovers, onCheckedChange = { backupCovers = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { backupCovers = !backupCovers }
                    )
                    AppBottomSheetDivider()
                    ListItem(
                        headlineContent = { Text("Artifacts (EPUB/PDF)", color = PrimaryText) },
                        supportingContent = { Text("Generated/downloaded EPUB and PDF artifacts", color = SecondaryText) },
                        trailingContent = { Checkbox(checked = backupArtifacts, onCheckedChange = { backupArtifacts = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { backupArtifacts = !backupArtifacts }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        showCreateBottomSheet = false
                        scope.launch(Dispatchers.IO) {
                            BackupService(context).createBackup(
                                backupDatabase = backupDatabase,
                                backupChapters = backupChapters,
                                backupCovers = backupCovers,
                                backupArtifacts = backupArtifacts
                            )
                            metadata = getLatestBackupMetadata(context)
                            scope.launch {
                                snackbarHostState.showSnackbar("Backup created successfully!")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create Backup", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
