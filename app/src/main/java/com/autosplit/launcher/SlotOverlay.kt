package com.autosplit.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.View

/**
 * Sudut aksen tebal di sekeliling jendela aplikasi (koordinat layar) plus label, digambar di
 * belakang jendela sehingga bingkai putih jendela terlihat satu desain dengan kartu speedometer.
 * Slot tanpa bingkai hanya menggambar label. Tidak menangkap sentuhan.
 */
class SlotOverlay(ctx: Context) : View(ctx) {

    class Slot(val rect: Rect, val label: String, val frame: Boolean = true)

    private var slots: List<Slot> = emptyList()
    private val d = ctx.resources.displayMetrics.density
    private val loc = IntArray(2)

    private val bracket = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = maxOf(3f, d * 3.2f)
        strokeCap = Paint.Cap.SQUARE
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * d
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        letterSpacing = 0.12f
    }

    fun setSlots(list: List<Slot>) {
        slots = list
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        if (slots.isEmpty()) return
        getLocationOnScreen(loc)
        bracket.color = Pal.accent
        text.color = Pal.accent
        val pad = PAD_DP * d
        val strip = STRIP_DP * d
        val len = 22f * d
        for (s in slots) {
            val labelX = s.rect.left - loc[0] + pad
            val labelY = s.rect.top - loc[1] - strip * 0.3f
            if (s.frame) {
                val l = s.rect.left - loc[0] - pad
                val r = s.rect.right - loc[0] + pad
                val t = s.rect.top - loc[1] - strip
                val b = s.rect.bottom - loc[1] + pad
                corner(c, l, t, len, 1f, 1f)
                corner(c, r, t, len, -1f, 1f)
                corner(c, l, b, len, 1f, -1f)
                corner(c, r, b, len, -1f, -1f)
            }
            c.drawText(s.label, labelX, labelY, text)
        }
    }

    private fun corner(c: Canvas, x: Float, y: Float, len: Float, dx: Float, dy: Float) {
        c.drawLine(x, y, x + len * dx, y, bracket)
        c.drawLine(x, y, x, y + len * dy, bracket)
    }

    companion object {
        const val PAD_DP = 6f
        const val STRIP_DP = 22f
    }
}
