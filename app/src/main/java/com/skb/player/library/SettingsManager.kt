package com.skb.player.library

import android.content.Context

class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("skb_settings", Context.MODE_PRIVATE)

    var defaultSpeedIndex: Int
        get() = prefs.getInt("speed", 1)
        set(v) = prefs.edit().putInt("speed", v).apply()

    var defaultSubtitleSize: Int
        get() = prefs.getInt("sub_size", 1)
        set(v) = prefs.edit().putInt("sub_size", v).apply()

    var defaultSubtitleProfile: Int
        get() = prefs.getInt("sub_profile", 0)
        set(v) = prefs.edit().putInt("sub_profile", v).apply()

    var autoplayQueue: Boolean
        get() = prefs.getBoolean("autoplay", true)
        set(v) = prefs.edit().putBoolean("autoplay", v).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keep_awake", true)
        set(v) = prefs.edit().putBoolean("keep_awake", v).apply()
}
