package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Símbolo a la izquierda y firma en dos líneas, restaurados desde la referencia aportada. */
class OfficialLogoView(ctx: Context, onPhoto: Boolean = false) : ImageView(ctx) {
    init {
        setImageResource(R.drawable.firma_referencia)
        scaleType = ScaleType.FIT_CENTER
        contentDescription = "Radio Enciclopedia, símbolo y firma de la emisora"
        setPadding(ctx.dpi(5), ctx.dpi(3), ctx.dpi(5), ctx.dpi(3))
        if (onPhoto) {
            // La misma firma: lettering blanco y símbolo lila sobre la fotografía.
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                -0.35f, 0f, 0f, 0f, 255f,
                -1.05f, 0f, 0f, 0f, 255f,
                0f, 0f, 0f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )))
        } else {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFFFDF4FA.toInt(), Pal.ROSE)).apply {
                cornerRadius = ctx.dp(16f)
            }
        }
    }
}

/**
 * Retrato restaurado: recorte centrado que conserva la cabeza y las proporciones. Un zoom
 * muy lento (efecto Ken Burns) le da vida, con esquinas redondeadas y filete dorado.
 */
class MartiPortraitView(ctx: Context) : View(ctx), Animated {
    private val bitmap = BitmapFactory.decodeResource(resources, R.drawable.jose_marti_restaurado)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ctx.dp(1.5f)
        color = withAlpha(Pal.GOLD, 0.75f)
    }
    private val borderRect = RectF()
    private val base = Rect(0, 0, bitmap.width, bitmap.height)
    private val src = Rect()
    private val dst = Rect()

    init {
        contentDescription = "Retrato de José Martí"
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, ctx.dp(14f))
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val scale = max(w.toFloat() / bitmap.width, h.toFloat() / bitmap.height)
        val sw = min(bitmap.width, (w / scale).toInt()).coerceAtLeast(1)
        val sh = min(bitmap.height, (h / scale).toInt()).coerceAtLeast(1)
        base.set((bitmap.width - sw) / 2, (bitmap.height - sh) / 2, (bitmap.width + sw) / 2, (bitmap.height + sh) / 2)
        dst.set(0, 0, w, h)
        borderRect.set(1f, 1f, w - 1f, h - 1f)
    }

    override fun onDraw(canvas: Canvas) {
        val t = Clock.t
        val zoom = 1f + 0.05f * (0.5f + 0.5f * sin(t * 0.42f))
        val sw = (base.width() / zoom).toInt().coerceAtLeast(1)
        val sh = (base.height() / zoom).toInt().coerceAtLeast(1)
        // El centro se desplaza un poco y se mantiene siempre dentro de la imagen.
        val cx = (base.centerX() + (sin(t * 0.21f) * base.width() * 0.012f).toInt())
            .coerceIn(sw / 2, (bitmap.width - sw / 2).coerceAtLeast(sw / 2))
        val cy = (base.centerY() - (sin(t * 0.42f) * base.height() * 0.008f).toInt())
            .coerceIn(sh / 2, (bitmap.height - sh / 2).coerceAtLeast(sh / 2))
        src.set(cx - sw / 2, cy - sh / 2, cx - sw / 2 + sw, cy - sh / 2 + sh)
        canvas.drawBitmap(bitmap, src, dst, paint)
        canvas.drawRoundRect(borderRect, context.dp(14f), context.dp(14f), border)
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

/** Destello diagonal de luz que cruza el banner cada pocos segundos. Solo es luz: no cambia la imagen. */
class GlintView(ctx: Context) : View(ctx), Animated {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val period = 9f

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        paint.shader = android.graphics.LinearGradient(
            0f, 0f, w * 0.22f, 0f,
            intArrayOf(0x00FFFFFF, 0x3CFFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val k = (Clock.t % period) / 1.6f
        if (k > 1f || width <= 0) return
        val band = width * 0.22f
        val x = -band * 1.5f + k * (width + band * 3f)
        c.save()
        c.translate(x, 0f)
        c.skew(-0.32f, 0f)
        c.drawRect(0f, 0f, band, height.toFloat(), paint)
        c.restore()
    }

    override fun tick(t: Float) {
        if ((t % period) / 1.6f <= 1.05f) invalidate()
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

/** Banner completo aportado por el propietario, sin recortar ni superponer otra firma. */
class BrandHero(ctx: Context, openMenu: () -> Unit) : ViewGroup(ctx) {
    private val image = ImageView(ctx).apply {
        setImageResource(R.drawable.portada_partituras_restaurada)
        scaleType = ImageView.ScaleType.FIT_CENTER
        setBackgroundColor(Pal.ROSE)
        contentDescription = "Radio Enciclopedia. Una emisora para todos los momentos de la vida."
    }
    private val glint = GlintView(ctx)
    private val ribbon = MusicRibbonView(ctx).apply { id = R.id.music_ribbon }
    private val menu = TextView(ctx).apply {
        text = "☰"; setTextColor(Pal.INK); textSize = 27f; gravity = Gravity.CENTER
        contentDescription = "Abrir menú"; isFocusable = true; setOnClickListener { openMenu() }
        background = GradientDrawable().apply {
            cornerRadius = ctx.dp(14f); setColor(withAlpha(Pal.ROSE, 0.82f))
            setStroke(ctx.dpi(1), withAlpha(Pal.BRAND, 0.55f))
        }
    }
    private var ribbonH = 0
    init { listOf(image, glint, ribbon, menu).forEach { addView(it) } }

    fun preferredHeight(width: Int): Int {
        val ratio = image.drawable.intrinsicHeight.toFloat() / image.drawable.intrinsicWidth
        return (width * ratio).toInt() + context.dpi(20)
    }

    override fun onMeasure(ws: Int, hs: Int) {
        val w = MeasureSpec.getSize(ws); val h = MeasureSpec.getSize(hs)
        setMeasuredDimension(w, h)
        ribbonH = min(h, context.dpi(if (h < context.dpi(180)) 14 else 20))
        fun size(v: View, cw: Int, ch: Int) = v.measure(MeasureSpec.makeMeasureSpec(cw.coerceAtLeast(1), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ch.coerceAtLeast(1), MeasureSpec.EXACTLY))
        size(image, w, h - ribbonH)
        size(glint, w, h - ribbonH)
        size(menu, context.dpi(44), context.dpi(44))
        size(ribbon, w, ribbonH)
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        image.layout(0, 0, width, height - ribbonH)
        glint.layout(0, 0, width, height - ribbonH)
        ribbon.layout(0, height - ribbonH, width, height)
        menu.layout(width - context.dpi(47), context.dpi(4), width - context.dpi(3), context.dpi(48))
    }
}
