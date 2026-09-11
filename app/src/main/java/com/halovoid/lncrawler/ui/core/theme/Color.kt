package com.halovoid.lncrawler.ui.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * LNCrawler Dynamic Theme Tokens mapped to active MaterialTheme ColorScheme
 */
val DarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val DarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val BrandAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val PrimaryAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.secondary

val PrimaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val SecondaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val BorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant


// Static Semantic & System Colors
val SuccessGreen = Color(0xFF22C55E) // Semantic Success
val WarningAmber = Color(0xFFF59E0B) // Semantic Warning
val ErrorRed = Color(0xFFEF4444)     // Semantic Error
val SupportRose = Color(0xFFF43F5E)   // Warm Rose for support/emotive actions

val DiscordBlurple = Color(0xFF5865F2)
val GitHubOrange = Color(0xFFF34F29)

// Legacy colors kept for reference or specific use cases
val NearBlack = Color(0xFF121212)
val PureWhite = Color(0xFFFFFFFF)
val LightGray = Color(0xFFE0E0E0)
