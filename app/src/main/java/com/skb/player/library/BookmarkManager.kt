package com.skb.player.library

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Bookmark(
    val id: String,
    val uri: String,
    val positionMs: Long,
    val note: String,
    val createdAt: Long
)

class BookmarkManager(context: Context) {

    private val prefs = context.getSharedPreferences("skb_bookmarks", Context.MODE_PRIVATE)

    fun add(uri: Uri, positionMs: Long, note: String): Bookmark {
        val bm = Bookmark(
            id = UUID.randomUUID().toString(),
            uri = uri.toString(),
            positionMs = positionMs,
            note = note,
            createdAt = System.currentTimeMillis()
        )
        val all = loadAll().toMutableList()
        all.add(0, bm)
        saveAll(all)
        return bm
    }

    fun remove(id: String) {
        val all = loadAll().filterNot { it.id == id }
        saveAll(all)
    }

    fun listFor(uri: Uri): List<Bookmark> =
        loadAll().filter { it.uri == uri.toString() }
            .sortedBy { it.positionMs }

    fun countFor(uri: Uri): Int = listFor(uri).size

    private fun loadAll(): List<Bookmark> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Bookmark(
                    id = o.optString("id"),
                    uri = o.optString("uri"),
                    positionMs = o.optLong("pos"),
                    note = o.optString("note"),
                    createdAt = o.optLong("at")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveAll(list: List<Bookmark>) {
        val arr = JSONArray()
        list.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("uri", b.uri)
                put("pos", b.positionMs)
                put("note", b.note)
                put("at", b.createdAt)
            })
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
