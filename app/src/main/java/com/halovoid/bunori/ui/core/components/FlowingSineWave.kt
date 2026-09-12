package com.halovoid.bunori.ui.core.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun FlowingSineWave(
    modifier: Modifier = Modifier,
    color: Color,
    amplitude: Float,
    wavelength: Float,
    durationMillis: Int,
    reverse: Boolean,
    fillProgress: Float = 0.5f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = if (reverse) (2 * Math.PI).toFloat() else 0f,
        targetValue = if (reverse) 0f else (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()

        path.moveTo(0f, height)
        for (x in 0..width.toInt() step 4) {
            val relativeX = x.toFloat()
            val y = (height * (1f - fillProgress)) + Math.sin((relativeX / wavelength * 2 * Math.PI) - phase).toFloat() * amplitude
            path.lineTo(relativeX, y)
        }
        path.lineTo(width, height)
        path.close()

        drawPath(
            path = path,
            color = color
        )
    }
}
