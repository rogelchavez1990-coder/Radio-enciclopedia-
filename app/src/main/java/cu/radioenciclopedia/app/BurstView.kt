package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Explosión musical al empezar a sonar: notas que suben en abanico, destellos dorados y
 * ondas de radio que se expanden. Capa transparente que no intercepta toques.
 */
class BurstView(ctx: Context) : View(ctx) {

    private class P(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        val max: Float, val kind: Int, val size: Float, val color: Int, val flag: Boolean
    ) {
        var life = 0f
    }

    private val ps = ArrayList<P>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rnd = Random(System.nanoTime())
    private var last = 0L
    private var palette = intArrayOf(Pal.ROSE, Pal.GOLD, Pal.PINK, Pal.GOLD_LIGHT)

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    fun burst(cx: Float, cy: Float) {
        if (!Motion.enabled()) return
        val d = resources.displayMetrics.density
        repeat(13) {
            val a = -PI / 2 + (rnd.nextFloat() - 0.5f) * PI * 1.15
            val sp = (120f + rnd.nextFloat() * 170f) * d
            ps.add(P(cx, cy, (cos(a) * sp).toFloat(), (sin(a) * sp).toFloat(), 1.5f + rnd.nextFloat() * 0.9f, 0,
                (8f + rnd.nextFloat() * 7f) * d, palette[rnd.nextInt(palette.size)], rnd.nextBoolean()))
        }
        repeat(18) {
            val a = rnd.nextFloat() * 2 * PI
            val sp = (50f + rnd.nextFloat() * 190f) * d
            ps.add(P(cx, cy, (cos(a) * sp).toFloat(), (sin(a) * sp).toFloat(), 0.7f + rnd.nextFloat() * 0.5f, 1,
                (3f + rnd.nextFloat() * 5f) * d, if (rnd.nextBoolean()) Pal.GOLD_LIGHT else Pal.WHITE, false))
        }
        repeat(2) { i ->
            ps.add(P(cx, cy, 0f, 0f, 0.95f + i * 0.25f, 2, (70f + i * 34f) * d, palette[1], false))
        }
        last = 0L
        postInvalidateOnAnimation()
    }

    /** Igual que [burst], pero con los colores de una emisora. */
    fun burstTinted(cx: Float, cy: Float, a: Int, b: Int) {
        val old = palette
        palette = intArrayOf(Pal.WHITE, a, b, Pal.GOLD_LIGHT)
        burst(cx, cy)
        palette = old
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
        last = now
        val d = resources.displayMetrics.density
        var i = ps.size - 1
        while (i >= 0) {
            val p = ps[i]
            p.life += dt
            if (p.life >= p.max) {
                ps.removeAt(i)
                i--
                continue
            }
            val t = p.life / p.max
            val fade = (1f - t * t).coerceIn(0f, 1f)
            when (p.kind) {
                0 -> {
                    p.vx *= 1f - 1.1f * dt
                    p.vy = p.vy * (1f - 1.1f * dt) + 38f * d * dt
                    p.x += p.vx * dt + sin(p.life * 6f + p.size) * 12f * d * dt
                    p.y += p.vy * dt
                    paint.style = Paint.Style.FILL
                    paint.color = withAlpha(p.color, fade)
                    drawMusicNote(c, paint, p.x, p.y, p.size, p.flag)
                }
                1 -> {
                    p.vx *= 1f - 1.8f * dt
                    p.vy *= 1f - 1.8f * dt
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    val s = p.size * (1f - t * 0.7f)
                    paint.style = Paint.Style.FILL
                    paint.color = withAlpha(p.color, fade)
                    path.reset()
                    path.moveTo(p.x, p.y - s * 1.6f)
                    path.quadTo(p.x, p.y, p.x + s * 1.6f, p.y)
                    path.quadTo(p.x, p.y, p.x, p.y + s * 1.6f)
                    path.quadTo(p.x, p.y, p.x - s * 1.6f, p.y)
                    path.quadTo(p.x, p.y, p.x, p.y - s * 1.6f)
                    path.close()
                    c.drawPath(path, paint)
                }
                else -> {
                    val ease = 1f - (1f - t) * (1f - t)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2.2f * d
                    paint.color = withAlpha(p.color, (1f - t) * 0.75f)
                    c.drawCircle(p.x, p.y, p.size * ease, paint)
                }
            }
            i--
        }
        if (ps.isNotEmpty()) postInvalidateOnAnimation() else last = 0L
    }
}
