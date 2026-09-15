package com.skb.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.skb.player.library.EqualizerManager
import com.skb.player.library.Playlist

@Composable
fun SaveToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onCreateNew: (String) -> Unit,
    onAddTo: (Playlist) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(playlists.isEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Playlist") },
        text = {
            Column {
                if (playlists.isNotEmpty() && !showNew) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(playlists, key = { it.id }) { p ->
                            TextButton(
                                onClick = { onAddTo(p); onDismiss() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(p.name, modifier = Modifier.weight(1f))
                                    Text(
                                        "${p.videoUris.size}",
                                        color = Color(0xFF9A9A9A)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showNew = true }) {
                        Text("+ New Playlist")
                    }
                } else {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (playlists.isNotEmpty()) {
                        TextButton(onClick = { showNew = false }) {
                            Text("Back to list")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showNew) {
                TextButton(
                    onClick = { onCreateNew(newName); onDismiss() },
                    enabled = newName.isNotBlank()
                ) { Text("Create & Add") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        dismissButton = if (showNew && playlists.isNotEmpty()) ({
            TextButton(onClick = { showNew = false }) { Text("Cancel") }
        }) else null
    )
}

@Composable
fun EqualizerDialog(
    eq: EqualizerManager,
    initialEnabled: Boolean,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(initialEnabled) }
    var bands by remember { mutableIntStateOf(eq.bandCount()) }
    val (minLevel, maxLevel) = remember { eq.bandLevelRange() }
    var preset by remember { mutableIntStateOf(-1) }

    val levels = remember {
        mutableListOf<Float>().apply {
            for (i in 0 until bands) add(eq.getBandLevel(i).toFloat())
        }
    }
    var bassStrength by remember { mutableIntStateOf(0) }
    var virtStrength by remember { mutableIntStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(enabled) {
        eq.setEnabled(enabled)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Equalizer") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Spacer(Modifier.height(8.dp))

                if (enabled && bands > 0) {
                    Text(
                        "Presets",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = Color(0xFF00E5FF)
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val pc = eq.presetCount()
                        items((0 until pc).toList()) { i ->
                            TextButton(onClick = {
                                preset = i
                                eq.usePreset(i.toShort())
                                for (b in 0 until bands) levels[b] = eq.getBandLevel(b).toFloat()
                            }) {
                                Text(
                                    eq.presetName(i.toShort()),
                                    color = if (preset == i) Color(0xFF00E5FF) else Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Bands", color = Color(0xFF00E5FF),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium)

                    for (i in 0 until minOf(bands, 5)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${eq.bandCenterFreqHz(i)}Hz",
                                modifier = Modifier.width(64.dp),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = levels[i],
                                onValueChange = { v ->
                                    levels[i] = v
                                    eq.setBandLevel(i, v.toInt().toShort())
                                },
                                valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text("Bass Boost", color = Color(0xFF00E5FF),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Slider(
                        value = bassStrength.toFloat(),
                        onValueChange = {
                            bassStrength = it.toInt()
                            eq.setBassStrength(it.toInt().toShort())
                        },
                        valueRange = 0f..1000f
                    )

                    Text("Virtualizer", color = Color(0xFF00E5FF),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Slider(
                        value = virtStrength.toFloat(),
                        onValueChange = {
                            virtStrength = it.toInt()
                            eq.setVirtualizerStrength(it.toInt().toShort())
                        },
                        valueRange = 0f..1000f
                    )
                } else if (enabled) {
                    Text("Equalizer not supported on this device.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "OK",
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (destructive) Color(0xFFFF6E6E) else Color(0xFF00E5FF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
