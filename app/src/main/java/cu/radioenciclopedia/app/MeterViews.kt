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

/** Ecualizador de barras con degradado violeta -> rosa -> dorado. */
class EqualizerView(ctx: Context, private val bars: Int = 9) : View(ctx), Animated {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        paint.shader = LinearGradient(
            0f, h.toFloat(), 0f, 0f,
            intArrayOf(Pal.VIOLET, Pal.PINK, Pal.GOLD_LIGHT),
            floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val playing = RadioPlayback.info.status == PlaybackStatus.PLAYING
        val t = if (playing) Clock.t else 0f
        val gap = resources.displayMetrics.density * 3f
        val bw = (w - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val v = if (playing) 0.5f + 0.28f * sin(t * (2.6f + i * 0.35f) + i * 1.3f) +
                0.22f * sin(t * (4.1f - i * 0.2f) + i * 0.7f) else 0.12f
            val bh = bw + (h - bw) * v.coerceIn(0.12f, 1f)
            val x = i * (bw + gap)
            rect.set(x, h - bh, x + bw, h)
            c.drawRoundRect(rect, bw / 2f, bw / 2f, paint)
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

/** Botón de reproducir con ondas que se expanden (decorativo; el toque lo gestiona el contenedor). */
class PlayButtonView(ctx: Context) : View(ctx), Animated {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tri = Path()
    private var fill: RadialGradient? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val r = min(w, h) / 2f * 0.66f
        fill = RadialGradient(
            w / 2f, h / 2f - r * 0.35f, r * 1.6f,
            intArrayOf(Pal.MAGENTA, Pal.VIOLET), null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val half = min(w, h) / 2f
        val r = half * 0.66f
        val d = resources.displayMetrics.density
        val status = RadioPlayback.info.status
        val t = if (status == PlaybackStatus.PLAYING || status == PlaybackStatus.CONNECTING) Clock.t else 0f

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.6f * d
        for (i in 0 until if (status == PlaybackStatus.PLAYING) 2 else 0) {
            val k = (t * 0.55f + i * 0.5f) % 1f
            p.color = withAlpha(Pal.PINK, (1f - k) * 0.6f)
            c.drawCircle(cx, cy, r + k * (half - r), p)
        }
        if (status != PlaybackStatus.PLAYING && status != PlaybackStatus.CONNECTING) {
            // En reposo, un anillo dorado «respira» para invitar a tocar y escuchar.
            val k = (Clock.t * 0.45f) % 1f
            p.color = withAlpha(Pal.GOLD, (1f - k) * 0.55f)
            c.drawCircle(cx, cy, r + k * (half - r), p)
        }

        p.style = Paint.Style.FILL
        p.shader = fill
        c.drawCircle(cx, cy, r, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.4f * d
        p.color = Pal.GOLD
        c.drawCircle(cx, cy, r, p)

        p.style = Paint.Style.FILL
        p.color = Pal.WHITE
        if (status == PlaybackStatus.PLAYING) {
            c.drawRoundRect(cx - r * 0.32f, cy - r * 0.39f, cx - r * 0.10f, cy + r * 0.39f, d, d, p)
            c.drawRoundRect(cx + r * 0.10f, cy - r * 0.39f, cx + r * 0.32f, cy + r * 0.39f, d, d, p)
        } else if (status == PlaybackStatus.CONNECTING) {
            c.drawRect(cx - r * 0.21f, cy - r * 0.21f, cx + r * 0.21f, cy + r * 0.21f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = d * 2f
            c.drawArc(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f, t * 150f, 210f, false, p)
        } else {
            tri.reset()
            tri.moveTo(cx - r * 0.22f, cy - r * 0.40f)
            tri.lineTo(cx - r * 0.22f, cy + r * 0.40f)
            tri.lineTo(cx + r * 0.46f, cy)
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

/** Punto verde "en vivo" que late. */
class LiveDot(ctx: Context) : View(ctx), Animated {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f
        val k = 0.5f + 0.5f * sin(Clock.t * 4f)
        p.color = withAlpha(Pal.GREEN, 0.25f + 0.25f * k)
        c.drawCircle(cx, cy, r, p)
        p.color = Pal.GREEN
        c.drawCircle(cx, cy, r * 0.55f, p)
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
