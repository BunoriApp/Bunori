package com.halovoid.bunori.ui.feature.novel.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.domain.models.Request
import com.halovoid.bunori.ui.core.components.ProgressIndicator
import com.halovoid.bunori.ui.core.theme.*
import com.halovoid.bunori.ui.feature.request.components.StatusIndicator

@Composable
fun ActiveRequestCard(
    request: Request,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = DarkSurfaceVariant.copy(alpha = 0.3f),
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${request.name} · ${request.progressSuccess} / ${request.progressTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusIndicator(request.rstatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProgressIndicator(
                success = request.progressSuccess,
                failed = request.progressFailed,
                cancelled = request.progressCancelled,
                total = request.progressTotal,
            )
        }
    }
}
