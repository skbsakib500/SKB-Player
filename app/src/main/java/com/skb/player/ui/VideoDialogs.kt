package com.skb.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skb.player.library.VideoItem

@Composable
fun RenameDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    val baseName = video.name.substringBeforeLast('.', video.name)
    val ext = video.name.substringAfterLast('.', "")
    var text by remember { mutableStateOf(baseName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column {
                Text(
                    "Current: ${video.name}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A9A9A)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                if (ext.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Extension: .$ext (kept)",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9A9A9A)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newName = if (ext.isNotBlank()) "$text.$ext" else text
                    onRename(newName)
                    onDismiss()
                },
                enabled = text.isNotBlank()
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VideoInfoDialog(video: VideoItem, onDismiss: () -> Unit) {
    val duration = fmtInfo(video.durationMs)
    val size = humanSizeInfo(video.sizeBytes)
    val resolution = if (video.width > 0 && video.height > 0)
        "${video.width} \u00D7 ${video.height}" else "Unknown"
    val dateStr = if (video.dateAddedSec > 0)
        java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(video.dateAddedSec * 1000L))
    else "Unknown"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video Info") },
        text = {
            Column {
                InfoRow("Name", video.name)
                InfoRow("Duration", duration)
                InfoRow("Size", size)
                InfoRow("Resolution", resolution)
                InfoRow("Format", video.mimeType)
                InfoRow("Folder", video.bucketName)
                InfoRow("Path", video.relativePath)
                InfoRow("Added", dateStr)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF9A9A9A), fontWeight = FontWeight.Medium)
        Text(value, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
fun DeleteConfirmDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete video?") },
        text = { Text("${video.name}\n\nThis will remove the file from your device.") },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text("Delete", color = Color(0xFFFF6E6E))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VideoActionMenu(
    video: VideoItem,
    isFavorite: Boolean,
    isWatchLater: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onInfo: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onSaveToPlaylist: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(video.name, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                MenuRow("\u25B6  Play") { onPlay(); onDismiss() }
                MenuRow(if (isFavorite) "\u2B50  Remove from Favorites" else "\u2606  Add to Favorites") {
                    onToggleFavorite(); onDismiss()
                }
                MenuRow(if (isWatchLater) "\u23F0  Remove from Watch Later" else "\u23F1  Add to Watch Later") {
                    onToggleWatchLater(); onDismiss()
                }
                MenuRow("\uD83D\uDCD1  Save to Playlist") { onSaveToPlaylist(); onDismiss() }
                MenuRow("\u2139  Info") { onInfo(); onDismiss() }
                MenuRow("\u270F  Rename") { onRename(); onDismiss() }
                MenuRow("\uD83D\uDD17  Share") { onShare(); onDismiss() }
                MenuRow("\uD83D\uDDD1  Delete", color = Color(0xFFFF6E6E)) {
                    onDelete(); onDismiss()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun MenuRow(
    label: String,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            color = if (color == Color.Unspecified) Color.Unspecified else color,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun fmtInfo(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun humanSizeInfo(bytes: Long): String {
    if (bytes <= 0) return "-"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}
