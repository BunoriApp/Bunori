package com.halovoid.lncrawler.ui.feature.novel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.core.theme.*

@Composable
fun NovelMetadataTable(novel: Novel, onClick: () -> Unit = {}) {
    val sources = novel.chapters.map { it.scanlationSource }.filter { it.isNotBlank() && it != "NotProvided" }.distinct()
    val sourceDisplay = if (sources.isNotEmpty()) sources.joinToString(", ") else novel.crawlerName

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        MetadataSection(
            mapOf(
                "Author" to (novel.author ?: "Unknown"),
                "Volumes" to novel.volumes.size.toString(),
                "Chapters" to novel.chapters.size.toString(),
                "Sources" to sourceDisplay
            )
        )
    }
}

@Composable
fun MetadataSection(data: Map<String, String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEach { (key, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = key, 
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryText, 
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
