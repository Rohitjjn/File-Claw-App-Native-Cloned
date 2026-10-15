package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ClaudeTerracotta,
    secondary = ClaudeSoftTomato,
    background = ClaudeAlabaster,
    surface = ClaudeWhite,
    onPrimary = Color.White,
    onSecondary = ClaudeCharcoal,
    onBackground = ClaudeCharcoal,
    onSurface = ClaudeCharcoal,
    outline = ClaudeSandBorder,
    surfaceVariant = ClaudeSelectedBg,
    onSurfaceVariant = ClaudeTerracotta
)

private val DarkColorScheme = darkColorScheme(
    primary = ClaudeTerracotta,
    secondary = ClaudeOrangeAccent,
    background = ClaudeOnyx,
    surface = ClaudeDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = ClaudeDarkText,
    onSurface = ClaudeDarkText,
    outline = ClaudeDarkBorder,
    surfaceVariant = ClaudeDarkSelectedBg,
    onSurfaceVariant = ClaudeOrangeAccent
)

/**
 * Whether the app is dark right now. Honours the in-app theme setting ("Light" / "Dark" / "System"),
 * which can differ from the phone's night mode; system bar icons must follow this, not the system.
 */
@Composable
fun isAppInDarkTheme(themeSetting: String): Boolean = when (themeSetting) {
    "Dark" -> true
    "Light" -> false
    else -> isSystemInDarkTheme()
}

@Composable
fun FilesClawTheme(
    themeSetting: String = "Light", // "Light", "Dark", "System"
    content: @Composable () -> Unit
) {
    val darkTheme = isAppInDarkTheme(themeSetting)

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalGlass provides if (darkTheme) DarkGlass else LightGlass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
