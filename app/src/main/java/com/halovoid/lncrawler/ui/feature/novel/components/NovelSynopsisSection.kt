package com.halovoid.lncrawler.ui.feature.novel.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.core.theme.*

@Composable
fun NovelSynopsisSection(
    novel: Novel,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "ChevronRotation"
    )

    val fadeColor = DarkBackground

    Column(
        modifier = Modifier
            .animateContentSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onExpandClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = novel.description ?: "No description available.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
                modifier = Modifier.drawWithContent {
                    drawContent()
                    if (!isExpanded) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, fadeColor),
                                startY = size.height * 0.5f,
                                endY = size.height
                            )
                        )
                    }
                }
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Show Less" else "Read More",
            tint = SecondaryText.copy(alpha = 0.7f),
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation)
                .offset(y = (-4).dp)
        )
    }
}
