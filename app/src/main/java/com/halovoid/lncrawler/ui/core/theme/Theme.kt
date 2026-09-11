package com.halovoid.lncrawler.ui.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.halovoid.lncrawler.data.repository.PreferenceRepository

@Composable
fun LNCrawlerTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val preferenceRepository = remember { PreferenceRepository.getInstance(context) }

    val themeModeStr by preferenceRepository.themeMode.collectAsState(initial = "SYSTEM")
    val selectedThemeId by preferenceRepository.selectedThemeId.collectAsState(initial = "DEFAULT")
    val isAmoledMode by preferenceRepository.isAmoledMode.collectAsState(initial = false)

    val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM)
    val appTheme = ThemeRegistry.getThemeById(selectedThemeId)

    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val baseColorScheme = when {
        appTheme.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isDark) {
                dynamic.copy(
                    outline = dynamic.outlineVariant.copy(alpha = 0.5f),
                    outlineVariant = dynamic.surfaceVariant.copy(alpha = 0.35f)
                )
            } else {
                dynamic.copy(
                    outline = dynamic.outlineVariant.copy(alpha = 0.6f),
                    outlineVariant = dynamic.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }
        isDark -> appTheme.darkColorScheme
        else -> appTheme.lightColorScheme
    }

    val colorScheme = if (isDark && isAmoledMode) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color(0xFF080808),
            surfaceVariant = Color(0xFF141414),
            outline = Color(0xFF222222),
            outlineVariant = Color(0xFF161616)
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
