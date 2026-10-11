package com.skb.player.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
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
import androidx.compose.ui.graphics.asImageBitmap
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
    var sliderPos by remember { mutableFloatStateOf(0f) }
    var sliderDragging by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var speedIndex by remember { mutableIntStateOf(0) }
    var aspectIndex by remember { mutableIntStateOf(0) }
    var rotationMode by remember { mutableIntStateOf(0) }
    var subtitleUri by remember { mutableStateOf<Uri?>(null) }
    var subtitleEnabled by remember { mutableStateOf(true) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }

    // 3080 Immersive for Landscape
    DisposableEffect(isLandscape) {
        if (isLandscape) {
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        } else {
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        onDispose {}
    }

    LaunchedEffect(player) {
        while (true) {
            try {
                position = player.currentPosition; duration = player.duration.coerceAtLeast(1L); isPlaying = player.isPlaying
                if (!sliderDragging) sliderPos = position.toFloat()
            } catch (_: Exception) {}
            delay(300)
        }
    }

    LaunchedEffect(rotationMode) {
        try {
            activity?.requestedOrientation = when (rotationMode) {
                1 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; 2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player; useController = false
                    resizeMode = ASPECTS[aspectIndex]
                    playerViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // GESTURES
        if (!locked &&!inPip) {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                var mode = DragMode.NONE; var startX = 0f; var startY = 0f; var startB = 0f; var startV = 0f; var startP = 0L
                awaitEachGesture {
                    val down = awaitFirstDown(); startX = down.position.x; startY = down.position.y
                    startB = brightness; startV = volumeFrac; startP = try { player.currentPosition } catch (_: Exception) { 0L }
                    mode = DragMode.NONE
                    var dragged = false
                    while (true) {
                        val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull { it.id == down.id }?: break
                        if (ch.changedToUp()) break
                        if (ch.positionChanged()) {
                            dragged = true; ch.consume()
                            val dx = ch.position.x - startX; val dy = ch.position.y - startY
                            if (mode == DragMode.NONE && (abs(dx) > 14f || abs(dy) > 14f)) {
                                mode = when { abs(dx) > abs(dy) -> DragMode.SEEK; startX < size.width/2f -> DragMode.BRIGHTNESS; else -> DragMode.VOLUME }
                            }
                            when (mode) {
                                DragMode.BRIGHTNESS -> { brightness = (startB - dy/size.height).coerceIn(0.01f,1f); activity?.window?.let { w -> val lp = w.attributes; lp.screenBrightness = brightness; w.attributes = lp }; hudText = "Brightness ${(brightness*100).toInt()}%" }
                                DragMode.VOLUME -> { volumeFrac = (startV - dy/size.height).coerceIn(0f,1f); audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeFrac*maxVolume).toInt(),0); hudText = "Volume ${(volumeFrac*100).toInt()}%" }
                                DragMode.SEEK -> { try { player.seekTo((startP + dx/size.width*60000).coerceIn(0L, player.duration)) } catch(_:Exception){} }
                                else -> {}
                            }
                        }
                    }
                    if (!dragged) overlayVisible =!overlayVisible
                }
            })
        }

        // 3080 HUD CONTROLS - LANDSCAPE FIXED
        if (!inPip &&!locked && overlayVisible) {
            // TOP BAR
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color(0x88000000)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, GlassWhite10, RoundedCornerShape(50)).clickable { onBack() }.padding(horizontal = 14.dp, vertical = 6.dp)) { Text("‹ Back", color = Color.White) }
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassWhite10).padding(horizontal = 10.dp, vertical = 6.dp)) { Text(if (rotationMode==1) "Landscape" else if (rotationMode==2) "Portrait" else "Auto", color = Color.White, fontSize = 11.sp) }
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.clip(CircleShape).border(1.dp, NeonCyan.copy(0.5f), CircleShape).clickable { onEnterPip() }.padding(8.dp)) { Text("PiP", color = NeonCyan, fontSize = 11.sp) }
            }

            // CENTER CONTROLS - NEXT/PREV FIXED
            Row(modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = if(isLandscape) 48.dp else 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                // PREV
                Box(modifier = Modifier.size(52.dp).clip(CircleShape).border(1.dp, GlassWhite10, CircleShape).background(Color(0x33000000)).clickable {
                    try { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0) } catch(_:Exception){}
                }, contentAlignment = Alignment.Center) { Text("⏮", color = Color.White, fontSize = 20.sp) }
                // PLAY/PAUSE with Glow Ring
                Box(modifier = Modifier.size(84.dp).clip(CircleShape).border(2.dp, NeonCyan, CircleShape).background(NeonCyan.copy(0.18f)).clickable { try { if (player.isPlaying) player.pause() else player.play() } catch(_:Exception){} }, contentAlignment = Alignment.Center) {
                    Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 32.sp)
                }
                // NEXT
                Box(modifier = Modifier.size(52.dp).clip(CircleShape).border(1.dp, GlassWhite10, CircleShape).background(Color(0x33000000)).clickable {
                    try { if (player.hasNextMediaItem()) player.seekToNextMediaItem() } catch(_:Exception){}
                }, contentAlignment = Alignment.Center) { Text("⏭", color = Color.White, fontSize = 20.sp) }
            }

            // BOTTOM BAR - FIXED FOR LANDSCAPE
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xDD05080F)).padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${SPEEDS[speedIndex]}x", color = Color.White.copy(0.7f), modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, GlassWhite10, RoundedCornerShape(50)).clickable { speedIndex = (speedIndex+1)%SPEEDS.size; try{ player.setPlaybackSpeed(SPEEDS[speedIndex]) }catch(_:Exception){} }.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp)
                    Text(ASPECT_LABELS[aspectIndex], color = Color.White.copy(0.7f), modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, GlassWhite10, RoundedCornerShape(50)).clickable { aspectIndex = (aspectIndex+1)%ASPECTS.size; playerViewRef?.resizeMode = ASPECTS[aspectIndex] }.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp)
                    Text(if (rotationMode==0) "Auto Rot" else if (rotationMode==1) "Landscape" else "Portrait", color = NeonCyan, modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(50)).clickable { rotationMode = (rotationMode+1)%3 }.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp)
                    Text("EQ", color = NeonCyan, modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(50)).clickable { showEqDialog = true }.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Slider(value = sliderPos, onValueChange = { sliderPos = it; sliderDragging = true }, onValueChangeFinished = { try{ player.seekTo(sliderPos.toLong()) }catch(_:Exception){}; sliderDragging = false }, valueRange = 0f..duration.toFloat().coerceAtLeast(1f), colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = GlassWhite10))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(position), color = Color.White.copy(0.6f), fontSize = 11.sp)
                    Text(formatTime(duration), color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
            }
        }

        if (!inPip && hudVisible && hudText!= null) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp).clip(RoundedCornerShape(50)).background(Color(0xCC000000)).border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text(hudText!!, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (!inPip) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp).clip(CircleShape).border(1.dp, GlassWhite10, CircleShape).background(Color(0x66000000)).clickable { locked =!locked }.padding(10.dp), contentAlignment = Alignment.Center) {
                Text(if (locked) "🔒" else "🔓", fontSize = 14.sp)
            }
        }
    }
    if (showAudioDialog) { AudioTrackDialog(player = player, onDismiss = { showAudioDialog = false }) }
    if (showEqDialog) { EqualizerDialog(eq = equalizer, initialEnabled = settings.eqEnabled, onDismiss = { settings.eqEnabled = equalizer.enabled; showEqDialog = false }) }
}

@Composable private fun AudioTrackDialog(player: Player, onDismiss: () -> Unit) {
    val groups = try { player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO } } catch (_: Exception) { emptyList() }
    val group = groups.firstOrNull()
    if (group == null) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Audio") }, text = { Text("No alternate audio") }, confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }); return }
    val trackGroup = group.mediaTrackGroup
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Audio Track") }, text = { Column { for (i in 0 until group.length) { val format = group.getTrackFormat(i); val label = format.label?: format.language?: "Track ${i+1}"; TextButton(onClick = { try{ player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(trackGroup,i)).build() }catch(_:Exception){}; onDismiss() }) { Text(label) } } } }, confirmButton = {})
}
private fun formatTime(ms: Long): String { val s = ms/1000; val m = s/60; val sec = s%60; return "%d:%02d".format(m,sec) }
private tailrec fun Context.findActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.findActivity(); else -> null }
