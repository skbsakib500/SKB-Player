package com.skb.player.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.skb.player.library.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

private enum class DragMode { NONE, BRIGHTNESS, VOLUME, SEEK }
private val SPEEDS = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
private val ASPECTS = listOf(AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioFrameLayout.RESIZE_MODE_FILL, AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
private val ASPECT_LABELS = listOf("Fit", "Fill", "Zoom")
private val SLEEP_OPTIONS = listOf(0, 15, 30, 45, 60)
private val PROFILE_NAMES = listOf("Cinema", "Anime", "Study", "Minimal")
private val POSITION_NAMES = listOf("Bottom", "Center", "Top")
private const val DOUBLE_TAP_MS = 320L
private const val DOUBLE_TAP_SLOP = 140f
private const val CENTER_ZONE = 130f
private data class SubtitleProfile(val size: Int, val position: Int, val bgColor: Int, val textColor: Int)
private fun profileFor(index: Int) = when (index) {
    1 -> SubtitleProfile(1, 0, 0xCCFFFFFF.toInt(), 0xFF000000.toInt())
    2 -> SubtitleProfile(1, 0, 0xB3000000.toInt(), 0xFF00E5FF.toInt())
    3 -> SubtitleProfile(0, 0, 0x00000000, 0xFFFFFFFF.toInt())
    else -> SubtitleProfile(2, 0, 0xB3000000.toInt(), 0xFFFFFFFF.toInt())
}

@Composable
fun VideoPlayerScreen(
    player: Player, uri: Uri?, startFrom: Long,
    history: HistoryManager, bookmarkManager: BookmarkManager,
    favorites: FavoritesManager, playlists: PlaylistManager, equalizer: EqualizerManager,
    settings: SettingsManager, isQueueMode: Boolean, onBack: () -> Unit, onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    val scope = rememberCoroutineScope()

    var locked by remember { mutableStateOf(false) }
    var overlayVisible by remember { mutableStateOf(true) }
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudVisible by remember { mutableStateOf(false) }
    var inPip by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 }?: 0.5f) }
    var volumeFrac by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var isPlaying by remember { mutableStateOf(false) }
    var queueIndex by remember { mutableIntStateOf(1) }
    var queueTotal by remember { mutableIntStateOf(1) }
    var sliderPos by remember { mutableFloatStateOf(0f) }
    var sliderDragging by remember { mutableStateOf(false) }
    var subtitleUri by remember { mutableStateOf<Uri?>(null) }
    var subtitleEnabled by remember { mutableStateOf(true) }
    var subtitleSize by remember { mutableIntStateOf(settings.defaultSubtitleSize) }
    var subtitleDelay by remember { mutableFloatStateOf(0f) }
    var subtitlePosition by remember { mutableIntStateOf(0) }
    var subtitleProfile by remember { mutableIntStateOf(settings.defaultSubtitleProfile) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var speedIndex by remember { mutableIntStateOf(settings.defaultSpeedIndex) }
    var aspectIndex by remember { mutableIntStateOf(0) }
    var rotationMode by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var zoomOffsetX by remember { mutableFloatStateOf(0f) }
    var zoomOffsetY by remember { mutableFloatStateOf(0f) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubDialog by remember { mutableStateOf(false) }
    var sleepMenuOpen by remember { mutableStateOf(false) }
    var sleepEndAt by remember { mutableLongStateOf(0L) }
    var sleepJob by remember { mutableStateOf<Job?>(null) }
    var showAddBookmark by remember { mutableStateOf(false) }
    var showBookmarkList by remember { mutableStateOf(false) }
    var bookmarkRefresh by remember { mutableIntStateOf(0) }
    var abRepeatA by remember { mutableLongStateOf(-1L) }
    var abRepeatB by remember { mutableLongStateOf(-1L) }
    var showEqDialog by remember { mutableStateOf(false) }
    var showSavePlaylist by remember { mutableStateOf(false) }
    var currentAudioSessionId by remember { mutableIntStateOf(-1) }

    val pipAvailable = remember { Build.VERSION.SDK_INT >= 26 && context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) }
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked -> if (picked!= null) subtitleUri = picked }

    if (!isQueueMode && uri!= null) {
        DisposableEffect(uri, subtitleUri) {
            try {
                val builder = MediaItem.Builder().setUri(uri)
                subtitleUri?.let { sub -> builder.setSubtitleConfigurations(listOf(MediaItem.SubtitleConfiguration.Builder(sub).setMimeType(detectSubtitleMime(sub)).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build())) }
                player.setMediaItem(builder.build()); player.prepare(); if (startFrom > 0L) player.seekTo(startFrom); player.play()
            } catch (_: Exception) {}
            onDispose { try { player.pause() } catch (_: Exception) {} }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (!playing) { try { val d = player.duration; val u = player.currentMediaItem?.localConfiguration?.uri; if (d > 0L && u!= null) history.savePosition(u, player.currentPosition, d) } catch (_: Exception) {} }
            }
        }
        player.addListener(listener); onDispose { player.removeListener(listener) }
    }

    DisposableEffect(Unit) {
        if (settings.keepScreenOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val originalOrient = activity?.requestedOrientation?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            try { val d = player.duration; val u = player.currentMediaItem?.localConfiguration?.uri; if (d > 0L && u!= null) history.savePosition(u, player.currentPosition, d) } catch (_: Exception) {}
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            try { activity?.requestedOrientation = originalOrient } catch (_: Exception) {}
        }
    }

    LaunchedEffect(player, subtitleEnabled) { try { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,!subtitleEnabled).build() } catch (_: Exception) {} }
    LaunchedEffect(aspectIndex) { playerViewRef?.resizeMode = ASPECTS[aspectIndex] }
    LaunchedEffect(rotationMode) { try { activity?.requestedOrientation = when (rotationMode) { 1 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; 2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED } } catch (_: Exception) {} }
    LaunchedEffect(brightness) { activity?.window?.let { w -> val lp = w.attributes; lp.screenBrightness = brightness.coerceIn(0.01f, 1f); w.attributes = lp } }
    LaunchedEffect(player) { while (true) { try { position = player.currentPosition; duration = player.duration.coerceAtLeast(1L); isPlaying = player.isPlaying; queueIndex = player.currentMediaItemIndex + 1; queueTotal = player.mediaItemCount.coerceAtLeast(1); if (!sliderDragging) sliderPos = position.toFloat() } catch (_: Exception) {}; delay(400) } }
    LaunchedEffect(player) { while (true) { delay(3000); try { val d = player.duration; val u = player.currentMediaItem?.localConfiguration?.uri; if (d > 0L && u!= null) { history.savePosition(u, player.currentPosition, d); if (player.isPlaying) history.addWatchMs(u, 3000L) } } catch (_: Exception) {} } }
    LaunchedEffect(activity) { while (true) { inPip = try { activity?.isInPictureInPictureMode == true } catch (_: Exception) { false }; delay(250) } }
    LaunchedEffect(player) { while (true) { try { val sessionId = try { val m = player.javaClass.getMethod("getAudioSessionId"); (m.invoke(player) as? Int)?: 0 } catch (_: Exception) { 0 }; if (sessionId!= 0 && sessionId!= currentAudioSessionId) { currentAudioSessionId = sessionId; equalizer.attach(sessionId) } } catch (_: Exception) {}; delay(2000) } }
    LaunchedEffect(abRepeatA, abRepeatB) { if (abRepeatA >= 0 && abRepeatB > abRepeatA) { while (true) { try { val pos = player.currentPosition; if (pos >= abRepeatB) player.seekTo(abRepeatA) } catch (_: Exception) {}; delay(300) } } }
    LaunchedEffect(hudText) { if (hudText!= null) { hudVisible = true; delay(850); hudVisible = false; delay(150); hudText = null } }
    LaunchedEffect(overlayVisible, inPip) { if (overlayVisible &&!inPip) { delay(3500); overlayVisible = false } }
    // IMMERSIVE for landscape
    DisposableEffect(isLandscape, overlayVisible) {
        if (isLandscape &&!inPip) {
            activity?.window?.decorView?.systemUiVisibility = if (overlayVisible) View.SYSTEM_UI_FLAG_VISIBLE else View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        onDispose {}
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); resizeMode = ASPECTS[aspectIndex]; try { val m = PlayerView::class.java.getMethod("setUseTextureView", java.lang.Boolean.TYPE); m.invoke(this, true) } catch (_: Exception) {}; playerViewRef = this } },
            update = { view -> view.subtitleView?.apply { val prof = profileFor(subtitleProfile); val sz = when (if (prof.size == subtitleSize) subtitleSize else subtitleSize) { 0 -> 0.04f; 1 -> 0.06f; else -> 0.08f }; setFractionalTextSize(sz); setStyle(CaptionStyleCompat(prof.textColor, prof.bgColor, 0x00000000, CaptionStyleCompat.EDGE_TYPE_NONE, 0x00000000, null)); visibility = if (subtitleEnabled) View.VISIBLE else View.GONE } },
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = zoomOffsetX, translationY = zoomOffsetY))

        if (!inPip &&!locked) {
            Box(modifier = Modifier.fillMaxSize().pointerInput(locked) {
                var mode = DragMode.NONE; var startX = 0f; var startY = 0f; var startBrightness = 0f; var startVolume = 0f; var startPos = 0L; var lastTapAt = 0L; var lastTapX = 0f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false); startX = down.position.x; startY = down.position.y; startBrightness = brightness; startVolume = volumeFrac; try { startPos = player.currentPosition } catch (_: Exception) {}; mode = DragMode.NONE; var dragged = false
                    while (true) {
                        val event = awaitPointerEvent(); val change = event.changes.firstOrNull { it.id == down.id }?: break; if (change.changedToUp()) { change.consume(); break }
                        if (change.positionChanged()) { dragged = true; change.consume(); val dx = change.position.x - startX; val dy = change.position.y - startY
                            if (mode == DragMode.NONE && (abs(dx) > 14f || abs(dy) > 14f)) { mode = when { abs(dx) > abs(dy) -> DragMode.SEEK; startX < size.width / 2f -> DragMode.BRIGHTNESS; else -> DragMode.VOLUME } }
                            when (mode) {
                                DragMode.BRIGHTNESS -> { brightness = (startBrightness - dy / size.height).coerceIn(0.01f, 1f); hudText = "Brightness ${(brightness * 100).toInt()}%" }
                                DragMode.VOLUME -> { volumeFrac = (startVolume - dy / size.height).coerceIn(0f, 1f); audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeFrac * maxVolume).toInt(), 0); hudText = "Volume ${(volumeFrac * 100).toInt()}%" }
                                DragMode.SEEK -> { try { val dur = player.duration.coerceAtLeast(0L); val target = (startPos + (dx / size.width * 60000).toLong()).coerceIn(0L, dur); player.seekTo(target); hudText = "Seek ${formatTime(target)}" } catch (_: Exception) {} }
                                DragMode.NONE -> {}
                            }
                        }
                    }
                    if (!dragged) {
                        val now = System.currentTimeMillis(); val isDouble = (now - lastTapAt) in 1..DOUBLE_TAP_MS && abs(down.position.x - lastTapX) < DOUBLE_TAP_SLOP
                        if (isDouble) {
                            val cx = size.width / 2f; val x = down.position.x
                            try { when { x < cx - CENTER_ZONE -> { player.seekTo((player.currentPosition - 10000).coerceAtLeast(0L)); hudText = "<< 10s" }; x > cx + CENTER_ZONE -> { player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration.coerceAtLeast(0L))); hudText = "10s >>" }; else -> { if (player.isPlaying) player.pause() else player.play(); hudText = if (player.isPlaying) "Playing" else "Paused" } } } catch (_: Exception) {}; lastTapAt = 0L
                        } else { lastTapAt = now; lastTapX = down.position.x; overlayVisible =!overlayVisible }
                    }
                }
            })
        }

        // 3080 CONTROLS - ALL FEATURES KEPT
        if (!inPip &&!locked && overlayVisible) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TOP BAR 1 - Back, Sub, etc
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xAA05080F)).padding(4.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("Back", color = Color.White) }
                    if (!isQueueMode) {
                        TextButton(onClick = { subtitlePicker.launch(arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*")) }) { Text("SUB", color = Color.White) }
                        TextButton(onClick = { showSubDialog = true }) { Text("Sub ⚙", color = Color.White) }
                        TextButton(onClick = { subtitleEnabled =!subtitleEnabled }) { Text(if (subtitleEnabled) "ON" else "OFF", color = if (subtitleEnabled) NeonCyan else Color.White) }
                    } else {
                        Text(" ▶ Queue $queueIndex / $queueTotal", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (pipAvailable) { TextButton(onClick = onEnterPip) { Text("PiP", color = NeonCyan) } }
                    TextButton(onClick = { rotationMode = (rotationMode + 1) % 3 }) { Text(when (rotationMode) { 1 -> "Landscape"; 2 -> "Portrait"; else -> "Auto" }, color = Color.White) }
                }
                // TOP BAR 2 - Smart features
                Row(modifier = Modifier.fillMaxWidth().background(Color(0x88000000).copy(alpha = 0.6f)).padding(horizontal = 4.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showAddBookmark = true }) { Text("📌 Add", color = Color(0xFFFFD54F)) }
                    TextButton(onClick = { bookmarkRefresh++; showBookmarkList = true }) {
                        val curUri = player.currentMediaItem?.localConfiguration?.uri
                        val count = if (curUri!= null) bookmarkManager.countFor(curUri) else 0
                        Text("📋 Notes ($count)", color = NeonCyan)
                    }
                    TextButton(onClick = { captureScreenshot(context, playerViewRef) { result -> when (result) { is ShotResult.Ok -> Toast.makeText(context, "Saved: ${result.path}", Toast.LENGTH_LONG).show(); is ShotResult.Error -> Toast.makeText(context, "Failed: ${result.msg}", Toast.LENGTH_SHORT).show() } } }) { Text("📸 Shot", color = Color.White) }
                    Spacer(Modifier.weight(1f))
                    val curUri = player.currentMediaItem?.localConfiguration?.uri
                    if (curUri!= null) {
                        val isFav = favorites.isFavorite(curUri)
                        TextButton(onClick = { favorites.toggleFavorite(curUri); hudText = if (isFav) "Removed fav" else "Added fav" }) { Text(if (isFav) "★" else "☆", color = Color(0xFFFFD54F)) }
                        TextButton(onClick = { showSavePlaylist = true }) { Text("📑", color = Color.White) }
                        val isLater = favorites.isWatchLater(curUri)
                        TextButton(onClick = { favorites.toggleWatchLater(curUri); hudText = if (isLater) "Removed Later" else "Added Later" }) { Text(if (isLater) "⏰" else "⏱", color = NeonCyan) }
                    }
                    TextButton(onClick = { showEqDialog = true }) { Text("EQ", color = NeonCyan) }
                }

                Spacer(Modifier.weight(1f))

                // CENTER 3080 PLAY CONTROLS WITH NEXT/PREV
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).border(1.dp, GlassWhite10, CircleShape).background(Color(0x33000000)).clickable { try { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0) } catch (_: Exception) {} }, contentAlignment = Alignment.Center) { Text("⏮", color = Color.White, fontSize = 20.sp) }
                    Box(modifier = Modifier.size(84.dp).clip(CircleShape).border(2.dp, NeonCyan, CircleShape).background(NeonCyan.copy(0.18f)).clickable { try { if (player.isPlaying) player.pause() else player.play() } catch (_: Exception) {} }, contentAlignment = Alignment.Center) { Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 30.sp) }
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).border(1.dp, GlassWhite10, CircleShape).background(Color(0x33000000)).clickable { try { if (player.hasNextMediaItem()) player.seekToNextMediaItem() } catch (_: Exception) {} }, contentAlignment = Alignment.Center) { Text("⏭", color = Color.White, fontSize = 20.sp) }
                }

                Spacer(Modifier.weight(1f))

                // BOTTOM CONTROLS - ALL FEATURES
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xDD05080F)).padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { speedIndex = (speedIndex + 1) % SPEEDS.size; try { player.setPlaybackSpeed(SPEEDS[speedIndex]) } catch (_: Exception) {}; hudText = "Speed ${SPEEDS[speedIndex]}x" }) { Text("${SPEEDS[speedIndex]}x", color = Color.White) }
                        TextButton(onClick = { showAudioDialog = true }) { Text("Audio", color = Color.White) }
                        TextButton(onClick = { aspectIndex = (aspectIndex + 1) % ASPECTS.size; hudText = "Aspect ${ASPECT_LABELS[aspectIndex]}" }) { Text(ASPECT_LABELS[aspectIndex], color = Color.White) }
                        TextButton(onClick = { sleepMenuOpen = true }) { Text(if (sleepEndAt > 0L) "Sleep *" else "Sleep", color = Color.White) }
                        TextButton(onClick = { when { abRepeatA < 0L -> { abRepeatA = position; abRepeatB = -1L; hudText = "A set" }; abRepeatB < 0L -> { if (position > abRepeatA) { abRepeatB = position; hudText = "A-B loop" } }; else -> { abRepeatA = -1L; abRepeatB = -1L; hudText = "A-B cleared" } } }) { Text(when { abRepeatA < 0L -> "A-B"; abRepeatB < 0L -> "A:${formatTime(abRepeatA)}"; else -> "Looping" }, color = if (abRepeatA >= 0L) Color(0xFFFFD54F) else Color.White) }
                    }
                    Slider(value = sliderPos, onValueChange = { sliderPos = it; sliderDragging = true }, onValueChangeFinished = { try { player.seekTo(sliderPos.toLong()) } catch (_: Exception) {}; sliderDragging = false }, valueRange = 0f..duration.toFloat().coerceAtLeast(1f), colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = GlassWhite10))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${formatTime(position)} / ${formatTime(duration)}", color = Color.White.copy(0.7f), fontSize = 12.sp)
                        Text("Fit", color = Color.White.copy(0.4f), fontSize = 11.sp, modifier = Modifier.clickable { aspectIndex = (aspectIndex + 1) % ASPECTS.size })
                    }
                }
            }
        }

        if (!inPip) {
            Column(modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp), horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier.clip(CircleShape).border(1.dp, GlassWhite10, CircleShape).background(Color(0x66000000)).clickable { locked =!locked; if (!locked) overlayVisible = true }.padding(10.dp)) { Text(if (locked) "🔒" else "🔓", fontSize = 14.sp) }
                if (zoom > 1.01f) { Spacer(Modifier.height(8.dp)); Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(NeonCyan.copy(0.2f)).border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(50)).clickable { zoom = 1f; zoomOffsetX = 0f; zoomOffsetY = 0f; hudText = "Zoom reset" }.padding(horizontal = 10.dp, vertical = 6.dp)) { Text("${"%.1f".format(zoom)}x ↺", color = NeonCyan, fontSize = 11.sp) } }
            }
        }

        if (!inPip && hudVisible && hudText!= null) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = if (isLandscape) 60.dp else 110.dp).clip(RoundedCornerShape(50)).background(Color(0xCC000000)).border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 8.dp)) { Text(hudText!!, color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }

    if (showAudioDialog) { AudioTrackDialog(player = player, onDismiss = { showAudioDialog = false }) }
    if (showSubDialog) {
        AlertDialog(onDismissRequest = { showSubDialog = false }, title = { Text("Subtitle Settings") },
            text = {
                Column {
                    Text("Delay: ${"%.1f".format(subtitleDelay)}s", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { subtitleDelay = (subtitleDelay - 0.5f).coerceIn(-5f, 5f) }) { Text("− 0.5s") }
                        TextButton(onClick = { subtitleDelay = 0f }) { Text("Reset") }
                        TextButton(onClick = { subtitleDelay = (subtitleDelay + 0.5f).coerceIn(-5f, 5f) }) { Text("+ 0.5s") }
                    }
                    Text("Position: ${POSITION_NAMES[subtitlePosition]}"); TextButton(onClick = { subtitlePosition = (subtitlePosition + 1) % POSITION_NAMES.size }) { Text("Cycle Position") }
                    Text("Size: ${listOf("S", "M", "L")[subtitleSize]}"); TextButton(onClick = { subtitleSize = (subtitleSize + 1) % 3 }) { Text("Cycle Size") }
                    Text("Profile: ${PROFILE_NAMES[subtitleProfile]}")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        PROFILE_NAMES.forEachIndexed { i, name -> TextButton(onClick = { subtitleProfile = i; val p = profileFor(i); subtitleSize = p.size; subtitlePosition = p.position; settings.defaultSubtitleProfile = i; hudText = "Profile: $name" }) { Text(name, color = if (subtitleProfile == i) NeonCyan else Color.White) } }
                    }
                }
            }, confirmButton = { TextButton(onClick = { showSubDialog = false }) { Text("Done") } })
    }
    if (sleepMenuOpen) {
        AlertDialog(onDismissRequest = { sleepMenuOpen = false }, title = { Text("Sleep Timer") },
            text = { Column { SLEEP_OPTIONS.forEach { min -> TextButton(onClick = { sleepJob?.cancel(); if (min == 0) { sleepEndAt = 0L; sleepJob = null; hudText = "Sleep off" } else { sleepEndAt = System.currentTimeMillis() + min * 60_000L; sleepJob = scope.launch { delay(min * 60_000L); try { player.pause() } catch (_: Exception) {}; sleepEndAt = 0L }; hudText = "Sleep in ${min}m" }; sleepMenuOpen = false }) { Text(if (min == 0) "Off" else "$min min") } } } }, confirmButton = {})
    }
    if (showAddBookmark) { AddBookmarkDialog(positionMs = position, onDismiss = { showAddBookmark = false }, onSave = { note -> try { val curUri = player.currentMediaItem?.localConfiguration?.uri; if (curUri!= null) { bookmarkManager.add(curUri, position, note); hudText = "Bookmark saved" } } catch (_: Exception) {} }) }
    if (showBookmarkList) {
        val curUri = player.currentMediaItem?.localConfiguration?.uri
        val list = remember(curUri, bookmarkRefresh) { if (curUri!= null) bookmarkManager.listFor(curUri) else emptyList() }
        BookmarksListDialog(bookmarks = list, onDismiss = { showBookmarkList = false }, onJump = { ms -> try { player.seekTo(ms) } catch (_: Exception) {} }, onDelete = { id -> bookmarkManager.remove(id); bookmarkRefresh++ })
    }
    if (showEqDialog) { EqualizerDialog(eq = equalizer, initialEnabled = settings.eqEnabled, onDismiss = { settings.eqEnabled = equalizer.enabled; showEqDialog = false }) }
    if (showSavePlaylist) {
        val curUri = player.currentMediaItem?.localConfiguration?.uri; val list = playlists.listAll()
        SaveToPlaylistDialog(playlists = list, onDismiss = { showSavePlaylist = false }, onCreateNew = { name -> if (curUri!= null && name.isNotBlank()) { val pl = playlists.create(name); playlists.addVideo(pl.id, curUri); hudText = "Added to ${pl.name}" } }, onAddTo = { pl -> if (curUri!= null) { playlists.addVideo(pl.id, curUri); hudText = "Added to ${pl.name}" } })
    }
}

@Composable private fun AudioTrackDialog(player: Player, onDismiss: () -> Unit) {
    val groups = try { player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO } } catch (_: Exception) { emptyList() }
    val group = groups.firstOrNull()
    if (group == null) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Audio Track") }, text = { Text("No alternate audio tracks.") }, confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }); return }
    val trackGroup = group.mediaTrackGroup
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Audio Track") }, text = { Column { for (i in 0 until group.length) { val format = group.getTrackFormat(i); val label = format.label?: format.language?: "Track ${i + 1}"; TextButton(onClick = { try { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(trackGroup, i)).build() } catch (_: Exception) {}; onDismiss() }) { Text(label) } } } }, confirmButton = {})
}
private sealed class ShotResult { data class Ok(val path: String) : ShotResult(); data class Error(val msg: String) : ShotResult() }
private fun captureScreenshot(context: Context, playerViewRef: PlayerView?, onResult: (ShotResult) -> Unit) {
    val pv = playerViewRef?: run { onResult(ShotResult.Error("Player not ready")); return }
    val targetView: View = pv.videoSurfaceView?: pv
    val w = targetView.width; val h = targetView.height
    if (w <= 0 || h <= 0) { onResult(ShotResult.Error("Video not visible")); return }
    val saveBitmap: (Bitmap) -> Unit = { bmp -> try { val dir = File(context.getExternalFilesDir(null), "screenshots"); dir.mkdirs(); val file = File(dir, "SKB_${System.currentTimeMillis()}.png"); FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; onResult(ShotResult.Ok(file.absolutePath)) } catch (e: Exception) { onResult(ShotResult.Error(e.message?: "save failed")) } }
    when (targetView) {
        is SurfaceView -> { val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); try { PixelCopy.request(targetView, bmp, { result -> if (result == PixelCopy.SUCCESS) saveBitmap(bmp) else onResult(ShotResult.Error("PixelCopy $result")) }, Handler(Looper.getMainLooper())) } catch (e: Exception) { onResult(ShotResult.Error(e.message?: "fail")) } }
        is TextureView -> { val bmp = try { targetView.bitmap } catch (_: Exception) { null }; if (bmp!= null) saveBitmap(bmp) else onResult(ShotResult.Error("Texture bitmap null")) }
        else -> { onResult(ShotResult.Error("Unsupported view")) }
    }
}
private fun detectSubtitleMime(uri: Uri): String { val name = uri.lastPathSegment?.lowercase()?: return MimeTypes.APPLICATION_SUBRIP; return when { name.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP; name.endsWith(".vtt") -> MimeTypes.TEXT_VTT; name.endsWith(".ass") -> MimeTypes.TEXT_SSA; name.endsWith(".ssa") -> MimeTypes.TEXT_SSA; else -> MimeTypes.APPLICATION_SUBRIP } }
private fun formatTime(ms: Long): String { val totalSec = ms / 1000; val m = totalSec / 60; val s = totalSec % 60; return "%d:%02d".format(m, s) }
private tailrec fun Context.findActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.findActivity(); else -> null }
