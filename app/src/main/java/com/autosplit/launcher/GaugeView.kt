package com.autosplit.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Dasar semua speedometer. Bagian statis (cincin, tick, angka) digambar sekali ke bitmap;
 * tiap frame hanya bagian dinamis (jarum, busur, angka digital). Animasi dibatasi ~30 fps
 * dan berhenti total saat kecepatan stabil, jadi hampir tidak membebani CPU.
 */
abstract class GaugeView(ctx: Context) : View(ctx) {

    var maxSpeed = 240f
        set(value) {
            field = value
            rebuildStatic()
        }

    var unit = "km/h"
        set(value) {
            field = value
            rebuildStatic()
        }

    protected var shown = 0f
    private var target = 0f
    private var animating = false

    protected var cx = 0f
    protected var cy = 0f
    protected var r = 0f

    private var cache: Bitmap? = null
    private val cachePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val arcRect = RectF()

    private val stepper = object : Runnable {
        override fun run() {
            val d = target - shown
            shown = if (abs(d) < 0.15f) target else shown + d * 0.28f
            invalidate()
            if (shown != target) postDelayed(this, FRAME_MS) else animating = false
        }
    }

    fun setSpeed(v: Float) {
        target = v.coerceIn(0f, maxSpeed)
        if (!animating && shown != target) {
            animating = true
            postDelayed(stepper, FRAME_MS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (shown != target && !animating) {
            animating = true
            postDelayed(stepper, FRAME_MS)
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(stepper)
        animating = false
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        r = min(w, h) / 2f * 0.95f
        onGeometry()
        rebuildStatic()
    }

    protected fun rebuildStatic() {
        cache?.recycle()
        cache = null
        if (width <= 0 || height <= 0) return
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawStatic(Canvas(bmp))
        cache = bmp
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val bmp = cache
        if (bmp != null) c.drawBitmap(bmp, 0f, 0f, cachePaint)
        drawDynamic(c)
    }

    protected abstract fun onGeometry()
    protected abstract fun drawStatic(c: Canvas)
    protected abstract fun drawDynamic(c: Canvas)

    /** Sudut jarum: 135 derajat (kiri-bawah) searah jarum jam sejauh 270 derajat. */
    protected fun angleOf(speed: Float): Float =
        START_ANGLE + SWEEP * (speed / maxSpeed).coerceIn(0f, 1f)

    protected fun px(radius: Float, angleDeg: Float): Float =
        cx + radius * cos(Math.toRadians(angleDeg.toDouble())).toFloat()

    protected fun py(radius: Float, angleDeg: Float): Float =
        cy + radius * sin(Math.toRadians(angleDeg.toDouble())).toFloat()

    protected fun arc(c: Canvas, radius: Float, start: Float, sweep: Float, p: Paint) {
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        c.drawArc(arcRect, start, sweep, false, p)
    }

    protected fun drawCentered(c: Canvas, text: String, x: Float, y: Float, p: Paint) {
        c.drawText(text, x, y - (p.descent() + p.ascent()) / 2f, p)
    }

    protected fun paint(color: Int, style: Paint.Style, width: Float = 0f): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = style
            strokeWidth = width
        }

    companion object {
        const val START_ANGLE = 135f
        const val SWEEP = 270f
        const val FRAME_MS = 33L
    }
}
