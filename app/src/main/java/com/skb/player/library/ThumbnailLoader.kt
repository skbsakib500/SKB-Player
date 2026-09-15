package com.skb.player.library

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache

object ThumbnailLoader {
    private val cache = LruCache<String, Bitmap>(80)

    fun load(context: Context, uri: Uri, sizePx: Int = 256): Bitmap? {
        val key = "${uri}_$sizePx"
        cache.get(key)?.let { return it }
        return try {
            val bmp = context.contentResolver.loadThumbnail(
                uri, android.util.Size(sizePx, sizePx), null
            )
            cache.put(key, bmp)
            bmp
        } catch (_: Exception) { null }
    }
}
