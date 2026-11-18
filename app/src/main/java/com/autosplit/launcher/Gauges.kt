package com.autosplit.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Gaya BMW: cincin bulat gelap, angka putih, jarum oranye, busur kemajuan biru listrik. */
class BmwGaugeView(ctx: Context) : GaugeView(ctx) {

    private val disc = paint(Color.BLACK, Paint.Style.FILL)
    private val ring = paint(Color.parseColor("#C9D3E0"), Paint.Style.STROKE)
    private val tickMajor = paint(Color.WHITE, Paint.Style.STROKE)
    private val tickMinor = paint(Color.parseColor("#6C7A8C"), Paint.Style.STROKE)
    private val num = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val track = paint(Color.parseColor("#16233A"), Paint.Style.STROKE)
    private val glow = paint(Color.parseColor("#402F86FF"), Paint.Style.STROKE)
    private val arcLit = paint(Color.parseColor("#2F86FF"), Paint.Style.STROKE)
    private val needle = paint(Color.parseColor("#FF5A1F"), Paint.Style.FILL)
    private val needleGlow = paint(Color.parseColor("#55FF5A1F"), Paint.Style.STROKE)
    private val hub = paint(Color.parseColor("#0B0F16"), Paint.Style.FILL)
    private val hubRing = paint(Color.parseColor("#C9D3E0"), Paint.Style.STROKE)
    private val digits = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    private val unitP = paint(Color.parseColor("#8FA0B5"), Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
    }
    private val needlePath = Path()

    override fun onGeometry() {
        ring.strokeWidth = r * 0.022f
        tickMajor.strokeWidth = r * 0.014f
        tickMinor.strokeWidth = r * 0.007f
        track.strokeWidth = r * 0.022f
        glow.strokeWidth = r * 0.06f
        arcLit.strokeWidth = r * 0.022f
        needleGlow.strokeWidth = r * 0.035f
        hubRing.strokeWidth = r * 0.012f
        num.textSize = r * 0.085f
        digits.textSize = r * 0.30f
        unitP.textSize = r * 0.07f
        disc.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.parseColor("#121A26"), Color.parseColor("#05070D")),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        needlePath.reset()
        needlePath.moveTo(cx - r * 0.14f, cy - r * 0.014f)
        needlePath.lineTo(cx + r * 0.80f, cy - r * 0.004f)
        needlePath.lineTo(cx + r * 0.80f, cy + r * 0.004f)
        needlePath.lineTo(cx - r * 0.14f, cy + r * 0.014f)
        needlePath.close()
    }

    override fun drawStatic(c: Canvas) {
        c.drawCircle(cx, cy, r, disc)
        c.drawCircle(cx, cy, r - ring.strokeWidth / 2f, ring)
        arc(c, r * 0.915f, START_ANGLE, SWEEP, track)
        val n = (maxSpeed / 10f).toInt()
        for (i in 0..n) {
            val a = START_ANGLE + SWEEP * i / n
            val major = i % 2 == 0
            val outer = r * 0.87f
            val inner = if (major) r * 0.77f else r * 0.82f
            c.drawLine(px(outer, a), py(outer, a), px(inner, a), py(inner, a), if (major) tickMajor else tickMinor)
            if (major) drawCentered(c, (i * 10).toString(), px(r * 0.66f, a), py(r * 0.66f, a), num)
        }
    }

    override fun drawDynamic(c: Canvas) {
        val sweep = SWEEP * (shown / maxSpeed).coerceIn(0f, 1f)
        if (sweep > 0.5f) {
            arc(c, r * 0.915f, START_ANGLE, sweep, glow)
            arc(c, r * 0.915f, START_ANGLE, sweep, arcLit)
        }
        c.save()
        c.rotate(angleOf(shown), cx, cy)
        c.drawLine(cx + r * 0.12f, cy, cx + r * 0.80f, cy, needleGlow)
        c.drawPath(needlePath, needle)
        c.restore()
        c.drawCircle(cx, cy, r * 0.06f, hub)
        c.drawCircle(cx, cy, r * 0.06f, hubRing)
        drawCentered(c, shown.roundToInt().toString(), cx, cy + r * 0.46f, digits)
        drawCentered(c, unit, cx, cy + r * 0.64f, unitP)
    }
}

/** Gaya Honda: batang segmen LED melengkung, angka digital besar di tengah. */
class HondaGaugeView(ctx: Context) : GaugeView(ctx) {

    private val segCount = 60
    private val disc = paint(Color.parseColor("#0A1018"), Paint.Style.FILL)
    private val innerLine = paint(Color.parseColor("#1E2B3A"), Paint.Style.STROKE)
    private val segOff = paint(Color.parseColor("#17212C"), Paint.Style.STROKE).apply { strokeCap = Paint.Cap.BUTT }
    private val segLit = paint(Color.parseColor("#2BD9FF"), Paint.Style.STROKE).apply { strokeCap = Paint.Cap.BUTT }
    private val glowP = paint(Color.parseColor("#262BD9FF"), Paint.Style.STROKE)
    private val num = paint(Color.parseColor("#7C8DA0"), Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val digits = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    private val unitP = paint(Color.parseColor("#8FA0B5"), Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
    }

    private val ringR get() = r * 0.86f
    private val step get() = SWEEP / segCount

    override fun onGeometry() {
        innerLine.strokeWidth = r * 0.006f
        segOff.strokeWidth = r * 0.11f
        segLit.strokeWidth = r * 0.11f
        glowP.strokeWidth = r * 0.22f
        num.textSize = r * 0.075f
        digits.textSize = r * 0.58f
        unitP.textSize = r * 0.08f
    }

    override fun drawStatic(c: Canvas) {
        c.drawCircle(cx, cy, r, disc)
        c.drawCircle(cx, cy, r * 0.70f, innerLine)
        for (i in 0 until segCount) {
            arc(c, ringR, START_ANGLE + i * step, step * 0.70f, segOff)
        }
        val labelStep = if (maxSpeed > 200f) 40 else 20
        var v = 0
        while (v <= maxSpeed.toInt()) {
            val a = angleOf(v.toFloat())
            drawCentered(c, v.toString(), px(r * 0.62f, a), py(r * 0.62f, a), num)
            v += labelStep
        }
    }

    private fun colorFor(frac: Float): Int = when {
        frac < 0.62f -> Color.parseColor("#2BD9FF")
        frac < 0.82f -> Color.parseColor("#FFB020")
        else -> Color.parseColor("#FF3B4A")
    }

    override fun drawDynamic(c: Canvas) {
        val lit = ceil((shown / maxSpeed * segCount).toDouble()).toInt().coerceIn(0, segCount)
        if (lit > 0) {
            arc(c, ringR, START_ANGLE, lit * step - step * 0.3f, glowP)
            for (i in 0 until lit) {
                segLit.color = colorFor(i / segCount.toFloat())
                arc(c, ringR, START_ANGLE + i * step, step * 0.70f, segLit)
            }
        }
        drawCentered(c, shown.roundToInt().toString(), cx, cy, digits)
        drawCentered(c, unit, cx, cy + r * 0.36f, unitP)
    }
}

/** Gaya Toyota/Lexus: cincin krom, angka putih di dalam tick, jarum merah menyilang ke hub tengah. */
class LexusGaugeView(ctx: Context) : GaugeView(ctx) {

    private val face = paint(Color.BLACK, Paint.Style.FILL)
    private val chrome = paint(Color.WHITE, Paint.Style.STROKE)
    private val innerRing = paint(Color.parseColor("#2B3640"), Paint.Style.STROKE)
    private val track = paint(Color.parseColor("#12303A"), Paint.Style.STROKE)
    private val arcLit = paint(Color.parseColor("#7FE7FF"), Paint.Style.STROKE)
    private val tickMajor = paint(Color.WHITE, Paint.Style.STROKE)
    private val tickMinor = paint(Color.parseColor("#6C7A8C"), Paint.Style.STROKE)
    private val num = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val needle = paint(Color.parseColor("#FF2D2D"), Paint.Style.FILL)
    private val needleGlow = paint(Color.parseColor("#55FF2D2D"), Paint.Style.STROKE)
    private val hub = paint(Color.parseColor("#0A0F15"), Paint.Style.FILL)
    private val hubRing = paint(Color.parseColor("#2B3640"), Paint.Style.STROKE)
    private val hubAccent = paint(Color.parseColor("#4000E5FF"), Paint.Style.STROKE)
    private val digits = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    private val unitP = paint(Color.parseColor("#8FA0B5"), Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
    }
    private val needlePath = Path()

    override fun onGeometry() {
        chrome.strokeWidth = r * 0.04f
        innerRing.strokeWidth = r * 0.01f
        track.strokeWidth = r * 0.014f
        arcLit.strokeWidth = r * 0.014f
        tickMajor.strokeWidth = r * 0.012f
        tickMinor.strokeWidth = r * 0.006f
        needleGlow.strokeWidth = r * 0.03f
        hubRing.strokeWidth = r * 0.012f
        hubAccent.strokeWidth = r * 0.008f
        num.textSize = r * 0.085f
        digits.textSize = r * 0.34f
        unitP.textSize = r * 0.07f
        chrome.shader = SweepGradient(
            cx, cy,
            intArrayOf(
                Color.parseColor("#E8EEF5"), Color.parseColor("#5B6672"),
                Color.parseColor("#E8EEF5"), Color.parseColor("#3A434D"),
                Color.parseColor("#E8EEF5")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        )
        face.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.parseColor("#10151C"), Color.parseColor("#040507")),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        needlePath.reset()
        needlePath.moveTo(cx + r * 0.30f, cy - r * 0.012f)
        needlePath.lineTo(cx + r * 0.89f, cy - r * 0.003f)
        needlePath.lineTo(cx + r * 0.89f, cy + r * 0.003f)
        needlePath.lineTo(cx + r * 0.30f, cy + r * 0.012f)
        needlePath.close()
    }

    override fun drawStatic(c: Canvas) {
        c.drawCircle(cx, cy, r * 0.96f, face)
        c.drawCircle(cx, cy, r * 0.96f, chrome)
        c.drawCircle(cx, cy, r * 0.57f, innerRing)
        arc(c, r * 0.905f, START_ANGLE, SWEEP, track)
        val n = (maxSpeed / 5f).toInt()
        for (i in 0..n) {
            val a = START_ANGLE + SWEEP * i / n
            val major = i % 4 == 0
            val outer = r * 0.87f
            val inner = if (major) r * 0.795f else r * 0.83f
            c.drawLine(px(outer, a), py(outer, a), px(inner, a), py(inner, a), if (major) tickMajor else tickMinor)
            if (major) drawCentered(c, (i * 5).toString(), px(r * 0.70f, a), py(r * 0.70f, a), num)
        }
    }

    override fun drawDynamic(c: Canvas) {
        val sweep = SWEEP * (shown / maxSpeed).coerceIn(0f, 1f)
        if (sweep > 0.5f) arc(c, r * 0.905f, START_ANGLE, sweep, arcLit)
        c.save()
        c.rotate(angleOf(shown), cx, cy)
        c.drawLine(cx + r * 0.45f, cy, cx + r * 0.89f, cy, needleGlow)
        c.drawPath(needlePath, needle)
        c.restore()
        c.drawCircle(cx, cy, r * 0.46f, hub)
        c.drawCircle(cx, cy, r * 0.46f, hubRing)
        c.drawCircle(cx, cy, r * 0.43f, hubAccent)
        drawCentered(c, shown.roundToInt().toString(), cx, cy - r * 0.03f, digits)
        drawCentered(c, unit, cx, cy + r * 0.22f, unitP)
    }
}
