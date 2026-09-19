package com.halovoid.bunori.ui.feature.novel.components.artifact

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.ui.core.theme.*

@Composable
fun ExportFormatItem(
    format: ExportFormat,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) BrandAccent.copy(alpha = 0.12f) else DarkSurfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.5.dp,
            color = if (selected) BrandAccent else BorderColor.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = format.extension.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) BrandAccent else PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (format) {
                        ExportFormat.EPUB -> "Reflowable eBook"
                        ExportFormat.PDF -> "Fixed Document"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = BrandAccent,
                    unselectedColor = SecondaryText.copy(alpha = 0.5f)
                ),
                modifier = Modifier.scale(0.9f)
            )
        }
    }
}
