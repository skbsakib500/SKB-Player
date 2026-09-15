package com.skb.player.library

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.media3.common.Player

class EqualizerManager {
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var audioSessionId: Int = -1

    var enabled: Boolean = false
        private set

    fun attach(sessionId: Int) {
        if (sessionId == audioSessionId) return
        release()
        audioSessionId = sessionId
        try {
            eq = Equalizer(0, sessionId).apply { this.enabled = enabled }
            bass = BassBoost(0, sessionId).apply { this.enabled = enabled }
            virt = Virtualizer(0, sessionId).apply { this.enabled = enabled }
        } catch (_: Exception) {}
    }

    fun setEnabled(on: Boolean) {
        enabled = on
        try {
            eq?.enabled = on
            bass?.enabled = on
            virt?.enabled = on
        } catch (_: Exception) {}
    }

    fun bandCount(): Int = try { eq?.numberOfBands?.toInt() ?: 0 } catch (_: Exception) { 0 }

    fun bandLevelRange(): Pair<Short, Short> = try {
        val r = eq?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        r[0] to r[1]
    } catch (_: Exception) { (-1500).toShort() to 1500.toShort() }

    fun bandCenterFreqHz(band: Int): Int = try {
        (eq?.getBandFreqRange(band.toShort())?.get(0)?.toInt() ?: 0) / 1000
    } catch (_: Exception) { 0 }

    fun getBandLevel(band: Int): Short = try {
        eq?.getBandLevel(band.toShort()) ?: 0
    } catch (_: Exception) { 0 }

    fun setBandLevel(band: Int, level: Short) {
        try { eq?.setBandLevel(band.toShort(), level) } catch (_: Exception) {}
    }

    fun setBassStrength(strength: Short) {
        try { bass?.setStrength(strength) } catch (_: Exception) {}
    }

    fun setVirtualizerStrength(strength: Short) {
        try { virt?.setStrength(strength) } catch (_: Exception) {}
    }

    fun usePreset(preset: Short) {
        try { eq?.usePreset(preset) } catch (_: Exception) {}
    }

    fun presetCount(): Int = try { eq?.numberOfPresets?.toInt() ?: 0 } catch (_: Exception) { 0 }

    fun presetName(idx: Short): String = try {
        eq?.getPresetName(idx) ?: "Preset $idx"
    } catch (_: Exception) { "Preset $idx" }

    fun release() {
        try { eq?.release() } catch (_: Exception) {}
        try { bass?.release() } catch (_: Exception) {}
        try { virt?.release() } catch (_: Exception) {}
        eq = null; bass = null; virt = null
        audioSessionId = -1
    }
}
