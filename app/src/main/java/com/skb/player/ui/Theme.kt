package com.skb.player.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Brush

// --- 3080 COLOR SYSTEM ---
val SpaceBlack = Color(0xFF05080F)
val SpaceBlack2 = Color(0xFF0A1020)
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFF8A5CFF)
val NeonBlue = Color(0xFF3A7BFF)
val GlassWhite10 = Color(0x1AFFFFFF)
val GlassWhite05 = Color(0x0DFFFFFF)

val HoloGradient = Brush.linearGradient(listOf(NeonCyan, NeonPurple, NeonBlue))
val CardBorderGradient = Brush.linearGradient(listOf(Color(0xFF00F0FF).copy(0.8f), Color(0xFF8A5CFF).copy(0.4f)))
val BgNebulaGradient = Brush.radialGradient(
    colors = listOf(Color(0xFF0F1A3A).copy(0.8f), Color(0xFF05080F)),
    radius = 1200f
)

@Composable
fun SKB3080Theme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = SpaceBlack,
        surface = SpaceBlack2,
        surfaceVariant = Color(0xFF101828),
        primary = NeonCyan,
        secondary = NeonPurple,
        onBackground = Color.White,
        onSurface = Color.White
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

// Helper for your existing files
fun surfaceColor(theme: SKBTheme): Color = GlassWhite10
fun navBarColor(theme: SKBTheme): Color = Color(0xCC05080F)
fun searchBarColor(theme: SKBTheme): Color = GlassWhite10
enum class SKBTheme { DARK, AMOLED, HOLO_3080 }
