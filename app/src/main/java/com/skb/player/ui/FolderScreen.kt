package com.skb.player.ui

import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skb.player.library.FileOps
import com.skb.player.library.FolderItem
import com.skb.player.library.MediaScanner
import com.skb.player.library.VideoItem

@Composable
fun FolderScreen(
    allVideos: List<VideoItem>,
    selectedFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    onOpenVideo: (Uri) -> Unit,
    onMore: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            refreshKey++
        }
    }

    val folders = remember(allVideos, refreshKey) { MediaScanner.foldersFrom(allVideos) }

    if (selectedFolderId == null) {
        FolderList(
            folders = folders,
            onOpenFolder = { onSelectFolder(it.bucketId) }
        )
    } else {
        val folder = folders.firstOrNull { it.bucketId == selectedFolderId }
        val videos = remember(allVideos, refreshKey, selectedFolderId) {
            MediaScanner.videosInFolder(allVideos, selectedFolderId)
        }
        FolderContents(
            folder = folder,
            videos = videos,
            onBack = { onSelectFolder(null) },
            onOpenVideo = { onOpenVideo(it.uri) },
            onMore = onMore
        )
    }
}

@Composable
private fun FolderList(
    folders: List<FolderItem>,
    onOpenFolder: (FolderItem) -> Unit
) {
    if (folders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No folders found", color = Color(0xFF888888))
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Folders (${folders.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
        }
        items(folders, key = { it.bucketId }) { f ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { onOpenFolder(f) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("\uD83D\uDCC1", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.padding(6.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            f.name,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${f.count} video${if (f.count != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9A9A9A)
                        )
                    }
                    Text("\u203A", style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF666666))
                }
            }
        }
    }
}

@Composable
private fun FolderContents(
    folder: FolderItem?,
    videos: List<VideoItem>,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onMore: (VideoItem) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("< Folders") }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    folder?.name ?: "Folder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${videos.size} videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A9A9A)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos, key = { it.uri.toString() }) { v ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenVideo(v) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                v.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${fmtFolder(v.durationMs)}  \u2022  ${humanSizeFolder(v.sizeBytes)}  \u2022  ${v.width}x${v.height}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9A9A9A)
                            )
                        }
                        TextButton(onClick = { onMore(v) }) {
                            Text("\u22EE", style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF9A9A9A))
                        }
                    }
                }
            }
        }
    }
}

private fun fmtFolder(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun humanSizeFolder(bytes: Long): String {
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
