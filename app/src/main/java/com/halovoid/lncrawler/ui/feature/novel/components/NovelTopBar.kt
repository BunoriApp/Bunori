package com.halovoid.lncrawler.ui.feature.novel.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.core.components.AppBottomSheet
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetDivider
import com.halovoid.lncrawler.ui.core.components.AppBottomSheetGroup
import com.halovoid.lncrawler.ui.core.theme.*

@Composable
fun NovelTopBar(
    novel: Novel,
    isOpaque: Boolean,
    showTitle: Boolean,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onClearSelection: () -> Unit,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit,
    onFilterClick: () -> Unit,
    isFilterActive: Boolean,
    onRefreshMetadata: () -> Unit,
    onDeleteNovel: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelectionMode || isOpaque) DarkBackground else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "TopBarBackgroundAnimation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        tonalElevation = if (isSelectionMode) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear Selection",
                        tint = PrimaryText
                    )
                }

                Text(
                    text = "$selectedCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(onClick = onMarkAsRead) {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = "Mark Completed",
                        tint = PrimaryText
                    )
                }

                IconButton(onClick = onMarkAsUnread) {
                    Icon(
                        Icons.Default.RemoveDone,
                        contentDescription = "Mark as Unread",
                        tint = PrimaryText
                    )
                }

                IconButton(onClick = onSelectAll) {
                    Icon(
                        Icons.Default.SelectAll,
                        contentDescription = "Select All",
                        tint = PrimaryText
                    )
                }

                IconButton(onClick = onUnselectAll) {
                    Icon(
                        Icons.Default.Deselect,
                        contentDescription = "Unselect All",
                        tint = PrimaryText
                    )
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryText
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showTitle,
                        enter = fadeIn(animationSpec = tween(durationMillis = 250)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 200))
                    ) {
                        Text(
                            text = novel.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onFilterClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isFilterActive) BrandAccent else PrimaryText
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter and Sort"
                    )
                }

                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = PrimaryText)
                }

                if (showMenu) {
                    NovelActionsBottomSheet(
                        novel = novel,
                        onDismiss = { showMenu = false },
                        onRefreshMetadata = {
                            onRefreshMetadata()
                            showMenu = false
                        },
                        onDeleteNovel = {
                            onDeleteNovel()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelActionsBottomSheet(
    novel: Novel,
    onDismiss: () -> Unit,
    onRefreshMetadata: () -> Unit,
    onDeleteNovel: () -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Novel Options",
        subtitle = novel.title
    ) {
        AppBottomSheetGroup {
            ListItem(
                headlineContent = { Text("Refresh Metadata", color = PrimaryText) },
                leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryText) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onRefreshMetadata() }
            )
            AppBottomSheetDivider()
            ListItem(
                headlineContent = { Text("Delete Novel", color = ErrorRed) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onDeleteNovel() }
            )
        }
    }
}
