package com.skb.player

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.skb.player.library.BookmarkManager
import com.skb.player.library.HistoryManager
import com.skb.player.ui.HomeScaffold
import com.skb.player.ui.VideoPlayerScreen

class MainActivity : ComponentActivity() {

    private lateinit var history: HistoryManager
    private lateinit var bookmarks: BookmarkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        history = HistoryManager(this)
        bookmarks = BookmarkManager(this)

        setContent {
            MaterialTheme(colorScheme = skbAmoled()) {
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
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    } else {
                        MainContent(ctrl, history, bookmarks)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    controller: MediaController,
    history: HistoryManager,
    bookmarks: BookmarkManager
) {
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var startFrom by remember { mutableLongStateOf(0L) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPos by remember { mutableLongStateOf(0L) }
    var tab by remember { mutableIntStateOf(0) }
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
            pickedUri != null -> {
                try { controller.pause() } catch (_: Exception) {}
                pickedUri = null
            }
            tab != 0 -> tab = 0
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

    fun openVideo(uri: Uri) {
        val saved = try { history.getPosition(uri) } catch (_: Exception) { 0L }
        if (saved > 30_000L) {
            pendingPos = saved
            pendingUri = uri
        } else {
            startFrom = 0L
            pickedUri = uri
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) openVideo(uri) }

    val current = pickedUri
    if (current == null) {
        HomeScaffold(
            history = history,
            tab = tab,
            onTabChange = { tab = it },
            onOpenVideo = { openVideo(it) },
            onPickVideo = { picker.launch(arrayOf("video/*")) }
        )
    } else {
        VideoPlayerScreen(
            player = controller,
            uri = current,
            startFrom = startFrom,
            history = history,
            bookmarkManager = bookmarks,
            onBack = {
                try { controller.pause() } catch (_: Exception) {}
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

    pendingUri?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                startFrom = 0L
                pickedUri = uri
                pendingUri = null
            },
            title = { Text("Resume Playback?") },
            text = { Text("Continue from ${fmtTime(uri)}?") },
            confirmButton = {
                TextButton(onClick = {
                    startFrom = pendingPos
                    pickedUri = uri
                    pendingUri = null
                }) { Text("Resume") }
            },
            dismissButton = {
                TextButton(onClick = {
                    startFrom = 0L
                    pickedUri = uri
                    pendingUri = null
                }) { Text("Start Over") }
            }
        )
    }
}

private fun skbAmoled() = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF0F0F0F),
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF000000),
    onBackground = Color(0xFFEAEAEA),
    onSurface = Color(0xFFEAEAEA),
    onSurfaceVariant = Color(0xFFAAAAAA)
)

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
