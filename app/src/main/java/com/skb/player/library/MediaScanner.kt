package com.skb.player.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class VideoItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long
)

object MediaScanner {

    fun scanVideos(context: Context, limit: Int = 200): List<VideoItem> {
        val result = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )
        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            var count = 0
            while (c.moveToNext() && count < limit) {
                val dur = c.getLong(durCol)
                if (dur <= 0L) continue
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
                result.add(
                    VideoItem(
                        uri = uri,
                        name = c.getString(nameCol) ?: "video",
                        durationMs = dur,
                        sizeBytes = c.getLong(sizeCol)
                    )
                )
                count++
            }
        }
        return result
    }
}
