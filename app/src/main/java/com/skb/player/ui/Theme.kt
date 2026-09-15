package com.skb.player.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

enum class SKBTheme(val label: String) {
    AMOLED("AMOLED"),
    DARK("Dark"),
    OCEAN("Ocean")
}

fun themeColors(theme: SKBTheme) = when (theme) {
    SKBTheme.AMOLED -> darkColorScheme(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceVariant = Color(0xFF0F0F0F),
        primary = Color(0xFF00E5FF),
        onPrimary = Color(0xFF000000),
        onBackground = Color(0xFFEAEAEA),
        onSurface = Color(0xFFEAEAEA),
        onSurfaceVariant = Color(0xFFAAAAAA)
    )
    SKBTheme.DARK -> darkColorScheme(
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF242424),
        primary = Color(0xFF00B0FF),
        onPrimary = Color(0xFF000000),
        onBackground = Color(0xFFEEEEEE),
        onSurface = Color(0xFFEEEEEE),
        onSurfaceVariant = Color(0xFFB0B0B0)
    )
    SKBTheme.OCEAN -> darkColorScheme(
        background = Color(0xFF0A1A1F),
        surface = Color(0xFF0F2429),
        surfaceVariant = Color(0xFF153035),
        primary = Color(0xFF1DE9B6),
        onPrimary = Color(0xFF000000),
        onBackground = Color(0xFFE0F2F1),
        onSurface = Color(0xFFE0F2F1),
        onSurfaceVariant = Color(0xFF80CBC4)
    )
}

fun surfaceColor(theme: SKBTheme): Color = when (theme) {
    SKBTheme.AMOLED -> Color(0xFF101010)
    SKBTheme.DARK -> Color(0xFF242424)
    SKBTheme.OCEAN -> Color(0xFF153035)
}

fun navBarColor(theme: SKBTheme): Color = when (theme) {
    SKBTheme.AMOLED -> Color(0xFF0A0A0A)
    SKBTheme.DARK -> Color(0xFF1A1A1A)
    SKBTheme.OCEAN -> Color(0xFF081418)
}

fun searchBarColor(theme: SKBTheme): Color = when (theme) {
    SKBTheme.AMOLED -> Color(0xFF141414)
    SKBTheme.DARK -> Color(0xFF2A2A2A)
    SKBTheme.OCEAN -> Color(0xFF1B3A40)
}
