package com.skb.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.skb.player.library.HistoryManager
import com.skb.player.library.MediaScanner
import com.skb.player.library.RecentEntry
import com.skb.player.library.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    history: HistoryManager,
    onOpenVideo: (Uri) -> Unit,
    onPickVideo: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var libraryVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res -> hasPermission = res.values.all { it } }

    LaunchedEffect(Unit) {
        val perms = if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        val granted = perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        hasPermission = granted
        if (!granted) permLauncher.launch(perms)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            libraryVideos = withContext(Dispatchers.IO) { MediaScanner.scanVideos(context) }
        }
    }

    val recentList = history.getRecent()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("SKB Player", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text("Premium Offline Media OS",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        if (recentList.isNotEmpty()) {
            item { SectionHeader("Continue Watching") }
            items(recentList, key = { it.uri.toString() }) { entry ->
                RecentCard(entry) { onOpenVideo(entry.uri) }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }

        if (hasPermission && libraryVideos.isNotEmpty()) {
            item { SectionHeader("Library") }
            items(libraryVideos.take(30), key = { it.uri.toString() }) { v ->
                LibraryCard(v) { onOpenVideo(v.uri) }
            }
            item { Spacer(Modifier.height(12.dp)) }
        } else if (!hasPermission) {
            item {
                Button(onClick = {
                    val perms = if (Build.VERSION.SDK_INT >= 33)
                        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    permLauncher.launch(perms)
                }) { Text("Grant Library Access") }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onPickVideo,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Pick Video") }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun RecentCard(entry: RecentEntry, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(12.dp)) {
            Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            val pct = if (entry.durationMs > 0)
                (entry.positionMs * 100 / entry.durationMs).toInt().coerceIn(0, 100) else 0
            Text("$pct%  •  ${fmt(entry.positionMs)} / ${fmt(entry.durationMs)}",
                style = MaterialTheme.typography.bodySmall)
            val frac = if (entry.durationMs > 0)
                entry.positionMs.toFloat() / entry.durationMs else 0f
            LinearProgressIndicator(
                progress = { frac.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun LibraryCard(v: VideoItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(12.dp)) {
            Text(v.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(fmt(v.durationMs) + "  •  " + humanSize(v.sizeBytes),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun fmt(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun humanSize(bytes: Long): String {
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
