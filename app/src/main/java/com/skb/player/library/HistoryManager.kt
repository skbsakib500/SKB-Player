package com.skb.player.library

import android.content.Context
import android.net.Uri

data class RecentEntry(
    val uri: Uri,
    val name: String,
    val positionMs: Long,
    val durationMs: Long,
    val watchMs: Long,
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

    fun addWatchMs(uri: Uri, ms: Long) {
        if (ms <= 0L) return
        val key = uri.toString()
        val cur = prefs.getLong("watch_$key", 0L)
        prefs.edit().putLong("watch_$key", cur + ms).apply()
    }

    fun getWatchMs(uri: Uri): Long = prefs.getLong("watch_${uri}", 0L)

    fun getPosition(uri: Uri): Long = prefs.getLong("pos_${uri}", 0L)

    fun getRecent(limit: Int = 100): List<RecentEntry> {
        val raw = prefs.getString("recent_uris", "") ?: ""
        return raw.split("|").filter { it.isNotBlank() }.take(limit).mapNotNull { u ->
            val dur = prefs.getLong("dur_$u", 0L)
            if (dur <= 0L) null
            else RecentEntry(
                uri = Uri.parse(u),
                name = prefs.getString("name_$u", "video") ?: "video",
                positionMs = prefs.getLong("pos_$u", 0L),
                durationMs = dur,
                watchMs = prefs.getLong("watch_$u", 0L),
                lastPlayedAt = prefs.getLong("time_$u", 0L)
            )
        }
    }

    fun remove(uri: Uri) {
        val key = uri.toString()
        val raw = prefs.getString("recent_uris", "") ?: ""
        val list = raw.split("|").filter { it.isNotBlank() && it != key }
        prefs.edit()
            .putString("recent_uris", list.joinToString("|"))
            .remove("pos_$key")
            .remove("dur_$key")
            .remove("time_$key")
            .remove("watch_$key")
            .remove("name_$key")
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun touchRecent(key: String) {
        val raw = prefs.getString("recent_uris", "") ?: ""
        val list = raw.split("|").filter { it.isNotBlank() && it != key }.toMutableList()
        list.add(0, key)
        prefs.edit().putString("recent_uris", list.take(200).joinToString("|")).apply()
    }

    private fun displayName(uri: Uri): String {
        val seg = uri.lastPathSegment ?: "video"
        return seg.substringAfterLast('/').take(80)
    }
}
