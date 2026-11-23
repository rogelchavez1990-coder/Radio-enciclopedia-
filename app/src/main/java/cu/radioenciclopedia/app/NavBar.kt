package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Barra de navegación inferior con una píldora «líquida»: se desliza con un resorte y se
 * estira en la dirección del movimiento. El icono activo da un pequeño salto elástico.
 * Cada pestaña es una vista real con descripción de accesibilidad ("Ir a Inicio", etc.).
 */
class NavBar(
    ctx: Context,
    labels: List<String>,
    iconRes: List<Int>,
    private val onPick: (Int) -> Unit
) : FrameLayout(ctx) {

    private val n = labels.size
    private val iconViews = ArrayList<ImageView>()
    private val textViews = ArrayList<TextView>()
    private val iconAnims = ArrayList<SpringAnim>()
    private val pill = RectF()
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val posAnim = SpringAnim(Spring(0f, 330f, 0.62f, 0.002f)) { invalidate() }
    private var selected = 0
    private val d = ctx.resources.displayMetrics.density
    private val barHeight = ctx.dpi(66)

    init {
        val rad = ctx.dp(26f)
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xEF341A4D.toInt(), 0xFA11091D.toInt())
        )
        bg.cornerRadii = floatArrayOf(rad, rad, rad, rad, 0f, 0f, 0f, 0f)
        bg.setStroke(ctx.dpi(1), withAlpha(Pal.PINK, 0.60f))
        background = bg

        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        addView(row, FrameLayout.LayoutParams(MATCH, barHeight, Gravity.TOP))

        for (i in 0 until n) {
            val item = LinearLayout(ctx)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER
            item.isClickable = true
            item.isFocusable = true
            item.contentDescription = "Ir a " + labels[i]
            item.setOnClickListener { v ->
                Haptics.tick(v)
                onPick(i)
            }

            val icon = ImageView(ctx)
            icon.setImageResource(iconRes[i])
            icon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            item.addView(icon, LinearLayout.LayoutParams(ctx.dpi(24), ctx.dpi(24)))

            val tv = ctx.label(labels[i], 11f, Pal.LILAC)
            tv.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            tv.maxLines = 1
            if (ctx.resources.configuration.screenWidthDp < 360) tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.topMargin = ctx.dpi(4)
            item.addView(tv, lp)

            row.addView(item, LinearLayout.LayoutParams(0, MATCH, 1f))
            iconViews.add(icon)
            textViews.add(tv)
            // Salto elástico: la escala sube con rebote y el icono se eleva a la vez.
            iconAnims.add(SpringAnim(Spring(1f, 520f, 0.38f)) { s ->
                icon.scaleX = s
                icon.scaleY = s
                icon.translationY = -(s - 1f) * 40f * d
            })
        }
        select(0, false)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val top = 7f * d
        pillPaint.shader = LinearGradient(
            0f, top, 0f, top + 52f * d,
            intArrayOf(withAlpha(Pal.BRAND, 0.88f), withAlpha(Pal.MAGENTA, 0.70f)), null, Shader.TileMode.CLAMP
        )
    }

    override fun dispatchDraw(c: Canvas) {
        val itemW = (width - paddingLeft - paddingRight) / n.toFloat()
        val x = paddingLeft + posAnim.value * itemW
        // Efecto líquido: el borde delantero se estira según la velocidad del resorte.
        val vel = posAnim.spring.velocity
        val stretch = (abs(vel) * 0.09f).coerceAtMost(0.5f) * itemW
        var left = x + 6f * d
        var right = x + itemW - 6f * d
        if (vel > 0f) right += stretch else left -= stretch
        pill.set(left, 7f * d, right, 59f * d)
        if (selected >= 0) c.drawRoundRect(pill, 22f * d, 22f * d, pillPaint)
        super.dispatchDraw(c)
    }

    fun select(i: Int, animate: Boolean) {
        selected = i
        for (k in 0 until n) {
            val on = k == i
            iconViews[k].setColorFilter(if (on) Pal.WHITE else withAlpha(Pal.LILAC, 0.8f))
            textViews[k].setTextColor(if (on) Pal.WHITE else withAlpha(Pal.LILAC, 0.8f))
            textViews[k].typeface = if (on) Fonts.bold else Fonts.regular
            val s = if (on) 1.12f else 1f
            if (animate) iconAnims[k].animateTo(s, if (on) 2.6f else null) else iconAnims[k].jump(s)
        }
        if (i < 0) {
            invalidate()
            return
        }
        if (animate) posAnim.animateTo(i.toFloat()) else posAnim.jump(i.toFloat())
    }

    override fun onDetachedFromWindow() {
        posAnim.cancel()
        iconAnims.forEach { it.cancel() }
        super.onDetachedFromWindow()
    }
}
