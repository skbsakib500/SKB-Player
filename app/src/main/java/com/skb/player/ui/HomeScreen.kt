package com.skb.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.skb.player.library.HistoryManager
import com.skb.player.library.MediaScanner
import com.skb.player.library.RecentEntry
import com.skb.player.library.SettingsManager
import com.skb.player.library.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SKBTab(val emoji: String, val label: String) {
    HOME("\uD83C\uDFE0", "Home"),
    VIDEOS("\uD83C\uDFAC", "Videos"),
    MUSIC("\uD83C\uDFB5", "Music"),
    SETTINGS("\u2699", "Settings")
}

enum class SortMode(val label: String) {
    DATE("Newest"),
    NAME("Name"),
    DURATION("Longest"),
    SIZE("Largest")
}

@Composable
fun HomeScaffold(
    history: HistoryManager,
    settings: SettingsManager,
    tab: Int,
    onTabChange: (Int) -> Unit,
    onOpenVideo: (Uri) -> Unit,
    onPickVideo: () -> Unit,
    onPlayAll: (List<VideoItem>) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var libraryVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var sortMode by remember { mutableIntStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res -> hasPermission = res.values.all { it } }

    fun requiredPerms(): Array<String> = if (Build.VERSION.SDK_INT >= 33)
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    LaunchedEffect(Unit) {
        val perms = requiredPerms()
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

    val sortedVideos = remember(libraryVideos, sortMode) {
        when (SortMode.entries[sortMode]) {
            SortMode.DATE -> libraryVideos
            SortMode.NAME -> libraryVideos.sortedBy { it.name.lowercase() }
            SortMode.DURATION -> libraryVideos.sortedByDescending { it.durationMs }
            SortMode.SIZE -> libraryVideos.sortedByDescending { it.sizeBytes }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A0A0A)) {
                SKBTab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { onTabChange(i) },
                        icon = { Text(t.emoji) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeTab(
                    history, libraryVideos, hasPermission,
                    onOpenVideo, onPickVideo, onOpenHistory, onOpenSearch
                ) { permLauncher.launch(requiredPerms()) }
                1 -> VideosTab(
                    sortedVideos, hasPermission, SortMode.entries[sortMode],
                    onOpenVideo, onPickVideo, onPlayAll, onOpenSearch,
                    onCycleSort = { sortMode = (sortMode + 1) % SortMode.entries.size }
                ) { permLauncher.launch(requiredPerms()) }
                2 -> PlaceholderTab(
                    "\uD83C\uDFB5 Music",
                    "Background playback, playlists, equalizer \u2014 coming soon"
                )
                else -> SettingsTabContent(settings)
            }
        }
    }
}

@Composable
private fun HomeTab(
    history: HistoryManager,
    libraryVideos: List<VideoItem>,
    hasPermission: Boolean,
    onOpenVideo: (Uri) -> Unit,
    onPickVideo: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val recent = history.getRecent(10)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header() }
        item { SearchBarPlaceholder(onClick = onOpenSearch) }

        if (recent.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Continue Watching", Modifier.weight(1f))
                    TextButton(onClick = onOpenHistory) {
                        Text("History >", color = Color(0xFF00E5FF))
                    }
                }
            }
            items(recent, key = { "recent_${it.uri}" }) { e ->
                RecentCard(e) { onOpenVideo(e.uri) }
            }
        }

        if (!hasPermission) {
            item {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRequestPermission) { Text("Grant Library Access") }
            }
        } else if (libraryVideos.isNotEmpty()) {
            item { SectionHeader("Recently Added") }
            items(libraryVideos.take(10), key = { "lib_${it.uri}" }) { v ->
                LibraryCard(v) { onOpenVideo(v.uri) }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onPickVideo,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Pick Video from Storage") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun VideosTab(
    libraryVideos: List<VideoItem>,
    hasPermission: Boolean,
    sortMode: SortMode,
    onOpenVideo: (Uri) -> Unit,
    onPickVideo: () -> Unit,
    onPlayAll: (List<VideoItem>) -> Unit,
    onOpenSearch: () -> Unit,
    onCycleSort: () -> Unit,
    onRequestPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header() }
        item { SearchBarPlaceholder(onClick = onOpenSearch) }

        if (!hasPermission) {
            item {
                Column {
                    Text("Library access required to show videos.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRequestPermission) { Text("Grant Access") }
                }
            }
        } else if (libraryVideos.isEmpty()) {
            item { Text("No videos found on device.") }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPlayAll(libraryVideos) },
                        modifier = Modifier.weight(1f)
                    ) { Text("\u25B6 Play All") }
                    OutlinedButton(
                        onClick = onPickVideo,
                        modifier = Modifier.weight(1f)
                    ) { Text("Pick") }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${libraryVideos.size} videos  \u2022  Sorted: ${sortMode.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onCycleSort) {
                        Text("Sort \u21BB", color = Color(0xFF00E5FF))
                    }
                }
            }
            items(libraryVideos, key = { "vid_${it.uri}" }) { v ->
                LibraryCard(v) { onOpenVideo(v.uri) }
            }
        }
    }
}

@Composable
private fun PlaceholderTab(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            "SKB Player",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text("Premium Offline Media OS", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SearchBarPlaceholder(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141414),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83D\uDD0D", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(10.dp))
            Text("Search videos, music\u2026", color = Color(0xFF888888))
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 10.dp)
    )
}

@Composable
private fun RecentCard(entry: RecentEntry, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                entry.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            val pct = if (entry.durationMs > 0)
                (entry.positionMs * 100 / entry.durationMs).toInt().coerceIn(0, 100)
            else 0
            Text(
                "$pct%  \u2022  ${fmt(entry.positionMs)} / ${fmt(entry.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A9A9A)
            )
            val frac = if (entry.durationMs > 0)
                entry.positionMs.toFloat() / entry.durationMs else 0f
            LinearProgressIndicator(
                progress = { frac.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun LibraryCard(v: VideoItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                v.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${fmt(v.durationMs)}  \u2022  ${humanSize(v.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A9A9A)
            )
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
