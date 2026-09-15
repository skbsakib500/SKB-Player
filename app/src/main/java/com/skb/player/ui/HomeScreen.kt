package com.skb.player.ui

import android.Manifest
import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.MediaStore
import android.widget.Toast
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.skb.player.library.FavoritesManager
import com.skb.player.library.FileOps
import com.skb.player.library.HistoryManager
import com.skb.player.library.MediaScanner
import com.skb.player.library.PlaylistManager
import com.skb.player.library.RecentEntry
import com.skb.player.library.SettingsManager
import com.skb.player.library.ThumbnailLoader
import com.skb.player.library.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SKBTab(val emoji: String, val label: String) {
    HOME("\uD83C\uDFE0", "Home"),
    VIDEOS("\uD83C\uDFAC", "Videos"),
    FOLDERS("\uD83D\uDCC1", "Folders"),
    LISTS("\u2B50", "Lists"),
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
    favorites: FavoritesManager,
    playlists: PlaylistManager,
    theme: SKBTheme,
    tab: Int,
    onTabChange: (Int) -> Unit,
    onOpenVideo: (Uri) -> Unit,
    onPickVideo: () -> Unit,
    onPlayAll: (List<VideoItem>) -> Unit,
    onPlayUris: (List<Uri>) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onThemeChange: (Int) -> Unit,
    selectedFolderId: String?,
    onSelectFolder: (String?) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var libraryVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var sortMode by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var actionVideo by remember { mutableStateOf<VideoItem?>(null) }
    var infoVideo by remember { mutableStateOf<VideoItem?>(null) }
    var renameVideo by remember { mutableStateOf<VideoItem?>(null) }
    var deleteVideo by remember { mutableStateOf<VideoItem?>(null) }
    var saveToPlaylistVideo by remember { mutableStateOf<VideoItem?>(null) }
    var favTick by remember { mutableIntStateOf(0) }
    var pendingDeleteVideo by remember { mutableStateOf<VideoItem?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res -> hasPermission = res.values.all { it } }

    fun requiredPerms(): Array<String> = if (Build.VERSION.SDK_INT >= 33)
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    LaunchedEffect(Unit, refreshKey) {
        val perms = requiredPerms()
        val granted = perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        hasPermission = granted
        if (granted) {
            libraryVideos = withContext(Dispatchers.IO) {
                MediaScanner.scanVideos(context, 2000)
            }
        } else {
            permLauncher.launch(perms)
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
            NavigationBar(containerColor = navBarColor(theme)) {
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
                    history, libraryVideos, hasPermission, theme,
                    onOpenVideo, onPickVideo, onOpenHistory, onOpenSearch
                ) { permLauncher.launch(requiredPerms()) }
                1 -> VideosTab(
                    sortedVideos, hasPermission, SortMode.entries[sortMode], theme,
                    onOpenVideo, onPickVideo, onPlayAll, onOpenSearch,
                    onMore = { actionVideo = it },
                    onCycleSort = { sortMode = (sortMode + 1) % SortMode.entries.size }
                ) { permLauncher.launch(requiredPerms()) }
                2 -> if (hasPermission) {
                    FolderScreen(
                        allVideos = libraryVideos,
                        selectedFolderId = selectedFolderId,
                        onSelectFolder = onSelectFolder,
                        onOpenVideo = onOpenVideo,
                        onMore = { actionVideo = it }
                    )
                } else {
                    PermissionPrompt { permLauncher.launch(requiredPerms()) }
                }
                3 -> ListsTab(
                    favorites = favorites,
                    playlists = playlists,
                    allVideos = libraryVideos,
                    theme = theme,
                    onOpenVideo = onOpenVideo,
                    onPlayUris = onPlayUris,
                    onRefresh = { refreshKey++ }
                )
                else -> SettingsTabContent(
                    settings = settings,
                    theme = theme,
                    onThemeChange = onThemeChange,
                    onRefreshLibrary = { refreshKey++ }
                )
            }
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val v = pendingDeleteVideo
        pendingDeleteVideo = null
        if (v != null && result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Deleted: ${v.name}", Toast.LENGTH_SHORT).show()
            refreshKey++
        }
    }

    actionVideo?.let { v ->
        VideoActionMenu(
            video = v,
            isFavorite = favorites.isFavorite(v.uri),
            isWatchLater = favorites.isWatchLater(v.uri),
            onDismiss = { actionVideo = null },
            onPlay = { onOpenVideo(v.uri) },
            onInfo = { infoVideo = v },
            onRename = { renameVideo = v },
            onDelete = { deleteVideo = v },
            onToggleFavorite = { favorites.toggleFavorite(v.uri); favTick++ },
            onToggleWatchLater = { favorites.toggleWatchLater(v.uri); favTick++ },
            onSaveToPlaylist = { saveToPlaylistVideo = v },
            onShare = {
                val ctx = context
                try {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(android.content.Intent.EXTRA_STREAM, v.uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(
                        android.content.Intent.createChooser(send, "Share video")
                    )
                } catch (_: Exception) {}
            }
        )
    }

    infoVideo?.let { v -> VideoInfoDialog(video = v, onDismiss = { infoVideo = null }) }

    renameVideo?.let { v ->
        RenameDialog(
            video = v,
            onDismiss = { renameVideo = null },
            onRename = { newName ->
                val r = FileOps.rename(context, v.uri, newName)
                if (r.isSuccess) {
                    Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show()
                    refreshKey++
                } else {
                    Toast.makeText(
                        context,
                        "Rename failed: ${r.exceptionOrNull()?.message ?: "unknown"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    deleteVideo?.let { v ->
        DeleteConfirmDialog(
            video = v,
            onDismiss = { deleteVideo = null },
            onConfirm = {
                pendingDeleteVideo = v
                try {
                    val pi = MediaStore.createDeleteRequest(
                        context.contentResolver, listOf(v.uri)
                    )
                    deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } catch (e: Exception) {
                    val r = FileOps.delete(context, v.uri)
                    if (r.isSuccess) {
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        refreshKey++
                    } else {
                        Toast.makeText(
                            context,
                            "Delete failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    saveToPlaylistVideo?.let { v ->
        SaveToPlaylistDialog(
            playlists = playlists.listAll(),
            onDismiss = { saveToPlaylistVideo = null },
            onCreateNew = { name ->
                if (name.isNotBlank()) {
                    val pl = playlists.create(name)
                    playlists.addVideo(pl.id, v.uri)
                    Toast.makeText(context, "Added to ${pl.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onAddTo = { pl ->
                playlists.addVideo(pl.id, v.uri)
                Toast.makeText(context, "Added to ${pl.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Library access required")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRequest) { Text("Grant Access") }
        }
    }
}

@Composable
private fun HomeTab(
    history: HistoryManager,
    libraryVideos: List<VideoItem>,
    hasPermission: Boolean,
    theme: SKBTheme,
    onOpenVideo: (Uri) -> Unit,
    onPickVideo: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val recent = history.getRecent(6)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header() }
        item { SearchBarPlaceholder(theme) { onOpenSearch() } }

        if (recent.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Continue Watching", Modifier.weight(1f))
                    TextButton(onClick = onOpenHistory) {
                        Text("History >", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(recent, key = { "r_${it.uri}" }) { e ->
                RecentCard(e, theme) { onOpenVideo(e.uri) }
            }
        }

        if (!hasPermission) {
            item {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRequestPermission) { Text("Grant Library Access") }
            }
        } else if (libraryVideos.isNotEmpty()) {
            item { SectionHeader("Recently Added") }
            items(libraryVideos.take(8), key = { "l_${it.uri}" }) { v ->
                LibraryCard(v, theme) { onOpenVideo(v.uri) }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onPickVideo, modifier = Modifier.fillMaxWidth()) {
                Text("Pick Video from Storage")
            }
        }
    }
}

@Composable
private fun VideosTab(
    libraryVideos: List<VideoItem>,
    hasPermission: Boolean,
    sortMode: SortMode,
    theme: SKBTheme,
    onOpenVideo: (Uri) -> Unit,
    onPickVideo: () -> Unit,
    onPlayAll: (List<VideoItem>) -> Unit,
    onOpenSearch: () -> Unit,
    onMore: (VideoItem) -> Unit,
    onCycleSort: () -> Unit,
    onRequestPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header() }
        item { SearchBarPlaceholder(theme) { onOpenSearch() } }

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
                    Button(onClick = { onPlayAll(libraryVideos) }, modifier = Modifier.weight(1f)) {
                        Text("\u25B6 Play All")
                    }
                    OutlinedButton(onClick = onPickVideo, modifier = Modifier.weight(1f)) {
                        Text("Pick")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${libraryVideos.size} videos  \u2022  ${sortMode.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onCycleSort) {
                        Text("Sort \u21BB", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(libraryVideos, key = { "v_${it.uri}" }) { v ->
                LibraryCard(v, theme, { onOpenVideo(v.uri) }, { onMore(v) })
            }
        }
    }
}

@Composable
private fun ListsTab(
    favorites: FavoritesManager,
    playlists: PlaylistManager,
    allVideos: List<VideoItem>,
    theme: SKBTheme,
    onOpenVideo: (Uri) -> Unit,
    onPlayUris: (List<Uri>) -> Unit,
    onRefresh: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val favUris = remember(refresh) { favorites.getFavorites() }
    val laterUris = remember(refresh) { favorites.getWatchLater() }
    val allPlaylists = remember(refresh) { playlists.listAll() }

    val favVideos = allVideos.filter { favUris.contains(it.uri.toString()) }
    val laterVideos = allVideos.filter { laterUris.contains(it.uri.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                Text("Lists", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text("Favorites, Watch Later, Playlists",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\u2B50 Favorites (${favVideos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (favVideos.isNotEmpty()) {
                    TextButton(onClick = { onPlayUris(favVideos.map { it.uri }) }) {
                        Text("\u25B6 Play All", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (favVideos.isEmpty()) {
            item { Text("No favorites yet. Tap \u2B50 in player.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(favVideos, key = { "fav_${it.uri}" }) { v ->
                LibraryCard(v, theme) { onOpenVideo(v.uri) }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\u23F0 Watch Later (${laterVideos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (laterVideos.isNotEmpty()) {
                    TextButton(onClick = { onPlayUris(laterVideos.map { it.uri }) }) {
                        Text("\u25B6 Play All", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (laterVideos.isEmpty()) {
            item { Text("Nothing queued. Tap \u23F0 in player.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(laterVideos, key = { "later_${it.uri}" }) { v ->
                LibraryCard(v, theme) { onOpenVideo(v.uri) }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text("Playlists (${allPlaylists.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
        if (allPlaylists.isEmpty()) {
            item { Text("No playlists yet. Save from player.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(allPlaylists, key = { it.id }) { p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = surfaceColor(theme)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(p.name, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(2.dp))
                        Text("${p.videoUris.size} videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    onPlayUris(p.videoUris.map { Uri.parse(it) })
                                },
                                enabled = p.videoUris.isNotEmpty()
                            ) { Text("\u25B6 Play") }
                            TextButton(onClick = {
                                playlists.delete(p.id); refresh++
                            }) { Text("Delete", color = Color(0xFFFF6E6E)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text("SKB Player",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("Premium Offline Media OS",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchBarPlaceholder(theme: SKBTheme, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = searchBarColor(theme),
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
            Text("Search videos, music\u2026",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun RecentCard(entry: RecentEntry, theme: SKBTheme, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColor(theme)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            val pct = if (entry.durationMs > 0)
                (entry.positionMs * 100 / entry.durationMs).toInt().coerceIn(0, 100) else 0
            Text("$pct%  \u2022  ${fmt(entry.positionMs)} / ${fmt(entry.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun LibraryCard(
    v: VideoItem,
    theme: SKBTheme,
    onClick: () -> Unit,
    onMore: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColor(theme)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumb(v.uri, theme)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(v.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("${fmt(v.durationMs)}  \u2022  ${humanSize(v.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onMore) {
                Text("\u22EE", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VideoThumb(uri: Uri, theme: SKBTheme) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            ThumbnailLoader.load(context, uri, 200)
        }
    }
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 64.dp)
            .padding(end = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83C\uDFAC", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
