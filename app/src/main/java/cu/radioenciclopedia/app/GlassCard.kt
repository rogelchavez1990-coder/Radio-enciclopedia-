package cu.radioenciclopedia.app

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout

/** Cristal translúcido, borde fino y reflejo suave. Un solo reloj; sin blur costoso. */
open class GlassCard(
    ctx: Context,
    radiusDp: Float = 22f,
    private val delay: Float = 0f,
    interactive: Boolean = true,
    private val gold: Boolean = false,
    private val light: Boolean = false
) : FrameLayout(ctx), Animated {
    private val radius = ctx.dp(radiusDp)
    private val rect = RectF()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ctx.dp(1f)
    }
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()
    private val matrix = Matrix()
    private var sheen: LinearGradient? = null
    private var wasShining = false
    private val period = 11f
    private val tilt = PressTilt(this)
    private val spot = Paint(Paint.ANTI_ALIAS_FLAG)
    private var spotRadius = 0f

    init {
        setWillNotDraw(false)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        if (interactive) {
            isClickable = true
            isFocusable = true
            val mask = GradientDrawable().apply {
                cornerRadius = radius
                setColor(Color.WHITE)
            }
            foreground = RippleDrawable(ColorStateList.valueOf(0x26FFFFFF), null, mask)
            // Pulsación con física: se hunde, se inclina hacia el dedo y deja un foco de luz.
            setOnTouchListener { _, e ->
                tilt.onTouch(e)
                false
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w <= 0 || h <= 0) return
        val s = border.strokeWidth / 2
        rect.set(s, s, w - s, h - s)
        clip.reset()
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        val colors = when {
            gold -> intArrayOf(0xFFFFE4BB.toInt(), 0xFFF5C995.toInt(), 0xFFDDA568.toInt())
            light -> intArrayOf(0xA6FFFFFF.toInt(), 0x5CFFFFFF, 0x38FFFFFF)
            else -> intArrayOf(withAlpha(Pal.BRAND, 0.62f), 0xD32E164B.toInt(), 0xEF170D2A.toInt())
        }
        fill.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors,
            floatArrayOf(0f, 0.48f, 1f), Shader.TileMode.CLAMP)
        border.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(withAlpha(Pal.PINK, 0.95f), withAlpha(Pal.BRAND, 0.58f), withAlpha(Pal.VIOLET, 0.95f)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        sheen = LinearGradient(0f, 0f, w * 0.28f, 0f,
            intArrayOf(0x00FFFFFF, 0x18FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        shine.shader = sheen
        spotRadius = maxOf(w, h) * 0.55f
        spot.shader = RadialGradient(0f, 0f, spotRadius, 0x3CFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        c.drawRoundRect(rect, radius, radius, fill)
        c.drawRoundRect(rect, radius, radius, border)
    }

    override fun dispatchDraw(c: Canvas) {
        super.dispatchDraw(c)
        if (tilt.glowAmount > 0.01f) {
            spot.alpha = (255f * tilt.glowAmount.coerceIn(0f, 1f)).toInt()
            c.save()
            c.clipPath(clip)
            c.translate(tilt.touchX, tilt.touchY)
            c.drawCircle(0f, 0f, spotRadius, spot)
            c.restore()
        }
        if (!Clock.running()) return
        val progress = (Clock.t + delay) % period
        if (progress > 2.4f) return
        val x = -width * 0.35f + progress / 2.4f * width * 1.75f
        matrix.setTranslate(x, 0f)
        sheen?.setLocalMatrix(matrix)
        c.save()
        c.clipPath(clip)
        c.drawRect(x, 0f, x + width * 0.28f, height.toFloat(), shine)
        c.restore()
    }

    override fun tick(t: Float) {
        val shining = (t + delay) % period <= 2.4f
        if (shining || wasShining) invalidate()
        wasShining = shining
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Clock.register(this)
    }

    override fun onDetachedFromWindow() {
        animate().cancel()
        tilt.cancel()
        Clock.unregister(this)
        super.onDetachedFromWindow()
    }
}
