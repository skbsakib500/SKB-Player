package com.skb.player.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class VideoItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val dateAddedSec: Long,
    val bucketId: String,
    val bucketName: String,
    val relativePath: String
)

data class FolderItem(
    val bucketId: String,
    val name: String,
    val count: Int,
    val coverUri: Uri
)

object MediaScanner {

    private val PROJECTION = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.RELATIVE_PATH
    )

    fun scanVideos(context: Context, limit: Int = 2000): List<VideoItem> {
        val result = mutableListOf<VideoItem>()
        val cursor = try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                PROJECTION, null, null,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
            )
        } catch (_: Exception) { null }

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val bidCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bnameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
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
                        sizeBytes = c.getLong(sizeCol),
                        width = c.getInt(wCol),
                        height = c.getInt(hCol),
                        mimeType = c.getString(mimeCol) ?: "video/*",
                        dateAddedSec = c.getLong(dateCol),
                        bucketId = c.getString(bidCol) ?: "",
                        bucketName = c.getString(bnameCol) ?: "Unknown",
                        relativePath = c.getString(pathCol) ?: ""
                    )
                )
                count++
            }
        }
        return result
    }

    fun foldersFrom(videos: List<VideoItem>): List<FolderItem> =
        videos.groupBy { it.bucketId }
            .filter { it.key.isNotBlank() }
            .map { (id, list) ->
                FolderItem(
                    bucketId = id,
                    name = list.first().bucketName,
                    count = list.size,
                    coverUri = list.first().uri
                )
            }
            .sortedBy { it.name.lowercase() }

    fun videosInFolder(videos: List<VideoItem>, bucketId: String): List<VideoItem> =
        videos.filter { it.bucketId == bucketId }
            .sortedBy { it.name.lowercase() }
}
