package com.skb.player.library

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object FileOps {

    /**
     * Rename. Returns IntentSender if user consent needed (API 30+).
     * Caller should launch with StartIntentSenderForResult.
     */
    fun rename(
        context: Context,
        uri: Uri,
        newName: String
    ): RenameResult {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, newName)
            }
            val rows = context.contentResolver.update(uri, values, null, null)
            if (rows > 0) RenameResult.Success
            else RenameResult.Error("Rename returned 0 rows")
        } catch (e: android.app.RecoverableSecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RenameResult.NeedsConsent(e.userAction.actionIntent.intentSender)
            } else RenameResult.Error(e.message ?: "Security")
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pi = MediaStore.createWriteRequest(
                        context.contentResolver, listOf(uri)
                    )
                    RenameResult.NeedsConsent(pi.intentSender)
                } catch (e2: Exception) {
                    RenameResult.Error(e2.message ?: "Consent fail")
                }
            } else RenameResult.Error(e.message ?: "Security")
        } catch (e: Exception) {
            RenameResult.Error(e.message ?: "Unknown")
        }
    }

    fun applyRename(context: Context, uri: Uri, newName: String): RenameResult {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, newName)
            }
            val rows = context.contentResolver.update(uri, values, null, null)
            if (rows > 0) RenameResult.Success else RenameResult.Error("0 rows")
        } catch (e: Exception) {
            RenameResult.Error(e.message ?: "Unknown")
        }
    }

    fun delete(context: Context, uri: Uri): DeleteResult {
        return try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) DeleteResult.Success else DeleteResult.Error("0 rows")
        } catch (e: android.app.RecoverableSecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                DeleteResult.NeedsConsent(e.userAction.actionIntent.intentSender)
            } else DeleteResult.Error(e.message ?: "Security")
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pi = MediaStore.createDeleteRequest(
                        context.contentResolver, listOf(uri)
                    )
                    DeleteResult.NeedsConsent(pi.intentSender)
                } catch (e2: Exception) {
                    DeleteResult.Error(e2.message ?: "Consent fail")
                }
            } else DeleteResult.Error(e.message ?: "Security")
        } catch (e: Exception) {
            DeleteResult.Error(e.message ?: "Unknown")
        }
    }

    fun applyDelete(context: Context, uri: Uri): DeleteResult {
        return try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) DeleteResult.Success else DeleteResult.Error("0 rows")
        } catch (e: Exception) {
            DeleteResult.Error(e.message ?: "Unknown")
        }
    }
}

sealed class RenameResult {
    object Success : RenameResult()
    data class NeedsConsent(val intentSender: IntentSender) : RenameResult()
    data class Error(val msg: String) : RenameResult()
}

sealed class DeleteResult {
    object Success : DeleteResult()
    data class NeedsConsent(val intentSender: IntentSender) : DeleteResult()
    data class Error(val msg: String) : DeleteResult()
}
