package com.halovoid.bunori.ui.feature.novel.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.domain.models.Artifact
import com.halovoid.bunori.ui.core.theme.PrimaryText
import com.halovoid.bunori.ui.feature.novel.components.artifact.ArtifactCard

fun LazyListScope.novelArtifactsSection(
    artifacts: List<Artifact>,
    onDownload: (Artifact) -> Unit,
    onOpen: (Artifact) -> Unit
) {
    if (artifacts.isEmpty()) return

    item {
        Text(
            "Artifacts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp)
        )
    }
    
    items(artifacts, key = { it.id }) { artifact ->
        Box(modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp)) {
            ArtifactCard (
                artifact = artifact,
                onOpen = { onOpen(it) },
                onDownload = { onDownload(it) }
            )
        }
    }
    
    item { Spacer(modifier = Modifier.height(16.dp)) }
}
