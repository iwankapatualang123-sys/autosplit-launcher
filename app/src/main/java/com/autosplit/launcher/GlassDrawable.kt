package com.autosplit.launcher

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Kartu transparan bergaya kaca: isi tembus pandang, dua garis tepi tipis bergradien
 * (terang di kiri-atas, redup di kanan-bawah, lalu dibalik di garis dalam) sehingga terlihat
 * timbul 3D. Hanya 3 goresan per kartu dan tanpa blur, jadi tetap ringan.
 */
class GlassDrawable(
    density: Float,
    radiusDp: Float = 12f,
    private val accent: Boolean = false
) : Drawable() {

    private val radius = radiusDp * density
    private val line = maxOf(1f, density * 0.75f)
    private val inset = maxOf(2f, density * 2f)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = line
    }
    private val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = line
    }

    private val rectOuter = RectF()
    private val rectInner = RectF()

    override fun onBoundsChange(b: Rect) {
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        val top = if (accent) Color.argb(70, 0, 229, 255) else Color.argb(30, 150, 220, 255)
        val bottom = if (accent) Color.argb(26, 0, 229, 255) else Color.argb(8, 0, 229, 255)
        fill.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
        outer.shader = LinearGradient(
            0f, 0f, w, h,
            Color.argb(170, 255, 255, 255), Color.argb(30, 0, 229, 255),
            Shader.TileMode.CLAMP
        )
        inner.shader = LinearGradient(
            0f, 0f, w, h,
            Color.argb(28, 0, 229, 255), Color.argb(90, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        rectOuter.set(b.left + line / 2f, b.top + line / 2f, b.right - line / 2f, b.bottom - line / 2f)
        rectInner.set(
            b.left + inset + line / 2f, b.top + inset + line / 2f,
            b.right - inset - line / 2f, b.bottom - inset - line / 2f
        )
    }

    override fun draw(c: Canvas) {
        c.drawRoundRect(rectOuter, radius, radius, fill)
        c.drawRoundRect(rectOuter, radius, radius, outer)
        val r2 = maxOf(0f, radius - inset)
        c.drawRoundRect(rectInner, r2, r2, inner)
    }

    override fun setAlpha(alpha: Int) {
        fill.alpha = alpha
        outer.alpha = alpha
        inner.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fill.colorFilter = colorFilter
        outer.colorFilter = colorFilter
        inner.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
