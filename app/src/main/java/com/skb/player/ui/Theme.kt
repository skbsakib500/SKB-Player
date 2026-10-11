package com.skb.player.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val SpaceBlack = Color(0xFF05080F)
val SpaceBlack2 = Color(0xFF0A1020)
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFF8A5CFF)
val NeonBlue = Color(0xFF3A7BFF)
val GlassWhite10 = Color(0x1AFFFFFF)
val GlassWhite05 = Color(0x0DFFFFFF)
val HoloGradient = Brush.linearGradient(listOf(NeonCyan, NeonPurple, NeonBlue))
val CardBorderGradient = Brush.linearGradient(listOf(Color(0xFF00F0FF).copy(0.8f), Color(0xFF8A5CFF).copy(0.4f)))

enum class SKBTheme { DARK, AMOLED, HOLO_3080 }

// Compatibility for old MainActivity.kt
@Composable fun themeColors(theme: SKBTheme) = MaterialTheme.colorScheme
fun surfaceColor(theme: SKBTheme): Color = GlassWhite10
fun navBarColor(theme: SKBTheme): Color = Color(0xCC05080F)
fun searchBarColor(theme: SKBTheme): Color = GlassWhite10
