package com.skb.player.core

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class PlayerEngine(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = false
    }
    fun setMedia(uri: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }
    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }
    fun release() { player.release() }
}
