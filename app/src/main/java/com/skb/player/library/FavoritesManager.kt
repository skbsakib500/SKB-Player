package com.skb.player.library

import android.content.Context
import android.net.Uri

class FavoritesManager(context: Context) {
    private val prefs = context.getSharedPreferences("skb_favorites", Context.MODE_PRIVATE)

    fun toggleFavorite(uri: Uri) {
        val set = getFavorites().toMutableSet()
        if (!set.add(uri.toString())) set.remove(uri.toString())
        prefs.edit().putStringSet("fav", set).apply()
    }

    fun getFavorites(): Set<String> = prefs.getStringSet("fav", emptySet()) ?: emptySet()
    fun isFavorite(uri: Uri): Boolean = getFavorites().contains(uri.toString())

    fun toggleWatchLater(uri: Uri) {
        val set = getWatchLater().toMutableSet()
        if (!set.add(uri.toString())) set.remove(uri.toString())
        prefs.edit().putStringSet("later", set).apply()
    }

    fun getWatchLater(): Set<String> = prefs.getStringSet("later", emptySet()) ?: emptySet()
    fun isWatchLater(uri: Uri): Boolean = getWatchLater().contains(uri.toString())
}
