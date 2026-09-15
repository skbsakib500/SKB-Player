package com.skb.player.library

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object FileOps {

    fun rename(context: Context, uri: Uri, newName: String): Result<String> = try {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, newName)
        }
        val rows = context.contentResolver.update(uri, values, null, null)
        if (rows > 0) Result.success(newName)
        else Result.failure(Exception("Rename returned 0 rows"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun delete(context: Context, uri: Uri): Result<Unit> = try {
        val rows = context.contentResolver.delete(uri, null, null)
        if (rows > 0) Result.success(Unit)
        else Result.failure(Exception("Delete returned 0 rows"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
