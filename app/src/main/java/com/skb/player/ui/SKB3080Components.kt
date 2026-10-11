package com.skb.player.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HoloCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier
            .border(BorderStroke(1.dp, CardBorderGradient), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassWhite10),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.background(Brush.verticalGradient(listOf(GlassWhite10, Color.Transparent)))) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun HoloButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .border(BorderStroke(1.dp, HoloGradient), RoundedCornerShape(50.dp))
            .clip(RoundedCornerShape(50.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.White)
    }
}

@Composable
fun FloatingHoloNavBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(BorderStroke(1.dp, GlassWhite10), RoundedCornerShape(28.dp)),
        color = Color(0xB30A1020),
        tonalElevation = 12.dp
    ) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SKBTab.entries.forEachIndexed { i, tab ->
                val isSel = selected == i
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSel) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                    modifier = Modifier.padding(4.dp)
                ) {
                    TextButton(onClick = { onSelect(i) }) {
                        Text(tab.emoji + " " + tab.label, color = if (isSel) NeonCyan else Color.White.copy(0.6f))
                    }
                }
            }
        }
    }
}
