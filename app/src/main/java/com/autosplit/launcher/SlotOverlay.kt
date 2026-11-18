package com.autosplit.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.View

/**
 * Bingkai sudut neon tipis di sekeliling jendela aplikasi (koordinat layar), lengkap dengan
 * label. Digambar di belakang jendela aplikasi, jadi tepi bawaan sistem terlihat "dibingkai"
 * oleh desain yang sama dengan speedometer. Tidak menangkap sentuhan.
 */
class SlotOverlay(ctx: Context) : View(ctx) {

    class Slot(val rect: Rect, val label: String)

    private var slots: List<Slot> = emptyList()
    private val d = ctx.resources.displayMetrics.density
    private val loc = IntArray(2)

    private val bracket = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Pal.CYAN
        style = Paint.Style.STROKE
        strokeWidth = maxOf(1f, d * 1.5f)
        strokeCap = Paint.Cap.SQUARE
    }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(46, 0, 229, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Pal.CYAN
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
        val pad = PAD_DP * d
        val strip = STRIP_DP * d
        val len = 22f * d
        for (s in slots) {
            val l = s.rect.left - loc[0] - pad
            val r = s.rect.right - loc[0] + pad
            val t = s.rect.top - loc[1] - strip
            val b = s.rect.bottom - loc[1] + pad
            c.drawRect(l, t, r, b, edge)
            corner(c, l, t, len, 1f, 1f)
            corner(c, r, t, len, -1f, 1f)
            corner(c, l, b, len, 1f, -1f)
            corner(c, r, b, len, -1f, -1f)
            c.drawText(s.label, l + pad * 2f, t + strip * 0.7f, text)
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
