package com.halovoid.bunori.ui.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.halovoid.bunori.ui.core.theme.BorderColor
import com.halovoid.bunori.ui.core.theme.ErrorRed
import com.halovoid.bunori.ui.core.theme.SecondaryText
import com.halovoid.bunori.ui.core.theme.SuccessGreen

@Composable
fun ProgressIndicator(
    success: Int,
    failed: Int,
    cancelled: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BorderColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (success > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(success.toFloat())
                        .background(SuccessGreen)
                )
            }

            if (failed > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(failed.toFloat())
                        .background(ErrorRed)
                )
            }

            if (cancelled > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(cancelled.toFloat())
                        .background(SecondaryText)
                )
            }

            val remaining = total - (success + failed + cancelled)
            if (remaining > 0) {
                Spacer(modifier = Modifier.weight(remaining.toFloat()))
            }
        }
    }
}
