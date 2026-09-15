package com.skb.player.library

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(
    val id: String,
    val name: String,
    val videoUris: List<String>
)

class PlaylistManager(context: Context) {
    private val prefs = context.getSharedPreferences("skb_playlists", Context.MODE_PRIVATE)

    fun create(name: String): Playlist {
        val id = "pl_${System.currentTimeMillis()}"
        val all = listAll().toMutableList()
        val pl = Playlist(id, name, emptyList())
        all.add(pl)
        saveAll(all)
        return pl
    }

    fun addVideo(playlistId: String, uri: Uri) {
        val all = listAll().map {
            if (it.id == playlistId) {
                if (it.videoUris.contains(uri.toString())) it
                else it.copy(videoUris = it.videoUris + uri.toString())
            } else it
        }
        saveAll(all)
    }

    fun removeVideo(playlistId: String, uri: Uri) {
        val all = listAll().map {
            if (it.id == playlistId) it.copy(videoUris = it.videoUris - uri.toString())
            else it
        }
        saveAll(all)
    }

    fun delete(playlistId: String) {
        saveAll(listAll().filterNot { it.id == playlistId })
    }

    fun rename(playlistId: String, newName: String) {
        saveAll(listAll().map {
            if (it.id == playlistId) it.copy(name = newName) else it
        })
    }

    fun listAll(): List<Playlist> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val vids = o.optJSONArray("videos") ?: JSONArray()
                val list = (0 until vids.length()).map { vids.optString(it) }
                Playlist(o.optString("id"), o.optString("name"), list)
            }
        } catch (_: Exception) { emptyList() }
    }

    fun get(id: String): Playlist? = listAll().firstOrNull { it.id == id }

    private fun saveAll(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("videos", JSONArray(p.videoUris))
            })
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
