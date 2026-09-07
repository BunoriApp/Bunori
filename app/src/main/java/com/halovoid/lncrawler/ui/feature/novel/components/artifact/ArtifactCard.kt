package com.halovoid.lncrawler.ui.feature.novel.components.artifact

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.ui.core.theme.*

@Composable
fun ArtifactCard(
    artifact: Artifact,
    onOpen: (Artifact) -> Unit,
    onDownload: (Artifact) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(artifact) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = DarkSurfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artifact.artifactName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "EPUB document",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }

        IconButton(onClick = { onDownload(artifact) }) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                tint = SecondaryText.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
