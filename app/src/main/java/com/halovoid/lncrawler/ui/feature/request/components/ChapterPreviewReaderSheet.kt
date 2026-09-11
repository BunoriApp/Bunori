package com.halovoid.lncrawler.ui.feature.request.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.data.parser.HtmlDocumentParser
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.ui.core.components.AppBottomSheet
import com.halovoid.lncrawler.ui.core.theme.*
import com.halovoid.lncrawler.ui.feature.reader.BlockItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterPreviewReaderSheet(
    chapter: Chapter,
    isLoading: Boolean,
    contentHtml: String?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val parser = remember(chapter.url) { HtmlDocumentParser(baseUrl = chapter.url) }
    val document = remember(contentHtml, chapter.id) {
        if (!contentHtml.isNullOrBlank()) {
            parser.parse(contentHtml, chapter.id)
        } else {
            null
        }
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        title = "Chapter ${chapter.index}",
        subtitle = chapter.title.takeIf { it.isNotBlank() && it != "Chapter ${chapter.index}" }
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BrandAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading chapter content...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            }
        } else if (document != null && document.blocks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(document.blocks, key = { it.id }) { block ->
                    BlockItem(
                        block = block,
                        isSelected = false,
                        selectionModeActive = false,
                        onToggleSelect = {},
                        onBackgroundTap = {},
                        onReloadChapter = {},
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No content available for this preview.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }
    }
}
