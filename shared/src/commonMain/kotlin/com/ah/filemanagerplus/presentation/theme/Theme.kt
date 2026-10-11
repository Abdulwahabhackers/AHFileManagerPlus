package com.ah.filemanagerplus.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.ah.filemanagerplus.data.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = AHColors.Primary,
    onPrimary = AHColors.OnPrimaryLight,
    primaryContainer = AHColors.PrimaryLight,
    onPrimaryContainer = AHColors.OnBackgroundLight,
    background = AHColors.BackgroundLight,
    onBackground = AHColors.OnBackgroundLight,
    surface = AHColors.SurfaceLight,
    onSurface = AHColors.OnBackgroundLight,
    surfaceVariant = AHColors.SurfaceVariantLight,
    onSurfaceVariant = AHColors.OnSurfaceVariantLight,
    outline = AHColors.DividerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = AHColors.Primary,
    onPrimary = AHColors.OnPrimaryLight,
    primaryContainer = AHColors.PrimaryDark,
    onPrimaryContainer = AHColors.OnBackgroundDark,
    background = AHColors.BackgroundDark,
    onBackground = AHColors.OnBackgroundDark,
    surface = AHColors.SurfaceDark,
    onSurface = AHColors.OnBackgroundDark,
    surfaceVariant = AHColors.SurfaceVariantDark,
    onSurfaceVariant = AHColors.OnSurfaceVariantDark,
    outline = AHColors.DividerDark
)

@Composable
fun AHFileManagerTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}