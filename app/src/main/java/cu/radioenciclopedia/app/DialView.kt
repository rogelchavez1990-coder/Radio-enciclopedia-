package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.min
import kotlin.math.sin

/**
 * Dial de radio: barras de espectro que bailan alrededor, regla de sintonía que gira,
 * disco de vinilo con reflejos y botón central de reproducir. Todo animado y decorativo.
 */
class DialView(ctx: Context) : View(ctx), Animated {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tri = Path()
    private val oval = RectF()
    private val stops = intArrayOf(Pal.GOLD, Pal.MAGENTA, Pal.VIOLET, Pal.MAGENTA, Pal.GOLD)
    private var vinyl: RadialGradient? = null
    private var labelFill: LinearGradient? = null
    private var glow: RadialGradient? = null

    private fun colorAt(f: Float): Int {
        val x = f.coerceIn(0f, 0.9999f) * (stops.size - 1)
        val i = x.toInt()
        return lerpColor(stops[i], stops[i + 1], x - i)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f
        vinyl = RadialGradient(
            cx, cy, r * 0.54f,
            intArrayOf(0xFF3A1470.toInt(), 0xFF1A0640.toInt(), 0xFF0E0222.toInt()),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        labelFill = LinearGradient(
            cx - r * 0.3f, cy - r * 0.3f, cx + r * 0.3f, cy + r * 0.3f,
            Pal.MAGENTA, Pal.VIOLET, Shader.TileMode.CLAMP
        )
        glow = RadialGradient(
            cx, cy, r,
            intArrayOf(withAlpha(Pal.MAGENTA, 0.50f), withAlpha(Pal.MAGENTA, 0f)),
            floatArrayOf(0.5f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f) return
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f
        val d = resources.displayMetrics.density
        val status = RadioPlayback.info.status
        val t = if (status == PlaybackStatus.PLAYING || status == PlaybackStatus.CONNECTING) Clock.t else 0f

        // halo cálido que respira mientras suena la radio
        if (status == PlaybackStatus.PLAYING) {
            p.style = Paint.Style.FILL
            p.shader = glow
            p.alpha = (150 + 70 * sin(t * 2f)).toInt().coerceIn(0, 255)
            c.drawCircle(cx, cy, r, p)
        }

        // ondas que se expanden
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        for (i in 0 until if (status == PlaybackStatus.PLAYING) 3 else 0) {
            val k = (t * 0.4f + i / 3f) % 1f
            p.color = withAlpha(Pal.PINK, (1f - k) * 0.4f)
            c.drawCircle(cx, cy, r * 0.62f + k * r * 0.38f, p)
        }

        // barras de espectro radiales
        val n = 60
        val r0 = r * 0.66f
        val maxLen = r * 0.30f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 3.2f * d
        for (i in 0 until n) {
            val v = if (status == PlaybackStatus.PLAYING) 0.5f + 0.25f * sin(t * 3.1f + i * 0.55f) + 0.25f * sin(t * 1.7f - i * 0.31f + 1f) else 0.12f
            val len = maxLen * (0.22f + 0.78f * v.coerceIn(0f, 1f))
            p.color = colorAt(i / n.toFloat())
            c.save()
            c.rotate(i * 360f / n + t * 5f, cx, cy)
            c.drawLine(cx, cy - r0, cx, cy - r0 - len, p)
            c.restore()
        }

        // regla de sintonía
        p.strokeCap = Paint.Cap.BUTT
        p.strokeWidth = 1.2f * d
        p.color = withAlpha(Pal.LILAC, 0.55f)
        val rt = r * 0.60f
        c.save()
        c.rotate(-t * 3f, cx, cy)
        for (i in 0 until 90) {
            val long = i % 10 == 0
            c.save()
            c.rotate(i * 4f, cx, cy)
            c.drawLine(cx, cy - rt, cx, cy - rt + (if (long) 8f * d else 4f * d), p)
            c.restore()
        }
        c.restore()

        // vinilo
        p.style = Paint.Style.FILL
        p.shader = vinyl
        c.drawCircle(cx, cy, r * 0.54f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * d
        p.color = withAlpha(Pal.WHITE, 0.09f)
        for (k in 1..5) {
            c.drawCircle(cx, cy, r * (0.30f + k * 0.043f), p)
        }
        // reflejos que giran
        c.save()
        c.rotate(t * 22f, cx, cy)
        p.strokeWidth = r * 0.20f
        p.color = withAlpha(Pal.WHITE, 0.07f)
        val ro = r * 0.40f
        oval.set(cx - ro, cy - ro, cx + ro, cy + ro)
        c.drawArc(oval, -70f, 40f, false, p)
        c.drawArc(oval, 110f, 40f, false, p)
        c.restore()

        // etiqueta central con botón de reproducir
        val lr = r * 0.27f * (1f + 0.025f * sin(t * 3.2f))
        p.style = Paint.Style.FILL
        p.shader = labelFill
        c.drawCircle(cx, cy, lr, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * d
        p.color = Pal.GOLD
        c.drawCircle(cx, cy, lr, p)
        p.style = Paint.Style.FILL
        p.color = Pal.WHITE
        if (status == PlaybackStatus.PLAYING) {
            c.drawRoundRect(cx - lr * 0.35f, cy - lr * 0.42f, cx - lr * 0.12f, cy + lr * 0.42f, d, d, p)
            c.drawRoundRect(cx + lr * 0.12f, cy - lr * 0.42f, cx + lr * 0.35f, cy + lr * 0.42f, d, d, p)
        } else if (status == PlaybackStatus.CONNECTING) {
            c.drawRect(cx - lr * 0.23f, cy - lr * 0.23f, cx + lr * 0.23f, cy + lr * 0.23f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = d * 3f
            c.drawArc(cx - lr * 0.74f, cy - lr * 0.74f, cx + lr * 0.74f, cy + lr * 0.74f, t * 150f, 230f, false, p)
        } else {
            tri.reset()
            tri.moveTo(cx - lr * 0.28f, cy - lr * 0.48f)
            tri.lineTo(cx - lr * 0.28f, cy + lr * 0.48f)
            tri.lineTo(cx + lr * 0.56f, cy)
            tri.close()
            c.drawPath(tri, p)
        }
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
