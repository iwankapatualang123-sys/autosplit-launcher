package com.autosplit.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.view.WindowManager

/**
 * Meluncurkan aplikasi ke panel. Dua jalur:
 * - freeform: tiap aplikasi diluncurkan dengan batas ukuran (launch bounds).
 * - split: aplikasi diluncurkan ke sisi seberang launcher (FLAG_ACTIVITY_LAUNCH_ADJACENT).
 */
class PanelEngine(private val ctx: Context, private val prefs: Prefs) {

    fun systemHasFreeform(): Boolean =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)

    fun freeformUsable(): Boolean = prefs.forceFreeform || systemHasFreeform()

    fun isInstalled(pkg: String): Boolean = ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    fun labelOf(pkg: String): String = try {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        "Pilih app"
    }

    @Suppress("DEPRECATION")
    private fun screenSize(): Point {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        wm.defaultDisplay.getRealSize(p)
        return p
    }

    /** Susunan Navigasi: A di atas (besar), B di bawah. Susunan Hiburan: sebaliknya. */
    fun boundsA(): Rect = when {
        prefs.panelCount < 3 -> full()
        prefs.entertainmentLayout -> lower()
        else -> upper()
    }

    fun boundsB(): Rect = if (prefs.entertainmentLayout) upper() else lower()

    private fun full(): Rect {
        val s = screenSize()
        return Rect(s.x * prefs.leftPercent / 100, 0, s.x, s.y)
    }

    private fun upper(): Rect {
        val s = screenSize()
        return Rect(s.x * prefs.leftPercent / 100, 0, s.x, s.y * 62 / 100)
    }

    private fun lower(): Rect {
        val s = screenSize()
        return Rect(s.x * prefs.leftPercent / 100, s.y * 62 / 100, s.x, s.y)
    }

    fun launch(pkg: String, bounds: Rect?, adjacent: Boolean): Boolean {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        var flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (adjacent) flags = flags or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
        intent.addFlags(flags)
        return try {
            val options = ActivityOptions.makeBasic()
            if (bounds != null) options.setLaunchBounds(bounds)
            ctx.startActivity(intent, options.toBundle())
            true
        } catch (e: Exception) {
            false
        }
    }
}
