package com.autosplit.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Menampilkan template ELIO Speedo (assets/speedo) dan mengirim data sensor ke API
 * ElioSpeedo/ElioBridge. Panggilan JS hanya dikirim saat nilainya berubah.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class SpeedoView(ctx: Context) : WebView(ctx) {

    private var ready = false
    private var style = "lfa"
    private var c1 = "#00E5FF"
    private var c2 = "#A855F7"
    private var eco = false

    private var lastSpeed = -1
    private var lastG = Int.MIN_VALUE
    private var lastLive = ""

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                ready = true
                applyAll()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
        }
        loadUrl("file:///android_asset/speedo/index.html")
    }

    fun configure(styleIndex: Int, color1: Int, color2: Int, ecoMode: Boolean) {
        style = STYLES[styleIndex.coerceIn(0, STYLES.size - 1)]
        c1 = hex(color1)
        c2 = hex(color2)
        eco = ecoMode
        applyAll()
    }

    fun setSpeed(kmh: Float) {
        val v = kmh.roundToInt().coerceIn(0, 260)
        if (v == lastSpeed) return
        lastSpeed = v
        js("ElioSpeedo.setSpeed($v)")
    }

    fun setG(g: Float) {
        val q = (g * 100f).roundToInt()
        if (q == lastG) return
        lastG = q
        js("ElioSpeedo.setGForce(${String.format(Locale.US, "%.2f", q / 100f)})")
    }

    /** [json] dibuat dengan org.json sehingga sudah ter-escape. */
    fun live(json: String) {
        if (json == lastLive) return
        lastLive = json
        js("ElioBridge.live($json)")
    }

    private fun applyAll() {
        if (!ready) return
        js("ElioSpeedo.setStyle('$style');ElioSpeedo.setGradient('$c1','$c2');ElioSpeedo.setEco($eco);")
        lastSpeed = -1
        lastG = Int.MIN_VALUE
        lastLive = ""
    }

    private fun js(code: String) {
        if (ready) evaluateJavascript(code, null)
    }

    private fun hex(c: Int): String = String.format(Locale.US, "#%06X", 0xFFFFFF and c)

    companion object {
        val STYLES = arrayOf("lfa", "m3", "mazda", "honda")
    }
}
