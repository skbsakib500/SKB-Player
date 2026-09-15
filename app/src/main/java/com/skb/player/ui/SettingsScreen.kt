package com.skb.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skb.player.library.SettingsManager

private val SPEED_LABELS = listOf("0.5x", "1.0x", "1.25x", "1.5x", "2.0x")
private val SUB_SIZE_LABELS = listOf("Small", "Medium", "Large")
private val PROFILE_LABELS = listOf("Cinema", "Anime", "Study", "Minimal")

@Composable
fun SettingsTabContent(settings: SettingsManager) {
    var speedIndex by remember { mutableIntStateOf(settings.defaultSpeedIndex) }
    var subSize by remember { mutableIntStateOf(settings.defaultSubtitleSize) }
    var subProfile by remember { mutableIntStateOf(settings.defaultSubtitleProfile) }
    var autoplay by remember { mutableStateOf(settings.autoplayQueue) }
    var keepAwake by remember { mutableStateOf(settings.keepScreenOn) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Playback & subtitle defaults",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A9A9A)
                )
            }
        }

        item {
            SettingSection("Playback") {
                CycleRow(
                    label = "Default speed",
                    value = SPEED_LABELS[speedIndex]
                ) {
                    speedIndex = (speedIndex + 1) % SPEED_LABELS.size
                    settings.defaultSpeedIndex = speedIndex
                }
                ToggleRow(
                    label = "Autoplay next in queue",
                    checked = autoplay
                ) {
                    autoplay = it
                    settings.autoplayQueue = it
                }
                ToggleRow(
                    label = "Keep screen on while playing",
                    checked = keepAwake
                ) {
                    keepAwake = it
                    settings.keepScreenOn = it
                }
            }
        }

        item {
            SettingSection("Subtitles") {
                CycleRow(
                    label = "Default size",
                    value = SUB_SIZE_LABELS[subSize]
                ) {
                    subSize = (subSize + 1) % SUB_SIZE_LABELS.size
                    settings.defaultSubtitleSize = subSize
                }
                CycleRow(
                    label = "Profile",
                    value = PROFILE_LABELS[subProfile]
                ) {
                    subProfile = (subProfile + 1) % PROFILE_LABELS.size
                    settings.defaultSubtitleProfile = subProfile
                }
            }
        }

        item {
            SettingSection("About") {
                Column(Modifier.padding(12.dp)) {
                    Text("SKB Player v1.3", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Premium Offline Media OS",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9A9A9A)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Screenshots: Android/data/com.skb.player/files/screenshots",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun CycleRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            Text(value, color = Color(0xFF00E5FF))
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
