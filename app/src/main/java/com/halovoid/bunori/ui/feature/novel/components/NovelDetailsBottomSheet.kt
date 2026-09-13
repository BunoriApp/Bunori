package com.halovoid.bunori.ui.feature.novel.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.bunori.domain.models.Novel
import com.halovoid.bunori.ui.core.components.AppBottomSheet
import com.halovoid.bunori.ui.core.components.AppBottomSheetGroup
import com.halovoid.bunori.ui.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailsBottomSheet(
    novel: Novel,
    onDismiss: () -> Unit
) {
    val sources = novel.chapters.map { it.scanlationSource }.filter { it.isNotBlank() && it != "NotProvided" && it != "Not Provided" }.distinct()
    val sourceDisplay = if (sources.isNotEmpty()) sources.joinToString(", ") else novel.crawlerName

    AppBottomSheet(
        onDismiss = onDismiss,
        title = novel.title,
        subtitle = novel.author?.let { "By $it" }
    ) {
        AppBottomSheetGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetadataSection(
                    mapOf(
                        "Author" to (novel.author ?: "Unknown"),
                        "Chapters" to novel.chapters.size.toString(),
                        "Sources" to sourceDisplay,
                        "Alternative Names" to (novel.alternativeNames ?: "None")
                    )
                )

                novel.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Synopsis",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            lineHeight = 22.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
