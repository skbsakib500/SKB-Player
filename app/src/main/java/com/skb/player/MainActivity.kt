package com.skb.player

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.skb.player.library.BookmarkManager
import com.skb.player.library.EqualizerManager
import com.skb.player.library.FavoritesManager
import com.skb.player.library.HistoryManager
import com.skb.player.library.PlaylistManager
import com.skb.player.library.SettingsManager
import com.skb.player.library.VideoItem
import com.skb.player.ui.HistoryScreen
import com.skb.player.ui.HomeScaffold
import com.skb.player.ui.SKBTheme
import com.skb.player.ui.SearchScreen
import com.skb.player.ui.VideoPlayerScreen
import com.skb.player.ui.themeColors
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val incomingUri = MutableStateFlow<Uri?>(null)

    private lateinit var history: HistoryManager
    private lateinit var bookmarks: BookmarkManager
    private lateinit var settings: SettingsManager
    private lateinit var favorites: FavoritesManager
    private lateinit var playlists: PlaylistManager
    private val equalizer = EqualizerManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        history = HistoryManager(this)
        bookmarks = BookmarkManager(this)
        settings = SettingsManager(this)
        favorites = FavoritesManager(this)
        playlists = PlaylistManager(this)

        setContent {
            var themeIdx by remember { mutableIntStateOf(settings.themeIndex) }
            val theme = SKBTheme.entries.getOrElse(themeIdx) { SKBTheme.AMOLED }

            MaterialTheme(colorScheme = themeColors(theme)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    var controller by remember { mutableStateOf<MediaController?>(null) }

                    DisposableEffect(Unit) {
                        val token = SessionToken(
                            context,
                            ComponentName(context, PlaybackService::class.java)
                        )
                        val future = MediaController.Builder(context, token).buildAsync()
                        future.addListener({
                            try { controller = future.get() } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(context))
                        onDispose {
                            MediaController.releaseFuture(future)
                            controller = null
                        }
                    }

                    val ctrl = controller
                    if (ctrl == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val extUri by incomingUri.collectAsState()
                        MainContent(
                            controller = ctrl,
                            history = history,
                            bookmarks = bookmarks,
                            settings = settings,
                            favorites = favorites,
                            playlists = playlists,
                            equalizer = equalizer,
                            theme = theme,
                            externalUri = extUri,
                            onExternalUriConsumed = { incomingUri.value = null },
                            onThemeChange = {
                                settings.themeIndex = it
                                themeIdx = it
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { incomingUri.value = it }
            }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) incomingUri.value = uri
            }
        }
    }

    override fun onDestroy() {
        equalizer.release()
        super.onDestroy()
    }
}

@Composable
private fun MainContent(
    controller: MediaController,
    history: HistoryManager,
    bookmarks: BookmarkManager,
    settings: SettingsManager,
    favorites: FavoritesManager,
    playlists: PlaylistManager,
    equalizer: EqualizerManager,
    theme: SKBTheme,
    externalUri: Uri?,
    onExternalUriConsumed: () -> Unit,
    onThemeChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var startFrom by remember { mutableLongStateOf(0L) }
    var queueMode by remember { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPos by remember { mutableLongStateOf(0L) }
    var tab by remember { mutableIntStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var lastBackAt by remember { mutableLongStateOf(0L) }

    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler {
        when {
            showPlayer -> {
                try { controller.pause() } catch (_: Exception) {}
                showPlayer = false
                queueMode = false
                pickedUri = null
            }
            showSearch -> showSearch = false
            showHistory -> showHistory = false
            tab == 2 && selectedFolderId != null -> selectedFolderId = null
            tab != 0 -> {
                tab = 0
                selectedFolderId = null
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackAt < 2000L) {
                    context.findActivity()?.finish()
                } else {
                    lastBackAt = now
                    Toast.makeText(
                        context, "Press back again to exit", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun openSingleVideo(uri: Uri) {
        val saved = try { history.getPosition(uri) } catch (_: Exception) { 0L }
        if (saved > 30_000L) {
            pendingPos = saved
            pendingUri = uri
        } else {
            startFrom = 0L
            pickedUri = uri
            queueMode = false
            showPlayer = true
        }
    }

    LaunchedEffect(externalUri) {
        val u = externalUri ?: return@LaunchedEffect
        try { openSingleVideo(u) } catch (_: Exception) {}
        onExternalUriConsumed()
    }

    fun playAll(videos: List<VideoItem>) {
        if (videos.isEmpty()) return
        try {
            val items = videos.map { MediaItem.fromUri(it.uri) }
            controller.setMediaItems(items, 0, 0L)
            controller.prepare()
            controller.play()
            pickedUri = videos.first().uri
            startFrom = 0L
            queueMode = true
            showPlayer = true
        } catch (e: Exception) {
            Toast.makeText(context, "Queue failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun playUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        try {
            val items = uris.map { MediaItem.fromUri(it) }
            controller.setMediaItems(items, 0, 0L)
            controller.prepare()
            controller.play()
            pickedUri = uris.first()
            startFrom = 0L
            queueMode = true
            showPlayer = true
        } catch (e: Exception) {
            Toast.makeText(context, "Queue failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) openSingleVideo(uri) }

    when {
        showPlayer -> {
            VideoPlayerScreen(
                player = controller,
                uri = pickedUri,
                startFrom = startFrom,
                history = history,
                bookmarkManager = bookmarks,
                favorites = favorites,
                playlists = playlists,
                equalizer = equalizer,
                settings = settings,
                isQueueMode = queueMode,
                onBack = {
                    try { controller.pause() } catch (_: Exception) {}
                    showPlayer = false
                    queueMode = false
                    pickedUri = null
                },
                onEnterPip = {
                    val act = context.findActivity() ?: return@VideoPlayerScreen
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            act.enterPictureInPictureMode(params)
                        } catch (_: Exception) {}
                    }
                }
            )
        }
        showSearch -> {
            SearchScreen(
                history = history,
                onBack = { showSearch = false },
                onOpenVideo = { uri ->
                    showSearch = false
                    openSingleVideo(uri)
                }
            )
        }
        showHistory -> {
            HistoryScreen(
                history = history,
                onBack = { showHistory = false },
                onOpenVideo = { uri ->
                    showHistory = false
                    openSingleVideo(uri)
                }
            )
        }
        else -> {
            HomeScaffold(
                history = history,
                settings = settings,
                favorites = favorites,
                playlists = playlists,
                theme = theme,
                tab = tab,
                onTabChange = {
                    tab = it
                    if (it != 2) selectedFolderId = null
                },
                onOpenVideo = { openSingleVideo(it) },
                onPickVideo = { picker.launch(arrayOf("video/*")) },
                onPlayAll = { playAll(it) },
                onPlayUris = { playUris(it) },
                onOpenHistory = { showHistory = true },
                onOpenSearch = { showSearch = true },
                onThemeChange = onThemeChange,
                selectedFolderId = selectedFolderId,
                onSelectFolder = { selectedFolderId = it }
            )
        }
    }

    pendingUri?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                startFrom = 0L
                pickedUri = uri
                queueMode = false
                showPlayer = true
                pendingUri = null
            },
            title = { Text("Resume Playback?") },
            text = { Text("Continue from ${fmtTime(pendingPos)}?") },
            confirmButton = {
                TextButton(onClick = {
                    startFrom = pendingPos
                    pickedUri = uri
                    queueMode = false
                    showPlayer = true
                    pendingUri = null
                }) { Text("Resume") }
            },
            dismissButton = {
                TextButton(onClick = {
                    startFrom = 0L
                    pickedUri = uri
                    queueMode = false
                    showPlayer = true
                    pendingUri = null
                }) { Text("Start Over") }
            }
        )
    }
}

internal fun fmtTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
