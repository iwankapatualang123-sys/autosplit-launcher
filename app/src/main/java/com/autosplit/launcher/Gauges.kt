package com.autosplit.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val RED = Color.parseColor("#FF3B4A")
private val AMBER = Color.parseColor("#FFB020")

/**
 * Gaya Lexus dan Mazda: kotak HUD digital di atas, jam bulat dengan busur tebal berwarna,
 * tick di dalam, jarum putih, dan pembacaan gaya G lateral di tengah.
 */
class RingGaugeView(ctx: Context, private val mazda: Boolean) : GaugeView(ctx) {

    private val accent = Pal.accent
    private val face = paint(Color.argb(170, 4, 8, 12), Paint.Style.FILL)
    private val ringThin = paint(Color.argb(150, 255, 255, 255), Paint.Style.STROKE)
    private val track = paint(withAlpha(accent, 55), Paint.Style.STROKE)
    private val arcP = paint(accent, Paint.Style.STROKE).apply { strokeCap = Paint.Cap.ROUND }
    private val glowP = paint(withAlpha(accent, 70), Paint.Style.STROKE).apply { strokeCap = Paint.Cap.ROUND }
    private val tickMajor = paint(Color.WHITE, Paint.Style.STROKE)
    private val tickMinor = paint(Color.argb(140, 255, 255, 255), Paint.Style.STROKE)
    private val tickRed = paint(RED, Paint.Style.STROKE)
    private val num = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val numRed = paint(RED, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val needleGlow = paint(withAlpha(accent, 90), Paint.Style.STROKE).apply { strokeCap = Paint.Cap.ROUND }
    private val needle = paint(Color.WHITE, Paint.Style.STROKE).apply { strokeCap = Paint.Cap.ROUND }
    private val hubP = paint(accent, Paint.Style.FILL)
    private val pillP = paint(RED, Paint.Style.STROKE)
    private val pillText = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val gText = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    private val label = paint(Pal.MUTED, Paint.Style.FILL).apply { textAlign = Paint.Align.CENTER }
    private val boxFill = paint(Color.argb(150, 0, 0, 0), Paint.Style.FILL)
    private val bracketP = paint(accent, Paint.Style.STROKE)
    private val barP = paint(accent, Paint.Style.STROKE).apply { strokeCap = Paint.Cap.ROUND }
    private val digits = italicDigits()
    private val unitP = paint(accent, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val titleP = paint(Pal.MUTED, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.15f
    }

    private val hud = RectF()
    private val pill = RectF()

    override fun onGeometry() {
        val w = width.toFloat()
        val h = height.toFloat()
        val hudH = h * 0.27f
        val hudW = minOf(w * 0.72f, hudH * 2.7f)
        hud.set(cx - hudW / 2f, h * 0.02f, cx + hudW / 2f, h * 0.02f + hudH)
        val gap = h * 0.03f
        val avail = h - hud.bottom - gap - h * 0.02f
        r = minOf(w / 2f, avail / 2f) * 0.96f
        cy = hud.bottom + gap + avail / 2f

        ringThin.strokeWidth = u * 0.5f
        val arcW = r * (if (mazda) 0.035f else 0.075f)
        track.strokeWidth = arcW
        arcP.strokeWidth = arcW
        glowP.strokeWidth = arcW * 2.4f
        tickMajor.strokeWidth = r * 0.012f
        tickMinor.strokeWidth = r * 0.006f
        tickRed.strokeWidth = r * 0.012f
        num.textSize = r * 0.075f
        numRed.textSize = r * 0.075f
        needle.strokeWidth = r * 0.012f
        needleGlow.strokeWidth = r * 0.035f
        pillP.strokeWidth = r * 0.01f
        pillText.textSize = r * 0.075f
        gText.textSize = r * 0.16f
        label.textSize = r * 0.06f
        bracketP.strokeWidth = u * 0.9f
        barP.strokeWidth = u * 1.2f
        digits.textSize = hud.height() * 0.62f
        unitP.textSize = hud.height() * 0.15f
        titleP.textSize = hud.height() * 0.10f
        pill.set(cx - r * 0.26f, cy - r * 0.40f, cx + r * 0.26f, cy - r * 0.26f)
    }

    override fun drawStatic(c: Canvas) {
        c.drawCircle(cx, cy, r, face)
        c.drawCircle(cx, cy, r * 0.995f, ringThin)
        arc(c, r * 0.90f, START_ANGLE, SWEEP, track)
        val n = (maxSpeed / 10f).toInt()
        for (i in 0..n) {
            val v = i * 10
            val a = START_ANGLE + SWEEP * i / n
            val major = i % 2 == 0
            val isRed = v >= maxSpeed * 0.92f
            val outer = r * 0.80f
            val inner = if (major) r * 0.71f else r * 0.76f
            val tp = if (isRed) tickRed else if (major) tickMajor else tickMinor
            c.drawLine(px(outer, a), py(outer, a), px(inner, a), py(inner, a), tp)
            if (major) drawCentered(c, v.toString(), px(r * 0.58f, a), py(r * 0.58f, a), if (isRed) numRed else num)
        }
        c.drawRoundRect(pill, pill.height() / 2f, pill.height() / 2f, pillP)
        drawCentered(c, if (mazda) "DRIVE" else "SPORT S+", cx, pill.centerY(), pillText)
        drawCentered(c, if (mazda) "G-FORCE" else "LATERAL G", cx, cy + r * 0.30f, label)
    }

    override fun drawDynamic(c: Canvas) {
        c.drawRoundRect(hud, u * 1.5f, u * 1.5f, boxFill)
        brackets(c, hud.left, hud.top, hud.right, hud.bottom, hud.height() * 0.22f, bracketP)
        c.drawLine(cx - hud.width() * 0.18f, hud.top, cx + hud.width() * 0.18f, hud.top, barP)
        drawCentered(c, "DIGITAL SPEED", cx, hud.top + hud.height() * 0.17f, titleP)
        drawCentered(c, shown.roundToInt().toString(), cx - hud.width() * 0.08f, hud.centerY() + hud.height() * 0.10f, digits)
        drawCentered(c, unit.uppercase(Locale.US), hud.right - hud.width() * 0.17f, hud.centerY() + hud.height() * 0.10f, unitP)

        val sweep = SWEEP * (shown / maxSpeed).coerceIn(0f, 1f)
        if (sweep > 0.5f) {
            arc(c, r * 0.90f, START_ANGLE, sweep, glowP)
            arc(c, r * 0.90f, START_ANGLE, sweep, arcP)
        }
        c.save()
        c.rotate(angleOf(shown), cx, cy)
        c.drawLine(cx + r * 0.10f, cy, cx + r * 0.78f, cy, needleGlow)
        c.drawLine(cx + r * 0.10f, cy, cx + r * 0.78f, cy, needle)
        c.restore()
        c.drawCircle(cx, cy, r * 0.045f, hubP)
        drawCentered(c, String.format(Locale.US, "%+.2fG", latG), cx, cy + r * 0.12f, gText)
    }
}

/** Gaya BMW: tangga kecepatan di kiri, tangga gaya G di kanan, angka besar di kotak tengah. */
class BmwLadderView(ctx: Context) : GaugeView(ctx) {

    private val accent = Pal.accent
    private val tickOff = paint(Color.argb(110, 255, 255, 255), Paint.Style.STROKE)
    private val tickOn = paint(accent, Paint.Style.STROKE)
    private val tickRed = paint(RED, Paint.Style.STROKE)
    private val numL = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val numR = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val boxFill = paint(Color.argb(170, 0, 0, 0), Paint.Style.FILL)
    private val boxLine = paint(Color.argb(170, 255, 255, 255), Paint.Style.STROKE)
    private val digits = italicDigits()
    private val unitP = paint(accent, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val chevron = paint(accent, Paint.Style.STROKE).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val gText = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private val gMax = 1.6f
    private var topY = 0f
    private var botY = 0f
    private var lx = 0f
    private var rx = 0f
    private val box = RectF()

    override fun onGeometry() {
        val w = width.toFloat()
        val h = height.toFloat()
        topY = h * 0.10f
        botY = h * 0.90f
        lx = w * 0.14f
        rx = w * 0.86f
        box.set(cx - w * 0.17f, h * 0.32f, cx + w * 0.17f, h * 0.68f)
        tickOff.strokeWidth = u * 0.5f
        tickOn.strokeWidth = u * 0.9f
        tickRed.strokeWidth = u * 0.9f
        numL.textSize = u * 3.6f
        numR.textSize = u * 3.6f
        boxLine.strokeWidth = u * 0.4f
        chevron.strokeWidth = u * 1.0f
        digits.textSize = box.height() * 0.62f
        unitP.textSize = box.height() * 0.12f
        gText.textSize = u * 5f
    }

    private fun speedY(v: Float): Float = botY - (botY - topY) * v / maxSpeed

    private fun gY(g: Float): Float = botY - (botY - topY) * g / gMax

    override fun drawStatic(c: Canvas) {
        val n = (maxSpeed / 10f).toInt()
        for (i in 0..n) {
            val v = i * 10f
            val y = speedY(v)
            val len = if (i % 3 == 0) u * 7f else u * 4f
            c.drawLine(lx, y, lx + len, y, tickOff)
            if (i % 3 == 0) c.drawText(v.toInt().toString(), lx - u * 1.5f, y + numL.textSize / 3f, numL)
        }
        for (i in 0..16) {
            val g = i * 0.1f
            val y = gY(g)
            val len = if (i % 4 == 0) u * 7f else u * 4f
            c.drawLine(rx - len, y, rx, y, tickOff)
            if (i % 4 == 0) c.drawText(String.format(Locale.US, "%.1fG", g), rx + u * 1.5f, y + numR.textSize / 3f, numR)
        }
        c.drawRoundRect(box, u * 2f, u * 2f, boxFill)
        c.drawRoundRect(box, u * 2f, u * 2f, boxLine)
        val ch = u * 4f
        c.drawLine(cx - ch, box.top - u * 3f, cx, box.top - u * 7f, chevron)
        c.drawLine(cx, box.top - u * 7f, cx + ch, box.top - u * 3f, chevron)
        c.drawLine(cx - ch, box.bottom + u * 3f, cx, box.bottom + u * 7f, chevron)
        c.drawLine(cx, box.bottom + u * 7f, cx + ch, box.bottom + u * 3f, chevron)
    }

    override fun drawDynamic(c: Canvas) {
        val n = (maxSpeed / 10f).toInt()
        for (i in 0..n) {
            val v = i * 10f
            if (v > shown) break
            val y = speedY(v)
            val len = if (i % 3 == 0) u * 7f else u * 4f
            c.drawLine(lx, y, lx + len, y, if (v >= maxSpeed * 0.92f) tickRed else tickOn)
        }
        val gm = gMag.coerceIn(0f, gMax)
        for (i in 0..16) {
            val g = i * 0.1f
            if (g > gm) break
            val y = gY(g)
            val len = if (i % 4 == 0) u * 7f else u * 4f
            c.drawLine(rx - len, y, rx, y, if (g >= 1.2f) tickRed else tickOn)
        }
        drawCentered(c, shown.roundToInt().toString(), cx, box.centerY() - box.height() * 0.06f, digits)
        drawCentered(c, unit.uppercase(Locale.US), cx, box.bottom - box.height() * 0.13f, unitP)
        drawCentered(c, String.format(Locale.US, "G  %.2f", gMag), cx, box.bottom + u * 14f, gText)
    }
}

/** Gaya Honda: bar segmen di atas, angka besar miring, dan G-meter bulat dengan titik bergerak. */
class HondaGaugeView(ctx: Context) : GaugeView(ctx) {

    private val accent = Pal.accent
    private val segCount = 12
    private val frame = paint(Color.argb(150, 255, 255, 255), Paint.Style.STROKE)
    private val segOff = paint(Color.argb(45, 255, 255, 255), Paint.Style.FILL)
    private val segOn = paint(accent, Paint.Style.FILL)
    private val chipP = paint(accent, Paint.Style.STROKE)
    private val chipText = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val digits = italicDigits()
    private val unitP = paint(Pal.MUTED, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val gRing = paint(Color.argb(150, 255, 255, 255), Paint.Style.STROKE)
    private val gCross = paint(Color.argb(80, 255, 255, 255), Paint.Style.STROKE)
    private val gDot = paint(accent, Paint.Style.FILL)
    private val gDotGlow = paint(withAlpha(accent, 80), Paint.Style.FILL)
    private val gText = paint(Color.WHITE, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    private val gLabel = paint(Pal.MUTED, Paint.Style.FILL).apply {
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.15f
    }

    private val bar = RectF()
    private val seg = RectF()
    private val chip = RectF()
    private var sx = 0f
    private var sy = 0f
    private var gx = 0f
    private var gy = 0f
    private var gr = 0f
    private var pad = 0f
    private var segW = 0f
    private var segGap = 0f

    override fun onGeometry() {
        val w = width.toFloat()
        val h = height.toFloat()
        bar.set(w * 0.06f, h * 0.04f, w * 0.94f, h * 0.14f)
        pad = u * 1.4f
        segGap = u * 1.2f
        segW = (bar.width() - 2f * pad - (segCount - 1) * segGap) / segCount
        sx = w * 0.36f
        sy = h * 0.60f
        gr = minOf(w * 0.16f, h * 0.24f)
        gx = w * 0.79f
        gy = h * 0.58f
        chip.set(sx - h * 0.14f, h * 0.27f, sx + h * 0.14f, h * 0.27f + h * 0.07f)
        frame.strokeWidth = u * 0.5f
        chipP.strokeWidth = u * 0.5f
        chipText.textSize = h * 0.04f
        digits.textSize = h * 0.40f
        unitP.textSize = h * 0.05f
        gRing.strokeWidth = u * 0.5f
        gCross.strokeWidth = u * 0.35f
        gText.textSize = h * 0.06f
        gLabel.textSize = h * 0.035f
    }

    private fun segRect(i: Int) {
        val left = bar.left + pad + i * (segW + segGap)
        seg.set(left, bar.top + pad, left + segW, bar.bottom - pad)
    }

    override fun drawStatic(c: Canvas) {
        c.drawRoundRect(bar, u * 1.5f, u * 1.5f, frame)
        for (i in 0 until segCount) {
            segRect(i)
            c.drawRoundRect(seg, u * 0.6f, u * 0.6f, segOff)
        }
        c.drawRoundRect(chip, chip.height() / 2f, chip.height() / 2f, chipP)
        drawCentered(c, "SPORT", chip.centerX(), chip.centerY(), chipText)
        drawCentered(c, unit.uppercase(Locale.US), sx, sy + height * 0.27f, unitP)

        c.drawCircle(gx, gy, gr, gRing)
        c.drawCircle(gx, gy, gr * 0.5f, gCross)
        c.drawLine(gx - gr, gy, gx + gr, gy, gCross)
        c.drawLine(gx, gy - gr, gx, gy + gr, gCross)
        drawCentered(c, "G-METER", gx, gy - gr - height * 0.05f, gLabel)
    }

    override fun drawDynamic(c: Canvas) {
        val lit = ceil((shown / maxSpeed * segCount).toDouble()).toInt().coerceIn(0, segCount)
        for (i in 0 until lit) {
            segOn.color = when {
                i >= segCount - 2 -> RED
                i >= segCount - 4 -> AMBER
                else -> accent
            }
            segRect(i)
            c.drawRoundRect(seg, u * 0.6f, u * 0.6f, segOn)
        }
        drawCentered(c, shown.roundToInt().toString(), sx, sy, digits)

        var dx = latG.coerceIn(-1f, 1f)
        var dy = longG.coerceIn(-1f, 1f)
        val m = sqrt(dx * dx + dy * dy)
        if (m > 1f) {
            dx /= m
            dy /= m
        }
        val px0 = gx + dx * gr * 0.92f
        val py0 = gy - dy * gr * 0.92f
        c.drawCircle(px0, py0, u * 3.4f, gDotGlow)
        c.drawCircle(px0, py0, u * 1.8f, gDot)
        drawCentered(c, String.format(Locale.US, "%+.2fG", longG), gx, gy + gr + height * 0.07f, gText)
    }
}
