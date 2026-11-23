package cu.radioenciclopedia.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Inicio sin ScrollView: banner, radio, accesos, sitios y redes en el espacio útil. */
class CompactHomeView(private val act: MainActivity) : ViewGroup(act) {
    private val hero = BrandHero(act) { act.openMenu() }
    private val live = InlinePlayerView(act)
    private val grid = HomeGrid(act)
    private val sites = HomeSites(act)
    private val socials = HomeSocials(act)
    val entryItems: List<View> get() = listOf(live, grid, sites, socials)
    private var landscape = false
    private val boxes = ArrayList<IntArray>()

    init {
        contentDescription = "Inicio completo, sin desplazamiento vertical"
        clipChildren = false
        grid.addTile(R.drawable.ic_gold_radio, "Programas", "Escucha y aprende", "Ir a la sección Programas") { act.goTo(2) }
        grid.addTile(R.drawable.ic_frequencies, "Frecuencias", "FM en toda Cuba", "Ver frecuencias nacionales") { act.goTo(5) }
        grid.addTile(R.drawable.ic_menu_video, "YouTube", "Videos y clips", "Abrir " + Links.VIDEOS.name) { act.openLink(Links.VIDEOS) }
        grid.addTile(R.drawable.ic_gold_comunidad, "Comunidad", "Síguenos", "Ir a la sección Comunidad") { act.goTo(3) }
        listOf(hero, live, grid, sites, socials).forEach { addView(it) }
        refreshSchedule()
    }

    fun refreshSchedule() {
        live.refresh()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this && visibility == VISIBLE) popIn()
    }

    /** Los accesos «saltan» a su sitio con un ligero rebote cada vez que se muestra Inicio. */
    private fun popIn() {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        var order = 0
        for (group in listOf<ViewGroup>(grid, sites, socials)) {
            // En redes, el primer hijo es el rótulo «Síguenos en nuestras redes».
            val first = if (group === socials) 1 else 0
            for (k in first until group.childCount) {
                val v = group.getChildAt(k)
                v.animate().cancel()
                // Resorte con rebote: cada acceso «salta» a su sitio, uno tras otro.
                v.springIn(140L + order * 55L, fromScale = 0.55f, ratio = 0.5f, stiffness = 260f)
                order++
            }
        }
    }

    override fun onMeasure(ws: Int, hs: Int) {
        val w = MeasureSpec.getSize(ws); val h = MeasureSpec.getSize(hs)
        setMeasuredDimension(w, h)
        val cw = (w - paddingLeft - paddingRight).coerceAtLeast(1)
        val ch = (h - paddingTop - paddingBottom).coerceAtLeast(1)
        val d = resources.displayMetrics.density
        val gap = act.dpi(if (ch / d < 500) 4 else if (ch / d < 620) 6 else 8)
        val side = act.dpi(12)
        landscape = cw > ch * 1.15f
        boxes.clear()
        if (!landscape) {
            val dense = ch / d < 620
            grid.singleRow = dense
            val liveH = if (dense) act.dpi(60) else (ch * 0.12f).toInt().coerceIn(act.dpi(72), act.dpi(96))
            val socialH = act.dpi(if (dense) 60 else 72)
            val sitesH = act.dpi(if (dense) 56 else 78)
            val gridMin = act.dpi(if (dense) 68 else 148)
            val heroH = min(hero.preferredHeight(cw), (ch - liveH - sitesH - socialH - gridMin - gap * 4).coerceAtLeast(1))
            val gridH = (ch - heroH - liveH - sitesH - socialH - gap * 4).coerceAtLeast(1)
            var y = 0
            boxes.add(intArrayOf(0, y, cw, y + heroH)); y += heroH + gap
            for (bh in listOf(liveH, gridH, sitesH, socialH)) {
                boxes.add(intArrayOf(side, y, cw - side, y + bh)); y += bh + gap
            }
        } else {
            grid.singleRow = true
            val left = (cw * 0.46f).toInt()
            val socialH = act.dpi(60)
            val heroH = (ch - socialH - gap).coerceAtLeast(1)
            val liveH = min(act.dpi(90), maxOf(act.dpi(60), (ch * 0.30f).toInt()))
            val sitesH = act.dpi(52)
            boxes.add(intArrayOf(0, 0, left - gap, heroH))
            boxes.add(intArrayOf(left + gap, 0, cw - side, liveH))
            boxes.add(intArrayOf(left + gap, liveH + gap, cw - side, ch - sitesH - gap))
            boxes.add(intArrayOf(left + gap, ch - sitesH, cw - side, ch))
            boxes.add(intArrayOf(side, ch - socialH, left - gap - side, ch))
        }
        for (i in 0 until childCount) {
            val b = boxes[i]
            getChildAt(i).measure(exact(b[2] - b[0]), exact(b[3] - b[1]))
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val box = boxes[i]
            getChildAt(i).layout(paddingLeft + box[0], paddingTop + box[1], paddingLeft + box[2], paddingTop + box[3])
        }
    }
}

private fun exact(v: Int) = View.MeasureSpec.makeMeasureSpec(v.coerceAtLeast(1), View.MeasureSpec.EXACTLY)

private fun fitted(ctx: Context, text: String, min: Int, max: Int, lines: Int = 1) = ctx.label(text, max.toFloat()).apply {
    maxLines = lines
    // El máximo sigue la preferencia del sistema; el mínimo permite ajustar TODO
    // el contenido del inicio en ventanas pequeñas, sin cortar líneas ni palabras.
    val minPx = ctx.dpi(min).coerceAtLeast(1)
    val maxPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, max.toFloat(), resources.displayMetrics).toInt().coerceAtLeast(minPx)
    setAutoSizeTextTypeUniformWithConfiguration(minPx, maxPx, 1, TypedValue.COMPLEX_UNIT_PX)
}

private class HomeGrid(private val act: MainActivity) : ViewGroup(act) {
    var singleRow = false
    fun addTile(icon: Int, title: String, sub: String, desc: String, click: () -> Unit) {
        val card = GlassCard(act, 22f, childCount * 1.7f)
        card.addView(TileContent(act, icon, title, sub, childCount * 1.3f), FrameLayout.LayoutParams(MATCH, MATCH))
        card.contentDescription = desc
        card.setOnClickListener { click() }
        addView(card)
    }
    override fun onMeasure(ws: Int, hs: Int) {
        val w = MeasureSpec.getSize(ws); val h = MeasureSpec.getSize(hs)
        setMeasuredDimension(w, h)
        val gap = act.dpi(8)
        val columns = if (singleRow) childCount else 2
        val rows = if (singleRow) 1 else 2
        for (i in 0 until childCount) getChildAt(i).measure(exact((w - gap * (columns - 1)) / columns), exact((h - gap * (rows - 1)) / rows))
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val gap = act.dpi(8)
        val columns = if (singleRow) childCount else 2
        for (i in 0 until childCount) {
            val v = getChildAt(i); val x = (i % columns) * (v.measuredWidth + gap); val y = (i / columns) * (v.measuredHeight + gap)
            v.layout(x, y, x + v.measuredWidth, y + v.measuredHeight)
        }
    }
}

/** Tres accesos directos independientes; títulos completos también para TalkBack. */
private class HomeSites(private val act: MainActivity) : ViewGroup(act) {
    init {
        val links = listOf(
            Triple(Links.PODCAST, "Pódcast", R.drawable.ic_gold_podcast),
            Triple(Links.WEB, "Web en español", R.drawable.ic_gold_web),
            Triple(Links.ENGLISH, "Web en inglés", R.drawable.ic_gold_web)
        )
        for ((link, title, res) in links) {
            val card = GlassCard(act, 16f)
            val column = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(act.dpi(3), act.dpi(3), act.dpi(3), act.dpi(3))
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
            column.addView(ImageView(act).apply { setImageResource(res); scaleType = ImageView.ScaleType.FIT_CENTER },
                LinearLayout.LayoutParams(MATCH, 0, 0.46f))
            column.addView(fitted(act, title, 10, 16, 2).apply { typeface = Fonts.script; gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(MATCH, 0, 0.54f))
            card.addView(column, FrameLayout.LayoutParams(MATCH, MATCH))
            card.contentDescription = "Abrir " + link.name
            card.setOnClickListener { act.openLink(link) }
            addView(card)
        }
    }

    override fun onMeasure(ws: Int, hs: Int) {
        val w = MeasureSpec.getSize(ws); val h = MeasureSpec.getSize(hs)
        setMeasuredDimension(w, h)
        val gap = act.dpi(6)
        for (i in 0 until childCount) getChildAt(i).measure(exact((w - gap * 2) / 3), exact(h))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val gap = act.dpi(6)
        for (i in 0 until childCount) {
            val v = getChildAt(i); val x = i * (v.measuredWidth + gap)
            v.layout(x, 0, x + v.measuredWidth, height)
        }
    }
}

private class TileContent(ctx: Context, res: Int, title: String, sub: String, private val phase: Float) : ViewGroup(ctx), Animated {
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var glowRadius = 0f
    private val icon = ImageView(ctx).apply { setImageResource(res); scaleType = ImageView.ScaleType.FIT_CENTER }
    private val name = fitted(ctx, title, 10, 21).apply { typeface = Fonts.script; gravity = Gravity.CENTER }
    private val subtitle = fitted(ctx, sub, 8, 13).apply { setTextColor(Pal.LILAC); gravity = Gravity.CENTER }
    private var iconH = 0; private var nameH = 0; private var subH = 0
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addView(icon); addView(name); addView(subtitle)
    }
    override fun onMeasure(ws: Int, hs: Int) {
        val w = MeasureSpec.getSize(ws); val h = MeasureSpec.getSize(hs)
        setMeasuredDimension(w, h)
        val compact = h < context.dpi(108)
        subtitle.maxLines = if (compact) 2 else 1
        iconH = min(context.dpi(48), (h * if (compact) 0.30f else 0.39f).toInt())
        nameH = (h * if (compact) 0.25f else 0.27f).toInt()
        subH = (h * if (compact) 0.34f else 0.22f).toInt()
        icon.measure(exact(w - context.dpi(12)), exact(iconH))
        name.measure(exact(w - context.dpi(8)), exact(nameH))
        subtitle.measure(exact(w - context.dpi(8)), exact(subH))
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var y = (height - iconH - nameH - subH) / 2
        icon.layout(context.dpi(6), y, width - context.dpi(6), y + iconH); y += iconH
        name.layout(context.dpi(4), y, width - context.dpi(4), y + nameH); y += nameH
        subtitle.layout(context.dpi(4), y, width - context.dpi(4), y + subH)
    }

    /** Resplandor dorado que respira detrás del icono. */
    override fun dispatchDraw(c: Canvas) {
        if (iconH > 0) {
            val r = iconH * 0.95f
            if (glowPaint.shader == null || r != glowRadius) {
                glowRadius = r
                glowPaint.shader = RadialGradient(0f, 0f, r, withAlpha(Pal.GOLD, 0.34f), withAlpha(Pal.GOLD, 0f), Shader.TileMode.CLAMP)
            }
            val k = 0.65f + 0.35f * sin(Clock.t * 1.7f + phase)
            glowPaint.alpha = (255f * k).toInt().coerceIn(0, 255)
            c.save()
            c.translate(icon.left + icon.width / 2f, icon.top + icon.height / 2f + icon.translationY)
            c.drawCircle(0f, 0f, r, glowPaint)
            c.restore()
        }
        super.dispatchDraw(c)
    }

    /** El icono flota suavemente, cada tarjeta con su propio desfase. */
    override fun tick(t: Float) {
        icon.translationY = sin(t * 1.6f + phase) * context.dp(2.5f)
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

private class HomeSocials(private val act: MainActivity) : ViewGroup(act), Animated {
    private val caption = fitted(act, "Síguenos en nuestras redes", 8, 15).apply { typeface = Fonts.script; gravity = Gravity.CENTER }
    init {
        addView(caption)
        val links = listOf(Links.FACEBOOK to R.drawable.ic_c_facebook, Links.INSTAGRAM to R.drawable.ic_c_instagram,
            Links.TELEGRAM to R.drawable.ic_c_telegram, Links.X to R.drawable.ic_c_x)
        for ((link, res) in links) {
            addView(button(res, "Abrir " + link.name) { act.openLink(link) }.apply {
                setOnLongClickListener { act.toggleFavorite(link); true }
            })
        }
        addView(button(R.drawable.ic_mail, "Enviar un correo a " + Links.EMAIL) { act.sendMail() })
    }
    private fun button(res: Int, desc: String, click: () -> Unit) = FrameLayout(act).apply {
        contentDescription = desc
        isFocusable = true
        setOnClickListener { click() }
        val icon = ImageView(act).apply {
            setImageResource(res)
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        if (res == R.drawable.ic_mail) {
            val disc = FrameLayout(act).apply {
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Pal.VIOLET, Pal.BRAND)).apply {
                    shape = GradientDrawable.OVAL
                    setStroke(act.dpi(1), Pal.PINK)
                }
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
            icon.setColorFilter(Pal.WHITE)
            disc.addView(icon, FrameLayout.LayoutParams(act.dpi(24), act.dpi(24), Gravity.CENTER))
            addView(disc, FrameLayout.LayoutParams(act.dpi(38), act.dpi(38), Gravity.CENTER))
        } else addView(icon, FrameLayout.LayoutParams(act.dpi(36), act.dpi(36), Gravity.CENTER))
    }
    override fun onMeasure(ws: Int, hs: Int) {
        val w = MeasureSpec.getSize(ws); val h = MeasureSpec.getSize(hs)
        setMeasuredDimension(w, h)
        val labelH = (h - act.dpi(48)).coerceAtLeast(act.dpi(12))
        caption.measure(exact(w), exact(labelH))
        for (i in 1 until childCount) getChildAt(i).measure(exact(w / 5), exact(h - labelH))
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        caption.layout(0, 0, width, caption.measuredHeight)
        for (i in 1 until childCount) {
            val x = (i - 1) * width / 5
            getChildAt(i).layout(x, caption.measuredHeight, i * width / 5, height)
        }
    }

    /** Cada 6 s los iconos de redes dan un saltito en cadena, como invitación a seguirnos. */
    override fun tick(t: Float) {
        val cycle = t % 6f
        for (i in 1 until childCount) {
            val k = (cycle - (i - 1) * 0.12f) / 0.5f
            val bump = if (k in 0f..1f) sin(k * PI.toFloat()) else 0f
            getChildAt(i).translationY = -act.dp(6f) * bump
        }
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
