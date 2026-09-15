package com.skb.player.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.skb.player.library.HistoryManager
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class DragMode { NONE, BRIGHTNESS, VOLUME, SEEK }

private val SPEEDS = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
private val ASPECTS = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT,
    AspectRatioFrameLayout.RESIZE_MODE_FILL,
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
)
private val ASPECT_LABELS = listOf("Fit", "Fill", "Zoom")

@Composable
fun VideoPlayerScreen(
    player: ExoPlayer,
    uri: Uri,
    history: HistoryManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var locked by remember { mutableStateOf(false) }
    var overlayVisible by remember { mutableStateOf(true) }
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudVisible by remember { mutableStateOf(false) }

    var brightness by remember {
        val cur = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (cur < 0f) 0.5f else cur)
    }
    var volumeFrac by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
        )
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var isPlaying by remember { mutableStateOf(false) }

    var sliderPos by remember { mutableFloatStateOf(0f) }
    var sliderDragging by remember { mutableStateOf(false) }

    var subtitleUri by remember { mutableStateOf<Uri?>(null) }
    var subtitleEnabled by remember { mutableStateOf(true) }
    var subtitleSize by remember { mutableIntStateOf(1) }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var speedIndex by remember { mutableIntStateOf(1) }
    var aspectIndex by remember { mutableIntStateOf(0) }
    var rotationLocked by remember { mutableStateOf(false) }

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { picked -> if (picked != null) subtitleUri = picked }

    DisposableEffect(uri, subtitleUri) {
        val saved = history.getPosition(uri)
        val builder = MediaItem.Builder().setUri(uri)
        subtitleUri?.let { sub ->
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(sub)
                        .setMimeType(detectSubtitleMime(sub))
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        player.setMediaItem(builder.build())
        player.prepare()
        if (saved > 0L) player.seekTo(saved)
        player.play()
        onDispose { player.pause() }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val originalOrient = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            history.savePosition(
                uri,
                player.currentPosition,
                player.duration.coerceAtLeast(0L)
            )
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = originalOrient
        }
    }

    LaunchedEffect(subtitleEnabled) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitleEnabled)
            .build()
    }

    LaunchedEffect(speedIndex) {
        player.setPlaybackSpeed(SPEEDS[speedIndex])
    }

    LaunchedEffect(aspectIndex) {
        playerViewRef?.resizeMode = ASPECTS[aspectIndex]
    }

    LaunchedEffect(rotationLocked) {
        activity?.requestedOrientation = if (rotationLocked)
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    LaunchedEffect(brightness) {
        activity?.window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness = brightness.coerceIn(0.01f, 1f)
            w.attributes = lp
        }
    }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(1L)
            isPlaying = player.isPlaying
            if (!sliderDragging) sliderPos = position.toFloat()
            delay(400)
        }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(5_000)
            if (player.duration > 0) {
                history.savePosition(uri, player.currentPosition, player.duration)
            }
        }
    }

    LaunchedEffect(hudText) {
        if (hudText != null) {
            hudVisible = true
            delay(900)
            hudVisible = false
            delay(200)
            hudText = null
        }
    }

    LaunchedEffect(overlayVisible) {
        if (overlayVisible) {
            delay(3500)
            overlayVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = ASPECTS[aspectIndex]
                    playerViewRef = this
                }
            },
            update = { view ->
                view.subtitleView?.apply {
                    setStyle(CaptionStyleCompat.DEFAULT)
                    val sz = when (subtitleSize) {
                        0 -> 0.04f
                        1 -> 0.06f
                        else -> 0.08f
                    }
                    setFractionalTextSize(sz)
                    visibility = if (subtitleEnabled) View.VISIBLE else View.GONE
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    detectTapGestures(
                        onTap = { overlayVisible = !overlayVisible },
                        onDoubleTap = { offset ->
                            val w = size.width
                            val deltaMs = if (offset.x < w / 2) -10_000L else 10_000L
                            val target = (player.currentPosition + deltaMs)
                                .coerceIn(0L, player.duration.coerceAtLeast(0L))
                            player.seekTo(target)
                            hudText = if (deltaMs < 0) "<< 10s" else "10s >>"
                        }
                    )
                }
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    var mode = DragMode.NONE
                    var startX = 0f
                    var startY = 0f
                    var startBrightness = 0f
                    var startVolume = 0f
                    var startPos = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            mode = DragMode.NONE
                            startX = offset.x
                            startY = offset.y
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (mode == DragMode.NONE) {
                                val dx = change.position.x - startX
                                val dy = change.position.y - startY
                                mode = when {
                                    abs(dx) > abs(dy) -> DragMode.SEEK
                                    startX < size.width / 2f -> DragMode.BRIGHTNESS
                                    else -> DragMode.VOLUME
                                }
                                startBrightness = brightness
                                startVolume = volumeFrac
                                startPos = player.currentPosition
                            }
                            when (mode) {
                                DragMode.BRIGHTNESS -> {
                                    brightness = (startBrightness - dragAmount.y / size.height)
                                        .coerceIn(0.01f, 1f)
                                    hudText = "Brightness ${(brightness * 100).toInt()}%"
                                }
                                DragMode.VOLUME -> {
                                    volumeFrac = (startVolume - dragAmount.y / size.height)
                                        .coerceIn(0f, 1f)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        (volumeFrac * maxVolume).toInt(),
                                        0
                                    )
                                    hudText = "Volume ${(volumeFrac * 100).toInt()}%"
                                }
                                DragMode.SEEK -> {
                                    val deltaMs =
                                        ((change.position.x - startX) / size.width * 60_000).toLong()
                                    val target = (startPos + deltaMs)
                                        .coerceIn(0L, player.duration.coerceAtLeast(0L))
                                    player.seekTo(target)
                                    hudText = "Seek ${formatTime(target)}"
                                }
                                DragMode.NONE -> {}
                            }
                        },
                        onDragEnd = { mode = DragMode.NONE },
                        onDragCancel = { mode = DragMode.NONE }
                    )
                }
        )

        if (!locked && overlayVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back", color = Color.White) }
                TextButton(onClick = {
                    subtitlePicker.launch(arrayOf(
                        "application/x-subrip", "text/vtt", "text/plain", "*/*"
                    ))
                }) { Text("SUB", color = Color.White) }
                TextButton(onClick = { subtitleSize = (subtitleSize + 1) % 3 }) {
                    val l = when (subtitleSize) { 0 -> "S"; 1 -> "M"; else -> "L" }
                    Text(l, color = Color.White)
                }
                TextButton(onClick = { subtitleEnabled = !subtitleEnabled }) {
                    Text(if (subtitleEnabled) "ON" else "OFF", color = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0x88000000))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        speedIndex = (speedIndex + 1) % SPEEDS.size
                        hudText = "Speed ${SPEEDS[speedIndex]}x"
                    }) { Text("${SPEEDS[speedIndex]}x", color = Color.White) }

                    TextButton(onClick = {
                        aspectIndex = (aspectIndex + 1) % ASPECTS.size
                        hudText = "Aspect ${ASPECT_LABELS[aspectIndex]}"
                    }) { Text(ASPECT_LABELS[aspectIndex], color = Color.White) }

                    TextButton(onClick = {
                        rotationLocked = !rotationLocked
                        hudText = if (rotationLocked) "Rotation locked" else "Rotation free"
                    }) { Text(if (rotationLocked) "Lock" else "Free", color = Color.White) }

                    TextButton(onClick = {
                        player.repeatMode = when (player.repeatMode) {
                            androidx.media3.common.Player.REPEAT_MODE_OFF ->
                                androidx.media3.common.Player.REPEAT_MODE_ALL
                            androidx.media3.common.Player.REPEAT_MODE_ALL ->
                                androidx.media3.common.Player.REPEAT_MODE_ONE
                            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                        }
                        hudText = when (player.repeatMode) {
                            androidx.media3.common.Player.REPEAT_MODE_OFF -> "Repeat off"
                            androidx.media3.common.Player.REPEAT_MODE_ALL -> "Repeat all"
                            else -> "Repeat one"
                        }
                    }) { Text("Repeat", color = Color.White) }
                }

                Slider(
                    value = sliderPos,
                    onValueChange = {
                        sliderPos = it
                        sliderDragging = true
                    },
                    onValueChangeFinished = {
                        player.seekTo(sliderPos.toLong())
                        sliderDragging = false
                    },
                    valueRange = 0f..duration.toFloat()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                    }) { Text(if (isPlaying) "Pause" else "Play", color = Color.White) }
                    Text(
                        "${formatTime(position)} / ${formatTime(duration)}",
                        color = Color.White
                    )
                }
            }
        }

        TextButton(
            onClick = {
                locked = !locked
                if (!locked) overlayVisible = true
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(8.dp)
        ) { Text(if (locked) "LOCKED" else "LOCK", color = Color.White) }

        if (hudVisible && hudText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .background(Color(0xCC000000), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = hudText!!,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun detectSubtitleMime(uri: Uri): String {
    val name = uri.lastPathSegment?.lowercase() ?: return MimeTypes.APPLICATION_SUBRIP
    return when {
        name.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
        name.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        name.endsWith(".ass") -> MimeTypes.TEXT_SSA
        name.endsWith(".ssa") -> MimeTypes.TEXT_SSA
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
