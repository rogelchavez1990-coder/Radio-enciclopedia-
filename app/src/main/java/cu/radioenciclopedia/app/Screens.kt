package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Una pantalla: su ScrollView y las vistas que entran escalonadas al mostrarse. */
class Screen(
    val scroll: ViewGroup,
    val items: List<View>,
    val refresh: (() -> Unit)? = null,
    /** Se ejecuta cada vez que la pantalla pasa a mostrarse (para animaciones de entrada propias). */
    val onShow: (() -> Unit)? = null
)

/** Construye las secciones de la emisora y sus accesos. */
class ScreenFactory(private val act: MainActivity) {

    private val ctx: Context = act

    private fun dp(v: Int): Int = ctx.dpi(v)

    // ------------------------------------------------------------------ utilidades de construcción

    private class Base(val scroll: ScrollView, val col: LinearLayout)

    private fun newBase(): Base {
        val sv = ScrollView(ctx)
        sv.isVerticalScrollBarEnabled = false
        sv.overScrollMode = View.OVER_SCROLL_NEVER
        sv.clipToPadding = false
        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        // En tabletas y en horizontal el contenido se centra con un ancho cómodo de lectura.
        val wide = ctx.resources.configuration.screenWidthDp > 560
        val lp = FrameLayout.LayoutParams(if (wide) dp(560) else MATCH, WRAP, Gravity.CENTER_HORIZONTAL)
        sv.addView(col, lp)
        return Base(sv, col)
    }

    private fun colLp(top: Int = 0, side: Int = 16, bottom: Int = 0): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.setMargins(dp(side), dp(top), dp(side), dp(bottom))
        return lp
    }

    /** Una sola línea que reduce su tamaño si el texto no cabe (pantallas estrechas). */
    private fun fit(tv: TextView, minSp: Int, maxSp: Int): TextView {
        tv.maxLines = 1
        tv.setAutoSizeTextTypeUniformWithConfiguration(minSp, maxSp, 1, TypedValue.COMPLEX_UNIT_SP)
        return tv
    }

    /** Diámetro de los botones redondos de redes: caben siempre cinco en una fila. */
    private fun socialSize(): Int {
        val w = minOf(ctx.resources.configuration.screenWidthDp, 560)
        return ((w - 32 - 5 * 10) / 5).coerceIn(40, 54)
    }

    private fun noA11y(v: View) {
        v.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun icon(res: Int, sizeDp: Int): ImageView {
        val iv = ImageView(ctx)
        iv.setImageResource(res)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        noA11y(iv)
        iv.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return iv
    }

    /** Teléfono que «suena» de vez en cuando. */
    private fun ringingIcon(res: Int, sizeDp: Int): ImageView {
        val iv = RingingIcon(ctx)
        iv.setImageResource(res)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        noA11y(iv)
        iv.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return iv
    }

    private fun shadowed(tv: TextView, color: Int = withAlpha(Pal.VIOLET, 0.9f)): TextView {
        tv.setShadowLayer(dp(10).toFloat(), 0f, dp(2).toFloat(), color)
        return tv
    }

    /** Encabezado de las pantallas interiores: emblema pequeño + título + subtítulo. */
    private fun header(col: LinearLayout, title: String, sub: String): View {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(18), dp(12), dp(18), dp(4))
        row.addView(EmblemView(ctx), LinearLayout.LayoutParams(dp(78), dp(78)))
        val tx = LinearLayout(ctx)
        tx.orientation = LinearLayout.VERTICAL
        val tt = ctx.sectionTitle(title, 34, 20)
        tx.addView(shadowed(tt), LinearLayout.LayoutParams(MATCH, WRAP))
        val s = ctx.label(sub, 16f, Pal.LILAC)
        s.typeface = Fonts.cond
        val slp = LinearLayout.LayoutParams(WRAP, WRAP)
        slp.topMargin = dp(4)
        tx.addView(s, slp)
        val tlp = LinearLayout.LayoutParams(0, WRAP, 1f)
        tlp.marginStart = dp(10)
        row.addView(tx, tlp)
        row.addView(ctx.label("☰", 27f).apply {
            gravity = Gravity.CENTER
            contentDescription = "Abrir menú"
            isFocusable = true
            setOnClickListener { act.openMenu() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        col.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
        return row
    }

    /** Tarjeta horizontal: icono dorado + título + subtítulo + flecha. */
    private fun listCard(
        iconRes: Int, title: String, sub: String, desc: String, delay: Float, onClick: () -> Unit
    ): GlassCard {
        val card = GlassCard(ctx, 22f, delay)
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(16), dp(14), dp(14), dp(14))
        row.addView(icon(iconRes, 52))
        val tx = LinearLayout(ctx)
        tx.orientation = LinearLayout.VERTICAL
        tx.addView(ctx.label(title, 18f, Pal.WHITE, true))
        val slp = LinearLayout.LayoutParams(WRAP, WRAP)
        slp.topMargin = dp(3)
        tx.addView(ctx.label(sub, 13f, Pal.LILAC), slp)
        val tlp = LinearLayout.LayoutParams(0, WRAP, 1f)
        tlp.marginStart = dp(16)
        row.addView(tx, tlp)
        row.addView(icon(R.drawable.ic_chevron, 24))
        card.addView(row, FrameLayout.LayoutParams(MATCH, WRAP))
        card.contentDescription = desc
        card.setOnClickListener { onClick() }
        return card
    }

    /** Tarjeta cuadrada para la cuadrícula: icono + título + subtítulo centrados. */
    private fun tile(
        iconRes: Int, title: String, sub: String, desc: String, delay: Float, onClick: () -> Unit
    ): GlassCard {
        val card = GlassCard(ctx, 24f, delay)
        val v = LinearLayout(ctx)
        v.orientation = LinearLayout.VERTICAL
        v.gravity = Gravity.CENTER_HORIZONTAL
        v.setPadding(dp(10), dp(16), dp(10), dp(14))
        v.addView(icon(iconRes, 58))
        val t = ctx.label(title, 17f, Pal.WHITE, true)
        t.gravity = Gravity.CENTER
        val tlp = LinearLayout.LayoutParams(WRAP, WRAP)
        tlp.topMargin = dp(8)
        v.addView(t, tlp)
        val s = ctx.label(sub, 12f, Pal.LILAC)
        s.gravity = Gravity.CENTER
        val slp = LinearLayout.LayoutParams(WRAP, WRAP)
        slp.topMargin = dp(2)
        v.addView(s, slp)
        card.addView(v, FrameLayout.LayoutParams(MATCH, WRAP))
        card.contentDescription = desc
        card.setOnClickListener { onClick() }
        return card
    }

    /** Dos tarjetas de la cuadrícula por fila, con el mismo ancho. */
    private fun pair(a: View, b: View): LinearLayout {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        val la = LinearLayout.LayoutParams(0, MATCH, 1f)
        la.marginEnd = dp(6)
        val lb = LinearLayout.LayoutParams(0, MATCH, 1f)
        lb.marginStart = dp(6)
        row.addView(a, la)
        row.addView(b, lb)
        return row
    }

    private fun open(l: Link) = act.openLink(l)

    // ------------------------------------------------------------------ 1. INICIO

    fun inicio(): Screen {
        val home = CompactHomeView(act)
        return Screen(home, home.entryItems) { home.refreshSchedule() }
    }

    private fun circleButton(res: Int, desc: String, onClick: () -> Unit): View {
        val size = socialSize()
        val fr = FrameLayout(ctx)
        val ring = GradientDrawable()
        ring.shape = GradientDrawable.OVAL
        ring.setColor(withAlpha(Pal.DEEP, 0.4f))
        ring.setStroke(dp(2), withAlpha(Pal.GOLD, 0.9f))
        fr.background = ring
        fr.isClickable = true
        fr.isFocusable = true
        fr.contentDescription = desc
        val iv = ImageView(ctx)
        iv.setImageResource(res)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        noA11y(iv)
        fr.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH).apply {
            setMargins(dp(4), dp(4), dp(4), dp(4))
        })
        fr.setOnClickListener { onClick() }
        fr.setOnTouchListener { v, e ->
            if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return@setOnTouchListener false
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90L).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150L).start()
            }
            false
        }
        val lp = LinearLayout.LayoutParams(dp(size), dp(size))
        lp.setMargins(dp(5), 0, dp(5), 0)
        fr.layoutParams = lp
        return fr
    }

    private fun mailButton(): View {
        val size = socialSize()
        val fr = FrameLayout(ctx)
        val ring = GradientDrawable()
        ring.shape = GradientDrawable.OVAL
        ring.setColor(withAlpha(Pal.VIOLET, 0.55f))
        ring.setStroke(dp(2), withAlpha(Pal.GOLD, 0.9f))
        fr.background = ring
        fr.isClickable = true
        fr.isFocusable = true
        fr.contentDescription = "Enviar un correo a " + Links.EMAIL
        val iv = ImageView(ctx)
        iv.setImageResource(R.drawable.ic_mail)
        iv.setColorFilter(Pal.WHITE)
        noA11y(iv)
        fr.addView(iv, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        fr.setOnClickListener { act.sendMail() }
        val lp = LinearLayout.LayoutParams(dp(size), dp(size))
        lp.setMargins(dp(5), 0, dp(5), 0)
        fr.layoutParams = lp
        return fr
    }

    // ------------------------------------------------------------------ 2. ESCUCHAR

    fun escuchar(): Screen {
        val view = ListeningView(act)
        return Screen(view, listOf(view.playerCard), refresh = { view.refresh() }, onShow = { view.tuneIn() })
    }

    fun frecuencias(): Screen = Screen(FrequenciesView(act), emptyList())

    // ------------------------------------------------------------------ 3. PROGRAMAS

    fun programas(): Screen {
        val view = ProgramsView(act)
        return Screen(view, emptyList(), refresh = { view.refreshNow() }, onShow = { view.playEntrance() })
    }

    // ------------------------------------------------------------------ 4. COMUNIDAD

    fun comunidad(): Screen {
        val b = newBase()
        val col = b.col
        val items = ArrayList<View>()
        items.add(header(col, "Comunidad", "La cultura nos une"))
        val culture = MartiCultureCard(act)
        col.addView(culture, colLp(top = 14))
        items.add(culture)

        val fb = socialTile(R.drawable.ic_c_facebook, Links.FACEBOOK, "/radioenciclopedia", 0.2f)
        val ig = socialTile(R.drawable.ic_c_instagram, Links.INSTAGRAM, "@radio_enciclopedia", 1.0f)
        val tg = socialTile(R.drawable.ic_c_telegram, Links.TELEGRAM, "@radio_enciclopedia7_cu", 1.8f)
        val xx = socialTile(R.drawable.ic_c_x, Links.X, "@enciclopedia_cu", 2.6f)
        val r1 = pair(fb, tg)
        val r2 = pair(ig, xx)
        col.addView(r1, colLp(top = 14))
        col.addView(r2, colLp(top = 12))
        items.add(r1)
        items.add(r2)

        return Screen(b.scroll, items, onShow = { culture.playEntrance() })
    }

    private fun socialTile(res: Int, link: Link, handle: String, delay: Float): GlassCard {
        val card = GlassCard(ctx, 24f, delay)
        val v = LinearLayout(ctx)
        v.orientation = LinearLayout.VERTICAL
        v.gravity = Gravity.CENTER_HORIZONTAL
        v.setPadding(dp(8), dp(18), dp(8), dp(16))
        v.addView(icon(res, 64))
        val t = ctx.label(link.name.lowercase().replaceFirstChar { it.uppercase() }, 17f, Pal.WHITE, true)
        t.gravity = Gravity.CENTER
        val tlp = LinearLayout.LayoutParams(WRAP, WRAP)
        tlp.topMargin = dp(10)
        v.addView(t, tlp)
        val s = ctx.label(handle, 11f, Pal.LILAC)
        s.gravity = Gravity.CENTER
        fit(s, 8, 11)
        val slp = LinearLayout.LayoutParams(MATCH, WRAP)
        slp.topMargin = dp(3)
        v.addView(s, slp)
        card.addView(v, FrameLayout.LayoutParams(MATCH, WRAP))
        card.contentDescription = "Abrir " + link.name
        card.setOnClickListener { open(link) }
        return card
    }

    // ------------------------------------------------------------------ 5. CONTACTO

    fun contacto(): Screen {
        val b = newBase()
        val col = b.col
        val items = ArrayList<View>()
        items.add(header(col, "Contacto", "El Vedado, La Habana"))

        // Tarjeta institucional con la firma de la referencia del propietario.
        val info = GlassCard(ctx, 26f, 0.4f, false)
        val iv = LinearLayout(ctx)
        iv.orientation = LinearLayout.VERTICAL
        iv.setPadding(dp(18), dp(18), dp(18), dp(18))
        val plate = FrameLayout(ctx)
        val pbg = GradientDrawable()
        pbg.setColor(withAlpha(Pal.BRAND, 0.10f))
        pbg.cornerRadius = ctx.dp(16f)
        pbg.setStroke(dp(2), Pal.GOLD)
        plate.background = pbg
        plate.setPadding(dp(10), dp(8), dp(10), dp(8))
        val logo = OfficialLogoView(ctx)
        logo.adjustViewBounds = true
        logo.scaleType = ImageView.ScaleType.FIT_CENTER
        logo.contentDescription = ctx.getString(R.string.contact_station)
        plate.addView(logo, FrameLayout.LayoutParams(MATCH, dp(95)))
        iv.addView(plate, LinearLayout.LayoutParams(MATCH, WRAP))
        val lines = listOf(
            ctx.getString(R.string.contact_address),
            ctx.getString(R.string.contact_zip),
            ctx.getString(R.string.contact_director)
        )
        for ((i, line) in lines.withIndex()) {
            val tv = ctx.label(line, 14f, Pal.WHITE)
            tv.setLineSpacing(dp(2).toFloat(), 1f)
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.topMargin = dp(if (i == 0) 14 else 6)
            iv.addView(tv, lp)
        }
        info.addView(iv, FrameLayout.LayoutParams(MATCH, WRAP))
        col.addView(info, colLp(top = 14))
        items.add(info)

        val ph = ctx.label("Teléfonos", 14f, Pal.GOLD, true)
        ph.letterSpacing = 0.06f
        col.addView(ph, colLp(top = 20, side = 22))
        items.add(ph)
        for ((i, p) in Links.PHONES.withIndex()) {
            val row = contactRow(R.drawable.ic_phone, p.first, "Llamar",
                ctx.getString(R.string.cd_call, p.first), 0.8f + i * 0.7f) { act.dial(p.second) }
            col.addView(row, colLp(top = 8))
            items.add(row)
        }

        val em = ctx.label(ctx.getString(R.string.contact_email_label), 14f, Pal.GOLD, true)
        em.letterSpacing = 0.06f
        col.addView(em, colLp(top = 20, side = 22))
        items.add(em)
        val mail = contactRow(R.drawable.ic_mail, Links.EMAIL, "",
            ctx.getString(R.string.cd_email, Links.EMAIL), 3.2f) { act.sendMail() }
        col.addView(mail, colLp(top = 8, bottom = 8))
        items.add(mail)
        return Screen(b.scroll, items)
    }

    private fun contactRow(
        iconRes: Int, text: String, action: String, desc: String, delay: Float, onClick: () -> Unit
    ): GlassCard {
        val card = GlassCard(ctx, 18f, delay)
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(16), dp(14), dp(16), dp(14))
        row.addView(if (iconRes == R.drawable.ic_phone) ringingIcon(iconRes, 24) else icon(iconRes, 24))
        val t = fit(ctx.label(text, 18f, Pal.WHITE, true), 10, 18)
        val tlp = LinearLayout.LayoutParams(0, WRAP, 1f)
        tlp.marginStart = dp(14)
        row.addView(t, tlp)
        if (action.isNotEmpty()) row.addView(ctx.label(action, 13f, Pal.GOLD, true))
        else row.addView(icon(R.drawable.ic_chevron, 24))
        card.addView(row, FrameLayout.LayoutParams(MATCH, WRAP))
        card.contentDescription = desc
        card.setOnClickListener { onClick() }
        return card
    }
}
