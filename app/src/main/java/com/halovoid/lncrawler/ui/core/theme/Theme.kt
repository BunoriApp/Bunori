package com.halovoid.lncrawler.ui.core.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Modern Dark color scheme for LNCrawler.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryText, // Text is often the most "primary" element in editorial
    secondary = BrandAccent,
    tertiary = ErrorRed,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = DarkBackground,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = PrimaryText,
    onSurface = PrimaryText,
    onSurfaceVariant = SecondaryText,
    outline = BorderColor,
    outlineVariant = BorderColor.copy(alpha = 0.5f)
)

/**
 * Light color scheme (simplified, focusing on Dark theme).
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    background = PureWhite,
    surface = LightGray
)

@Composable
fun LNCrawlerTheme(
    darkTheme: Boolean = true, // Default to dark theme as requested
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
