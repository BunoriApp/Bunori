package com.halovoid.bunori.ui.feature.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.domain.models.Chapter
import com.halovoid.bunori.ui.core.components.AppBottomSheet
import com.halovoid.bunori.ui.core.theme.*
import kotlinx.coroutines.launch

/**
 * A lightweight, unobtrusive Table of Contents: a scrollable chapter list,
 * current chapter highlighted, read chapters dimmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOfContentsSheet(
    chapters: List<Chapter>,
    currentChapterId: Int?,
    onChapterSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentChapterId, chapters) {
        val index = chapters.indexOfFirst { it.id == currentChapterId }
        if (index != -1) listState.scrollToItem((index - 3).coerceAtLeast(0))
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        title = "Contents"
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
        ) {
            items(chapters, key = { it.id }) { chapter ->
                val isCurrent = chapter.id == currentChapterId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onChapterSelected(chapter.id)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                        .background(if (isCurrent) PrimaryAccent.copy(alpha = 0.12f) else DarkBackground)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isCurrent -> PrimaryAccent
                            chapter.read -> SecondaryText
                            else -> PrimaryText
                        },
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (chapter.read && !isCurrent) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Read",
                            tint = SecondaryText.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
