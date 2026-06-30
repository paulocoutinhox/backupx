package com.backupx.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the application palette, taken from the brand logo
 * Change these values to restyle the whole interface
 */
object BrandColors {

    // logo accents
    val blue = Color(0xFF0075FE)
    val teal = Color(0xFF06A79D)
    val navy = Color(0xFF021F42)

    // dark neutral surfaces tuned around the brand navy
    val background = Color(0xFF0A1322)
    val surface = Color(0xFF0F1B30)
    val surfaceVariant = Color(0xFF16243E)
    val outline = Color(0xFF2A3A57)
    val outlineVariant = Color(0xFF1E2C45)

    // navy ramp used by cards, dialogs and other raised containers
    val surfaceDim = Color(0xFF0A1322)
    val surfaceBright = Color(0xFF24375A)
    val surfaceContainerLowest = Color(0xFF09111E)
    val surfaceContainerLow = Color(0xFF101C31)
    val surfaceContainer = Color(0xFF15233C)
    val surfaceContainerHigh = Color(0xFF1A2A45)
    val surfaceContainerHighest = Color(0xFF1F3150)

    // foreground used for text and icons over dark surfaces
    val onDark = Color(0xFFE6ECF6)
    val onAccent = Color(0xFFFFFFFF)

    // accent containers for subtle highlighted areas
    val blueContainer = Color(0xFF003D85)
    val onBlueContainer = Color(0xFFCFE2FF)
    val tealContainer = Color(0xFF0A4A45)
    val onTealContainer = Color(0xFFBDEFE9)

    // feedback
    val error = Color(0xFFFF5A4E)
}
