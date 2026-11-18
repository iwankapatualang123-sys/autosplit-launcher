package com.autosplit.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/**
 * Bingkai tebal polos berwarna aksen: satu bingkai penuh di tepi layar, dan bingkai utuh di
 * sekeliling tiap jendela aplikasi (koordinat layar) beserta labelnya. Digambar di belakang
 * jendela aplikasi. Tidak menangkap sentuhan.
 */
class SlotOverlay(ctx: Context) : View(ctx) {

    class Slot(val rect: Rect, val label: String, val frame: Boolean = true)

    private var slots: List<Slot> = emptyList()
    private val d = ctx.resources.displayMetrics.density
    private val loc = IntArray(2)
    private val box = RectF()

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = maxOf(3.5f, d * 3.5f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val screenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = maxOf(4f, d * 4f)
        strokeJoin = Paint.Join.ROUND
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
        framePaint.color = Pal.accent
        screenPaint.color = Pal.accent
        text.color = Pal.accent

        val sw = screenPaint.strokeWidth
        box.set(sw / 2f + 2f * d, sw / 2f + 2f * d, width - sw / 2f - 2f * d, height - sw / 2f - 2f * d)
        c.drawRoundRect(box, 18f * d, 18f * d, screenPaint)

        if (slots.isEmpty()) return
        getLocationOnScreen(loc)
        val pad = PAD_DP * d
        val strip = STRIP_DP * d
        val r = 14f * d
        for (s in slots) {
            if (s.frame) {
                box.set(
                    s.rect.left - loc[0] - pad,
                    s.rect.top - loc[1] - strip,
                    s.rect.right - loc[0] + pad,
                    s.rect.bottom - loc[1] + pad
                )
                c.drawRoundRect(box, r, r, framePaint)
            }
            c.drawText(s.label, s.rect.left - loc[0] + pad, s.rect.top - loc[1] - strip * 0.3f, text)
        }
    }

    companion object {
        const val PAD_DP = 6f
        const val STRIP_DP = 22f
    }
}
