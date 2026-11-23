package cu.radioenciclopedia.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Pentagrama malva sobre rosa, repetido sin huecos y desplazado hacia la izquierda. */
class MusicRibbonView(ctx: Context) : View(ctx), Animated {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Pal.BRAND }
    private val staff = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = withAlpha(Pal.BRAND, 0.64f); strokeWidth = 0.75f
    }
    private val clef = Path().apply {
        moveTo(27f, 60f)
        cubicTo(43f, 58f, 26f, 21f, 31f, 7f)
        cubicTo(38f, -1f, 42f, 14f, 29f, 25f)
        cubicTo(9f, 41f, 19f, 56f, 34f, 49f)
        cubicTo(49f, 41f, 32f, 26f, 25f, 38f)
        cubicTo(22f, 43f, 26f, 47f, 30f, 46f)
    }
    private val notes = intArrayOf(34, 28, 40, 31, 22, 34, 46, 37, 28, 31, 43, 46, 40, 34, 25, 31, 43, 37, 46, 34, 28, 40, 31, 22, 34, 25, 37, 28)
    private var offset = 0f
    private var previousTime: Float? = null

    init {
        setBackgroundColor(Pal.ROSE)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        if (height <= 0 || width <= 0) return
        val scale = height / 64f
        val tileWidth = 896f * scale
        val phase = -offset % tileWidth
        var x = phase - tileWidth
        while (x < width) {
            canvas.save()
            canvas.translate(x, 0f)
            canvas.scale(scale, scale)
            drawPhrase(canvas)
            canvas.restore()
            x += tileWidth
        }
    }

    private fun drawPhrase(canvas: Canvas) {
        for (line in 0..4) canvas.drawLine(0f, 19f + line * 7f, 896f, 19f + line * 7f, staff)
        ink.style = Paint.Style.STROKE
        ink.strokeWidth = 2.5f
        ink.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(clef, ink)
        ink.style = Paint.Style.FILL
        canvas.drawCircle(26f, 59f, 3.4f, ink)
        for (i in notes.indices) {
            val x = 68f + i * 29f
            val y = notes[i].toFloat()
            canvas.save()
            canvas.rotate(-18f, x, y)
            canvas.drawOval(x - 5.5f, y - 3.7f, x + 5.5f, y + 3.7f, ink)
            canvas.restore()
            canvas.drawRect(x + 4f, y - 24f, x + 5.8f, y, ink)
            if (i % 4 == 1 && i + 1 < notes.size) {
                ink.strokeWidth = 3.4f
                canvas.drawLine(x + 4f, y - 24f, x + 34.8f, notes[i + 1] - 24f, ink)
            } else if (i % 4 == 3) {
                ink.strokeWidth = 2.5f
                canvas.drawLine(x + 5f, y - 23f, x + 13f, y - 16f, ink)
            }
        }
        canvas.drawRect(880f, 18f, 881.5f, 48f, ink)
    }

    override fun tick(t: Float) {
        val before = previousTime
        previousTime = t
        if (!ValueAnimator.areAnimatorsEnabled()) return
        if (before != null && height > 0) {
            // Avance de fase negativo al dibujar: derecha → izquierda, a 18 dp por segundo.
            // Las notas corren más deprisa mientras suena la radio.
            val boost = if (RadioPlayback.info.status == PlaybackStatus.PLAYING) 2.6f else 1f
            offset = (offset + context.dp(18f) * boost * (t - before).coerceIn(0f, 0.12f)) % (896f * height / 64f)
            invalidate()
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); previousTime = null; Clock.register(this) }
    override fun onDetachedFromWindow() { Clock.unregister(this); previousTime = null; super.onDetachedFromWindow() }
}
