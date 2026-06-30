package com.backupx.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Applies the brand dark theme built from the values in BrandColors
 * Every color role is derived here so the palette stays in one place
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = BrandColors.blue,
        onPrimary = BrandColors.onAccent,
        primaryContainer = BrandColors.blueContainer,
        onPrimaryContainer = BrandColors.onBlueContainer,
        secondary = BrandColors.teal,
        onSecondary = BrandColors.onAccent,
        secondaryContainer = BrandColors.tealContainer,
        onSecondaryContainer = BrandColors.onTealContainer,
        tertiary = BrandColors.teal,
        onTertiary = BrandColors.onAccent,
        background = BrandColors.background,
        onBackground = BrandColors.onDark,
        surface = BrandColors.surface,
        onSurface = BrandColors.onDark,
        surfaceVariant = BrandColors.surfaceVariant,
        onSurfaceVariant = BrandColors.onDark,
        surfaceTint = BrandColors.blue,
        surfaceDim = BrandColors.surfaceDim,
        surfaceBright = BrandColors.surfaceBright,
        surfaceContainerLowest = BrandColors.surfaceContainerLowest,
        surfaceContainerLow = BrandColors.surfaceContainerLow,
        surfaceContainer = BrandColors.surfaceContainer,
        surfaceContainerHigh = BrandColors.surfaceContainerHigh,
        surfaceContainerHighest = BrandColors.surfaceContainerHighest,
        outline = BrandColors.outline,
        outlineVariant = BrandColors.outlineVariant,
        error = BrandColors.error,
        onError = BrandColors.onAccent
    )

    MaterialTheme(colorScheme = colorScheme, content = content)
}
