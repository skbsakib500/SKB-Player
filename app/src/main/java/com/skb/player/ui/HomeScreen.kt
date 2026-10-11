package com.skb.player.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.skb.player.library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SKBTab(val emoji: String, val label: String) {
    HOME("◐", "Home"), VIDEOS("◎", "Vault"), FOLDERS("⬡", "Folders"), LISTS("✦", "Lists"), SETTINGS("◍", "System")
}
enum class SortMode(val label: String) { DATE("Newest"), NAME("Name"), DURATION("Longest"), SIZE("Largest") }

@Composable
fun HomeScaffold(
    history: HistoryManager, settings: SettingsManager, favorites: FavoritesManager, playlists: PlaylistManager,
    theme: SKBTheme, tab: Int, onTabChange: (Int) -> Unit, onOpenVideo: (Uri) -> Unit, onPickVideo: () -> Unit,
    onPlayAll: (List<VideoItem>) -> Unit, onPlayUris: (List<Uri>) -> Unit, onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit, onThemeChange: (Int) -> Unit, selectedFolderId: String?, onSelectFolder: (String?) -> Unit
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
    var pendingDeleteVideo by remember { mutableStateOf<VideoItem?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res -> hasPermission = res.values.all { it } }
    fun requiredPerms(): Array<String> = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    LaunchedEffect(Unit, refreshKey) {
        val perms = requiredPerms()
        val granted = perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        hasPermission = granted
        if (granted) { libraryVideos = withContext(Dispatchers.IO) { MediaScanner.scanVideos(context, 2000) } } else { permLauncher.launch(perms) }
    }
    val sortedVideos = remember(libraryVideos, sortMode) {
        when (SortMode.entries[sortMode]) {
            SortMode.DATE -> libraryVideos
            SortMode.NAME -> libraryVideos.sortedBy { it.name.lowercase() }
            SortMode.DURATION -> libraryVideos.sortedByDescending { it.durationMs }
            SortMode.SIZE -> libraryVideos.sortedByDescending { it.sizeBytes }
        }
    }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val v = pendingDeleteVideo; pendingDeleteVideo = null
        if (v!= null && result.resultCode == Activity.RESULT_OK) { Toast.makeText(context, "Deleted: ${v.name}", Toast.LENGTH_SHORT).show(); refreshKey++ }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A1020), SpaceBlack)))) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    FloatingHoloNavBar(selected = tab, onSelect = onTabChange)
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> HomeTab3080(theme, sortedVideos.size, hasPermission, onPickVideo, onOpenSearch, onOpenHistory) { permLauncher.launch(requiredPerms()) }
                    1 -> VideosTab3080(libraryVideos = sortedVideos, hasPermission = hasPermission, sortMode = SortMode.entries[sortMode], theme = theme, onOpenVideo = onOpenVideo, onPickVideo = onPickVideo, onPlayAll = onPlayAll, onOpenSearch = onOpenSearch, onMore = { actionVideo = it }, onCycleSort = { sortMode = (sortMode + 1) % SortMode.entries.size }, onRequestPermission = { permLauncher.launch(requiredPerms()) }, onDeleted = { refreshKey++ })
                    2 -> if (hasPermission) { FolderScreen(allVideos = libraryVideos, selectedFolderId = selectedFolderId, onSelectFolder = onSelectFolder, onOpenVideo = onOpenVideo, onMore = { actionVideo = it }) } else { PermissionPrompt { permLauncher.launch(requiredPerms()) } }
                    3 -> ListsTab(favorites = favorites, playlists = playlists, allVideos = libraryVideos, theme = theme, onOpenVideo = onOpenVideo, onPlayUris = onPlayUris)
                    else -> SettingsTabContent(settings = settings, theme = theme, onThemeChange = onThemeChange, onRefreshLibrary = { refreshKey++ })
                }
            }
        }
    }
    actionVideo?.let { v -> VideoActionMenu(video = v, isFavorite = favorites.isFavorite(v.uri), isWatchLater = favorites.isWatchLater(v.uri), onDismiss = { actionVideo = null }, onPlay = { onOpenVideo(v.uri) }, onInfo = { infoVideo = v }, onRename = { renameVideo = v }, onDelete = { deleteVideo = v }, onToggleFavorite = { favorites.toggleFavorite(v.uri) }, onToggleWatchLater = { favorites.toggleWatchLater(v.uri) }, onSaveToPlaylist = { saveToPlaylistVideo = v }, onShare = { try { val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "video/*"; putExtra(android.content.Intent.EXTRA_STREAM, v.uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(android.content.Intent.createChooser(send, "Share video")) } catch (_: Exception) {} }) }
    infoVideo?.let { v -> VideoInfoDialog(video = v, onDismiss = { infoVideo = null }) }
    renameVideo?.let { v -> RenameDialog(video = v, onDismiss = { renameVideo = null }, onRename = { newName -> val r = FileOps.rename(context, v.uri, newName); if (r.isSuccess) { Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show(); refreshKey++ } }) }
    deleteVideo?.let { v -> DeleteConfirmDialog(video = v, onDismiss = { deleteVideo = null }, onConfirm = { pendingDeleteVideo = v; try { val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(v.uri)); deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build()) } catch (_: Exception) { FileOps.delete(context, v.uri); refreshKey++ } }) }
    saveToPlaylistVideo?.let { v -> SaveToPlaylistDialog(playlists = playlists.listAll(), onDismiss = { saveToPlaylistVideo = null }, onCreateNew = { name -> if (name.isNotBlank()) { val pl = playlists.create(name); playlists.addVideo(pl.id, v.uri) } }, onAddTo = { pl -> playlists.addVideo(pl.id, v.uri) }) }
}

@Composable private fun PermissionPrompt(onRequest: () -> Unit) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { HoloButton("Grant Access", onClick = onRequest) } }

// --- 3080 HOME ---
@Composable
private fun HomeTab3080(theme: SKBTheme, totalVideos: Int, hasPermission: Boolean, onPickVideo: () -> Unit, onOpenSearch: () -> Unit, onOpenHistory: () -> Unit, onRequestPermission: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column {
                Text("SKB // 3080", style = MaterialTheme.typography.labelSmall, color = NeonCyan, letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp))
                Spacer(Modifier.height(4.dp))
                Text("AETHER PLAYER", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text("Offline Vault • No Signal • Pure", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
            }
        }
        item { Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(BorderStroke(1.dp, GlassWhite10), RoundedCornerShape(16.dp)).background(GlassWhite05).clickable { onOpenSearch() }.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("◍ Search vault...", color = Color.White.copy(0.5f)); Spacer(Modifier.weight(1f)); Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(50)).background(NeonCyan.copy(0.15f)).border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) { Text("↗", color = NeonCyan) } } } }
        item {
            HoloCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(NeonCyan.copy(0.3f), NeonPurple.copy(0.3f)))), contentAlignment = Alignment.Center) { Text("◐", color = Color.White) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("VAULT STATUS", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        Text(if (hasPermission) "$totalVideos Artifacts" else "Access Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text("● LIVE", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(Brush.linearGradient(listOf(NeonCyan, NeonBlue))).clickable { onPickVideo() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text("IMPORT", color = Color.Black, fontWeight = FontWeight.Bold) }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.dp, GlassWhite10, RoundedCornerShape(50)).clickable { onOpenHistory() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text("HISTORY", color = Color.White) }
                }
            }
        }
        if (!hasPermission) { item { HoloButton("Enable Vault Access", onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) } }
    }
}

// --- 3080 VIDEOS ---
@Composable
private fun VideosTab3080(
    libraryVideos: List<VideoItem>, hasPermission: Boolean, sortMode: SortMode, theme: SKBTheme,
    onOpenVideo: (Uri) -> Unit, onPickVideo: () -> Unit, onPlayAll: (List<VideoItem>) -> Unit,
    onOpenSearch: () -> Unit, onMore: (VideoItem) -> Unit, onCycleSort: () -> Unit,
    onRequestPermission: () -> Unit, onDeleted: () -> Unit
) {
    val context = LocalContext.current
    var selectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    fun exitSelection() { selectionMode = false; selectedUris = emptySet() }
    BackHandler(enabled = selectionMode) { exitSelection() }
    val batchDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result -> val uris = pendingDeleteUris; pendingDeleteUris = emptyList(); if (result.resultCode == Activity.RESULT_OK && uris.isNotEmpty()) { Toast.makeText(context, "Deleted ${uris.size}", Toast.LENGTH_SHORT).show(); onDeleted() }; exitSelection() }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("${libraryVideos.size} FILES", color = NeonCyan, style = MaterialTheme.typography.labelSmall); Text("VAULT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, GlassWhite10, RoundedCornerShape(50)).clickable { onCycleSort() }.padding(horizontal = 12.dp, vertical = 6.dp)) { Text(sortMode.label + " ↕", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelSmall) }
            }
        }
        items(libraryVideos, key = { "v_${it.uri}" }) { v ->
            val key = v.uri.toString(); val selected = selectedUris.contains(key)
            HoloCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF101828)), contentAlignment = Alignment.Center) {
                        val bmp = androidx.compose.runtime.produceState<android.graphics.Bitmap?>(initialValue = null, v.uri) { value = withContext(Dispatchers.IO) { ThumbnailLoader.load(context, v.uri, 200) } }.value
                        if (bmp!= null) { androidx.compose.foundation.Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) } else { Text("◎", color = NeonCyan.copy(0.6f)) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(v.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, fontWeight = FontWeight.Medium); Spacer(Modifier.height(4.dp)); Text("${fmt(v.durationMs)} • ${humanSize(v.sizeBytes)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f)) }
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).border(1.dp, GlassWhite10, RoundedCornerShape(50)).clickable { if (selectionMode) { selectedUris = if (selected) selectedUris - key else selectedUris + key } else { onOpenVideo(v.uri) } }, contentAlignment = Alignment.Center) { Text(if (selected) "✓" else "▶", color = if (selected) NeonCyan else Color.White) }
                }
            }
        }
    }
}

private fun fmt(ms: Long): String { val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60; return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec) }
private fun humanSize(bytes: Long): String { if (bytes <= 0) return "-"; val kb = bytes / 1024.0; val mb = kb / 1024.0; val gb = mb / 1024.0; return when { gb >= 1.0 -> "%.2f GB".format(gb); mb >= 1.0 -> "%.1f MB".format(mb); else -> "%.0f KB".format(kb) } }
