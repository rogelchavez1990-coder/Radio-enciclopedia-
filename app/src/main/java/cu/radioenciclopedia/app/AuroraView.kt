package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Fondo malva RGB con reflejos rosados suaves de la identidad de la emisora. */
class AuroraView(ctx: Context) : View(ctx), Animated {

    private class Blob(
        val color: Int, val x0: Float, val y0: Float,
        val ax: Float, val ay: Float, val fx: Float, val fy: Float, val r: Float
    )

    private val blobs = listOf(
        Blob(0x507F5C9B, 0.18f, 0.16f, 0.16f, 0.10f, 0.018f, 0.025f, 0.80f),
        Blob(0x407B416C, 0.88f, 0.42f, 0.14f, 0.12f, 0.020f, 0.022f, 0.72f),
        Blob(0x508E318F, 0.28f, 0.80f, 0.20f, 0.09f, 0.024f, 0.020f, 0.90f),
        Blob(0x38E8A575, 0.78f, 0.92f, 0.12f, 0.05f, 0.020f, 0.025f, 0.55f)
    )

    private val base = Paint()
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private var shaders: List<RadialGradient> = emptyList()

    private val stars = 16
    private val rnd = Random(5)
    private val sx = FloatArray(stars) { rnd.nextFloat() }
    private val sy = FloatArray(stars) { rnd.nextFloat() }
    private val sz = FloatArray(stars) { 0.5f + rnd.nextFloat() * 1.1f }
    private val sp = FloatArray(stars) { 0.8f + rnd.nextFloat() * 2.2f }
    private val ph = FloatArray(stars) { rnd.nextFloat() * 6.283f }

    // Notas musicales que suben despacio por el fondo, se mecen y se desvanecen.
    private val noteCount = 7
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nx = FloatArray(noteCount) { 0.06f + rnd.nextFloat() * 0.88f }
    private val nSpeed = FloatArray(noteCount) { 0.030f + rnd.nextFloat() * 0.030f }
    private val nOff = FloatArray(noteCount) { rnd.nextFloat() }
    private val nSize = FloatArray(noteCount) { 9f + rnd.nextFloat() * 8f }
    private val nSway = FloatArray(noteCount) { rnd.nextFloat() * 6.283f }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        if (w <= 0 || h <= 0) return
        base.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Pal.MID, 0xFF4C2D57.toInt(), 0xFF643A70.toInt(), 0xFF72436C.toInt()),
            floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        val m = max(w, h).toFloat()
        shaders = blobs.map {
            RadialGradient(0f, 0f, it.r * m, it.color, it.color and 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
    }

    override fun onDraw(c: Canvas) {
        if (shaders.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val t = Clock.t
        c.drawRect(0f, 0f, w, h, base)
        val m = max(w, h)
        for (i in blobs.indices) {
            val b = blobs[i]
            val cx = (b.x0 + b.ax * sin(t * b.fx * 6.283f + i)) * w
            val cy = (b.y0 + b.ay * cos(t * b.fy * 6.283f + i * 2f)) * h
            blobPaint.shader = shaders[i]
            c.save()
            c.translate(cx, cy)
            c.drawCircle(0f, 0f, b.r * m, blobPaint)
            c.restore()
        }
        val d = resources.displayMetrics.density
        for (i in 0 until stars) {
            val a = 0.2f + 0.8f * (0.5f + 0.5f * sin(t * sp[i] + ph[i]))
            starPaint.alpha = (a * 85f).toInt()
            c.drawCircle(sx[i] * w, sy[i] * h * 0.8f, sz[i] * d, starPaint)
        }
        for (i in 0 until noteCount) {
            val k = (t * nSpeed[i] + nOff[i]) % 1f
            val fade = sin(k * 3.1416f)
            val x = nx[i] * w + sin(t * 0.7f + nSway[i]) * 14f * d
            val y = h * (1.02f - k * 1.1f)
            notePaint.color = withAlpha(Pal.ROSE, 0.22f * fade)
            drawNote(c, x, y, nSize[i] * d, i % 2 == 0)
        }
    }

    /** Corchea sencilla dibujada con formas, sin depender de que la fuente tenga el símbolo. */
    private fun drawNote(c: Canvas, x: Float, y: Float, s: Float, flag: Boolean) {
        c.save()
        c.rotate(-18f, x, y)
        c.drawOval(x - s * 0.5f, y - s * 0.34f, x + s * 0.5f, y + s * 0.34f, notePaint)
        c.restore()
        c.drawRect(x + s * 0.38f, y - s * 2.2f, x + s * 0.50f, y, notePaint)
        if (flag) c.drawOval(x + s * 0.50f, y - s * 2.2f, x + s * 1.05f, y - s * 1.55f, notePaint)
    }

    override fun tick(t: Float) {
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Clock.register(this)
    }

    override fun onDetachedFromWindow() {
        Clock.unregister(this)
        super.onDetachedFromWindow()
    }
}
