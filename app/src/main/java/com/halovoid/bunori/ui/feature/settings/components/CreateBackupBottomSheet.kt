package com.halovoid.bunori.ui.feature.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.ui.core.components.AppBottomSheet
import com.halovoid.bunori.ui.core.components.AppBottomSheetDivider
import com.halovoid.bunori.ui.core.components.AppBottomSheetGroup
import com.halovoid.bunori.ui.core.theme.BrandAccent
import com.halovoid.bunori.ui.core.theme.PrimaryText
import com.halovoid.bunori.ui.core.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBackupBottomSheet(
    initialDatabase: Boolean = true,
    initialChapters: Boolean = true,
    initialCovers: Boolean = true,
    initialArtifacts: Boolean = false,
    onDismiss: () -> Unit,
    onCreateBackup: (backupDatabase: Boolean, backupChapters: Boolean, backupCovers: Boolean, backupArtifacts: Boolean) -> Unit
) {
    var backupDatabase by remember { mutableStateOf(initialDatabase) }
    var backupChapters by remember { mutableStateOf(initialChapters) }
    var backupCovers by remember { mutableStateOf(initialCovers) }
    var backupArtifacts by remember { mutableStateOf(initialArtifacts) }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Create Backup",
        subtitle = "Select what to include in the backup archive."
    ) {
        AppBottomSheetGroup {
            ListItem(
                headlineContent = { Text("Database", color = PrimaryText) },
                supportingContent = { Text("Library metadata, downloads, reading progress, settings", color = SecondaryText) },
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
                onCreateBackup(backupDatabase, backupChapters, backupCovers, backupArtifacts)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Create Backup",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
