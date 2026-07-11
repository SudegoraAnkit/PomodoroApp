package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SophisticatedPurple,
    secondary = BreakEmeraldGreen,
    tertiary = LongBreakCalmBlue,
    background = SophisticatedDarkBg,
    surface = SophisticatedDarkSurface,
    onPrimary = SophisticatedOnPurple,
    onSecondary = SophisticatedDarkBg,
    onTertiary = SophisticatedDarkBg,
    onBackground = SophisticatedText,
    onSurface = SophisticatedText,
    outline = SophisticatedDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = SophisticatedPurple,
    secondary = BreakEmeraldGreen,
    tertiary = LongBreakCalmBlue,
    background = SophisticatedDarkBg, // Keep deep rich dark theme for Sophisticated Dark design theme
    surface = SophisticatedDarkSurface,
    onPrimary = SophisticatedOnPurple,
    onSecondary = SophisticatedDarkBg,
    onTertiary = SophisticatedDarkBg,
    onBackground = SophisticatedText,
    onSurface = SophisticatedText,
    outline = SophisticatedDarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to preserve our beautifully customized Tomato Focus theme colors!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
