package com.halovoid.bunori.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun InfoScreen(
    icon: ImageVector,
    headingText: String,
    subtitleText: String,
    acceptText: String? = null,
    onAcceptClick: (() -> Unit)? = null,
    canAccept: Boolean = true,
    rejectText: String? = null,
    onRejectClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        bottomBar = {
            val strokeWidth = Dp.Hairline
            val borderColor = MaterialTheme.colorScheme.outline
            if (acceptText != null || rejectText != null) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .drawBehind {
                            drawLine(
                                borderColor,
                                Offset(0f, 0f),
                                Offset(size.width, 0f),
                                strokeWidth.value,
                            )
                        }
                        .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                        .padding(
                            horizontal = 10.dp,
                            vertical = 10.dp,
                        ),
                ) {
                    if (acceptText != null && onAcceptClick != null) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canAccept,
                            onClick = onAcceptClick,
                        ) {
                            Text(text = acceptText)
                        }
                    }
                    if (rejectText != null && onRejectClick != null) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onRejectClick,
                        ) {
                            Text(text = rejectText)
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp)
                .padding(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = headingText,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = subtitleText,
                modifier = Modifier
                    .padding(vertical = 10.dp),
                style = MaterialTheme.typography.titleSmall,
            )

            content()
        }
    }
}

@PreviewLightDark
@Composable
private fun InfoScaffoldPreview() {
    InfoScreen(
        icon = Icons.Outlined.Newspaper,
        headingText = "Heading",
        subtitleText = "Subtitle",
        acceptText = "Accept",
        onAcceptClick = {},
        rejectText = "Reject",
        onRejectClick = {},
    ) {
        Text("Hello world")
    }
}
