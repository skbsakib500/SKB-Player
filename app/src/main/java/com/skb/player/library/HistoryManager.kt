package com.skb.player.library

import android.content.Context
import android.net.Uri

data class RecentEntry(
    val uri: Uri,
    val name: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long
)

class HistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("skb_history", Context.MODE_PRIVATE)

    fun savePosition(uri: Uri, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        val key = uri.toString()
        prefs.edit()
            .putLong("pos_$key", positionMs)
            .putLong("dur_$key", durationMs)
            .putLong("time_$key", System.currentTimeMillis())
            .putString("name_$key", displayName(uri))
            .apply()
        touchRecent(key)
    }

    fun getPosition(uri: Uri): Long = prefs.getLong("pos_${uri}", 0L)

    fun getRecent(limit: Int = 15): List<RecentEntry> {
        val raw = prefs.getString("recent_uris", "") ?: ""
        return raw.split("|").filter { it.isNotBlank() }.take(limit).mapNotNull { u ->
            val dur = prefs.getLong("dur_$u", 0L)
            if (dur <= 0L) null
            else RecentEntry(
                uri = Uri.parse(u),
                name = prefs.getString("name_$u", "video") ?: "video",
                positionMs = prefs.getLong("pos_$u", 0L),
                durationMs = dur,
                lastPlayedAt = prefs.getLong("time_$u", 0L)
            )
        }
    }

    private fun touchRecent(key: String) {
        val raw = prefs.getString("recent_uris", "") ?: ""
        val list = raw.split("|").filter { it.isNotBlank() && it != key }.toMutableList()
        list.add(0, key)
        prefs.edit().putString("recent_uris", list.take(20).joinToString("|")).apply()
    }

    private fun displayName(uri: Uri): String {
        val seg = uri.lastPathSegment ?: "video"
        return seg.substringAfterLast('/').take(60)
    }
}
