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
 * Kartu HUD: isi transparan, garis tepi putih 2dp (sama dengan garis jendela Maps/YouTube),
 * aksen sudut tebal berwarna, dan batang aksen di tengah sisi atas. Hanya beberapa goresan
 * garis per kartu, tanpa blur, jadi tetap ringan.
 */
class GlassDrawable(
    density: Float,
    radiusDp: Float = 4f,
    private val accentFill: Boolean = false
) : Drawable() {

    private val radius = radiusDp * density
    private val line = maxOf(2f, density * 2f)
    private val cornerLen = 18f * density
    private val cornerW = maxOf(3f, density * 3.2f)
    private val barLen = 56f * density

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = line
    }
    private val corner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = cornerW
        strokeCap = Paint.Cap.SQUARE
    }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = cornerW
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()

    override fun onBoundsChange(b: Rect) {
        val h = b.height().toFloat()
        val a = Pal.accent
        val top = if (accentFill) Color.argb(70, Color.red(a), Color.green(a), Color.blue(a)) else Color.argb(26, 150, 200, 255)
        val bottom = if (accentFill) Color.argb(24, Color.red(a), Color.green(a), Color.blue(a)) else Color.argb(6, 0, 229, 255)
        fill.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
        rect.set(b.left + line / 2f, b.top + line / 2f, b.right - line / 2f, b.bottom - line / 2f)
        val half = minOf(barLen, rect.width() * 0.3f) / 2f
        val cx = (rect.left + rect.right) / 2f
        bar.shader = LinearGradient(cx - half, 0f, cx + half, 0f, Pal.accent, Pal.accent2, Shader.TileMode.CLAMP)
    }

    override fun draw(c: Canvas) {
        c.drawRoundRect(rect, radius, radius, fill)
        outline.color = if (accentFill) Pal.accent else Color.argb(235, 255, 255, 255)
        c.drawRoundRect(rect, radius, radius, outline)

        corner.color = Pal.accent
        val l = rect.left
        val t = rect.top
        val r = rect.right
        val b = rect.bottom
        val len = minOf(cornerLen, rect.width() / 3f, rect.height() / 3f)
        c.drawLine(l, t, l + len, t, corner)
        c.drawLine(l, t, l, t + len, corner)
        c.drawLine(r, t, r - len, t, corner)
        c.drawLine(r, t, r, t + len, corner)
        c.drawLine(l, b, l + len, b, corner)
        c.drawLine(l, b, l, b - len, corner)
        c.drawLine(r, b, r - len, b, corner)
        c.drawLine(r, b, r, b - len, corner)

        bar.color = Pal.accent
        val half = minOf(barLen, rect.width() * 0.3f) / 2f
        val cx = (l + r) / 2f
        c.drawLine(cx - half, t, cx + half, t, bar)
    }

    override fun setAlpha(alpha: Int) {
        fill.alpha = alpha
        outline.alpha = alpha
        corner.alpha = alpha
        bar.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fill.colorFilter = colorFilter
        outline.colorFilter = colorFilter
        corner.colorFilter = colorFilter
        bar.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
