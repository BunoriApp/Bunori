package com.halovoid.lncrawler.ui.feature.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.halovoid.lncrawler.domain.models.Block
import com.halovoid.lncrawler.domain.models.InlineSpan
import com.halovoid.lncrawler.ui.core.theme.*

/**
 * Renders a single Block with selection support:
 * - Long press: toggles this block's selection (enters selection mode).
 * - Short tap: if selection mode is already active, toggles this block;
 *   otherwise forwards to [onBackgroundTap] so the original "tap anywhere
 *   to hide/show controls" behavior is preserved.
 *
 * Requires: implementation("io.coil-kt:coil-compose:2.6.0")
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlockItem(
    block: Block,
    isSelected: Boolean,
    selectionModeActive: Boolean,
    onToggleSelect: (String) -> Unit,
    onBackgroundTap: () -> Unit,
    onReloadChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectableModifier = modifier
        .fillMaxWidth()
        .then(
            if (isSelected) Modifier.background(PrimaryAccent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            else Modifier
        )
        .combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onLongClick = { onToggleSelect(block.id) },
            onClick = { if (selectionModeActive) onToggleSelect(block.id) else onBackgroundTap() }
        )

    when (block) {
        is Block.Paragraph -> Text(
            text = block.spans.toAnnotatedString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 34.sp,
                letterSpacing = 0.5.sp,
                fontSize = 19.sp
            ),
            color = PrimaryText.copy(alpha = 0.9f),
            modifier = selectableModifier.padding(vertical = 2.dp).padding(bottom = 24.dp)
        )

        is Block.Heading -> Text(
            text = block.spans.toAnnotatedString(),
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            modifier = selectableModifier.padding(top = 8.dp, bottom = 16.dp)
        )

        is Block.Quote -> Column(
            modifier = selectableModifier.padding(start = 12.dp, bottom = 16.dp)
        ) {
            block.blocks.forEach { child ->
                BlockItem(
                    block = child,
                    isSelected = isSelected,
                    selectionModeActive = selectionModeActive,
                    onToggleSelect = onToggleSelect,
                    onBackgroundTap = onBackgroundTap,
                    onReloadChapter = onReloadChapter
                )
            }
        }

        is Block.ListBlock -> Column(modifier = selectableModifier.padding(bottom = 16.dp)) {
            block.items.forEachIndexed { index, item ->
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = if (block.ordered) "${index + 1}." else "\u2022",
                        color = PrimaryAccent.copy(alpha = 0.8f),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = item.spans.toAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp),
                        color = PrimaryText.copy(alpha = 0.9f)
                    )
                }
            }
        }

        is Block.ImageBlock -> ReaderImage(
            block = block,
            modifier = selectableModifier.padding(bottom = 24.dp)
        )

        is Block.Divider -> HorizontalDivider(
            modifier = selectableModifier.padding(vertical = 24.dp),
            color = SecondaryText.copy(alpha = 0.2f)
        )

        is Block.ErrorPlaceholder -> Box(
            modifier = selectableModifier.padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = block.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onReloadChapter,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Reload Chapter")
                }
            }
        }

        is Block.Unsupported -> Text(
            text = block.rawText,
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText.copy(alpha = 0.6f),
            modifier = selectableModifier.padding(bottom = 16.dp)
        )
    }
}

/**
 * Images load via Coil: relative/API URLs are already resolved to absolute
 * URLs by the parser, aspect ratio is preserved when width/height are known
 * so surrounding text doesn't jump as the image loads, and load failures
 * show a small text placeholder instead of breaking the chapter.
 */
@Composable
private fun ReaderImage(block: Block.ImageBlock, modifier: Modifier = Modifier) {
    var failed by remember(block.src) { mutableStateOf(false) }
    val aspectRatio = if (block.width != null && block.height != null && block.height > 0) {
        block.width.toFloat() / block.height.toFloat()
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (aspectRatio != null) Modifier.aspectRatio(aspectRatio) else Modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(SecondaryText.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (failed) {
            Text(
                text = block.alt ?: "Image unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(block.src)
                    .crossfade(true)
                    .build(),
                contentDescription = block.alt,
                modifier = Modifier.fillMaxWidth(),
                onError = { failed = true }
            )
        }
    }
}

@Composable
private fun List<InlineSpan>.toAnnotatedString(): AnnotatedString {
    val linkColor = PrimaryAccent
    val builder = AnnotatedString.Builder()
    fun append(spans: List<InlineSpan>) {
        spans.forEach { span ->
            when (span) {
                is InlineSpan.Text -> builder.append(span.text)
                is InlineSpan.LineBreak -> builder.append("\n")
                is InlineSpan.Bold -> {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(span.children); builder.pop()
                }
                is InlineSpan.Italic -> {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(span.children); builder.pop()
                }
                is InlineSpan.Underline -> {
                    builder.pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(span.children); builder.pop()
                }
                is InlineSpan.Strikethrough -> {
                    builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(span.children); builder.pop()
                }
                is InlineSpan.Link -> {
                    builder.pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                    append(span.children); builder.pop()
                }
            }
        }
    }
    append(this)
    return builder.toAnnotatedString()
}