package com.autosplit.launcher

import android.content.Context
import android.graphics.Color
import android.os.Build

object Pal {
    /** Warna aksen yang dipilih pengguna; diisi dari Prefs saat aplikasi dibuka. */
    var accent: Int = Color.parseColor("#00E5FF")

    val BG = Color.parseColor("#05070D")
    val CYAN: Int get() = accent
    val TEXT = Color.parseColor("#E8F1FA")
    val MUTED = Color.parseColor("#8A9BAE")
    val RED = Color.parseColor("#FF3B4A")
}

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("autosplit", Context.MODE_PRIVATE)

    var gaugeStyle: Int
        get() = sp.getInt("style", 0)
        set(v) = sp.edit().putInt("style", v).apply()

    var accent: Int
        get() = sp.getInt("accent", Color.parseColor("#00E5FF"))
        set(v) = sp.edit().putInt("accent", v).apply()

    var useMph: Boolean
        get() = sp.getBoolean("mph", false)
        set(v) = sp.edit().putBoolean("mph", v).apply()

    var panelA: String
        get() = sp.getString("panelA", DEFAULT_A) ?: DEFAULT_A
        set(v) = sp.edit().putString("panelA", v).apply()

    var panelB: String
        get() = sp.getString("panelB", DEFAULT_B) ?: DEFAULT_B
        set(v) = sp.edit().putString("panelB", v).apply()

    var leftPercent: Int
        get() = sp.getInt("left", 40)
        set(v) = sp.edit().putInt("left", v).apply()

    var panelCount: Int
        get() = sp.getInt("count", 3)
        set(v) = sp.edit().putInt("count", v).apply()

    var entertainmentLayout: Boolean
        get() = sp.getBoolean("hiburan", false)
        set(v) = sp.edit().putBoolean("hiburan", v).apply()

    var panelsActive: Boolean
        get() = sp.getBoolean("panelsactive", false)
        set(v) = sp.edit().putBoolean("panelsactive", v).apply()

    var autoDim: Boolean
        get() = sp.getBoolean("autodim", true)
        set(v) = sp.edit().putBoolean("autodim", v).apply()

    var keepScreenOn: Boolean
        get() = sp.getBoolean("keepon", true)
        set(v) = sp.edit().putBoolean("keepon", v).apply()

    var forceFreeform: Boolean
        get() = sp.getBoolean("forcefree", Build.MANUFACTURER.equals("samsung", ignoreCase = true))
        set(v) = sp.edit().putBoolean("forcefree", v).apply()

    var autoOnCharge: Boolean
        get() = sp.getBoolean("autocharge", true)
        set(v) = sp.edit().putBoolean("autocharge", v).apply()

    var autoStartPanels: Boolean
        get() = sp.getBoolean("autopanels", true)
        set(v) = sp.edit().putBoolean("autopanels", v).apply()

    companion object {
        const val DEFAULT_A = "com.google.android.apps.maps"
        const val DEFAULT_B = "com.google.android.youtube"
    }
}
