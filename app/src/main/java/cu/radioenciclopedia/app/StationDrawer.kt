package cu.radioenciclopedia.app

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Navegación de la referencia visual. El panel puede desplazarse; Inicio permanece fijo. */
class StationDrawer(private val act: MainActivity) : FrameLayout(act) {
    private val panel = ScrollView(act)
    private val rows = HashMap<Int, GlassCard>()
    private val allRows = ArrayList<View>()
    private val scrim = View(act)
    private val panelAnim = SpringAnim(Spring(1f, 380f, 0.8f)) { p ->
        panel.translationX = -panelWidth() * (1f - p)
        panel.alpha = (0.35f + 0.65f * p).coerceIn(0f, 1f)
    }

    init {
        isFocusableInTouchMode = true
        scrim.apply {
            setBackgroundColor(0xAD000000.toInt())
            contentDescription = "Cerrar menú tocando fuera"
            setOnClickListener { act.closeMenu() }
        }
        addView(scrim, LayoutParams(MATCH, MATCH))
        panel.isVerticalScrollBarEnabled = false
        panel.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF160C26.toInt(), 0xFF25113E.toInt(), Pal.DEEP)).apply { cornerRadius = act.dp(22f) }
        addView(panel, LayoutParams((resources.displayMetrics.widthPixels * 0.90f).toInt(), MATCH, Gravity.START))
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        panel.addView(col, LayoutParams(MATCH, WRAP))

        val head = FrameLayout(act)
        head.addView(OfficialLogoView(act), LayoutParams(MATCH, act.dpi(92)).apply { rightMargin = act.dpi(25) })
        head.addView(act.label("La Habana · Cuba", 20f, Pal.GOLD).apply { typeface = Fonts.script; gravity = Gravity.CENTER },
            LayoutParams(MATCH, act.dpi(28), Gravity.BOTTOM))
        head.addView(TextView(act).apply {
            text = "×"; textSize = 30f; gravity = Gravity.CENTER; setTextColor(Pal.WHITE)
            contentDescription = "Cerrar menú"; isFocusable = true; setOnClickListener { act.closeMenu() }
        }, LayoutParams(act.dpi(44), act.dpi(44), Gravity.TOP or Gravity.END))
        col.addView(head, LinearLayout.LayoutParams(MATCH, act.dpi(124)))

        fun row(label: String, icon: Int, cd: String, screen: Int? = null, action: () -> Unit) {
            val card = GlassCard(act, 19f)
            val line = LinearLayout(act).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(act.dpi(14), act.dpi(8), act.dpi(12), act.dpi(8))
            }
            val iv = android.widget.ImageView(act).apply {
                setImageResource(icon)
                if (icon != R.drawable.ic_menu_video) setColorFilter(Pal.WHITE)
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            line.addView(iv, LinearLayout.LayoutParams(act.dpi(28), act.dpi(28)))
            line.addView(act.label(label, 20f).apply { typeface = Fonts.cond }, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginStart = act.dpi(15) })
            if (screen == 0) line.addView(act.label("›", 28f), LinearLayout.LayoutParams(WRAP, WRAP))
            card.addView(line, LayoutParams(MATCH, MATCH))
            card.contentDescription = cd
            card.setOnClickListener { act.closeMenu(); action() }
            col.addView(card, LinearLayout.LayoutParams(MATCH, act.dpi(51)).apply { topMargin = act.dpi(6) })
            if (screen != null) rows[screen] = card
            allRows.add(card)
        }
        row("Inicio", R.drawable.ic_nav_home, "Ir a Inicio", 0) { act.goTo(0) }
        row("Escuchar en vivo", R.drawable.ic_nav_listen, "Ir a Escuchar", 1) { act.goTo(1) }
        row("Programas", R.drawable.ic_nav_programs, "Ir a Programas", 2) { act.goTo(2) }
        row("YouTube", R.drawable.ic_menu_video, "Abrir VIDEOS") { act.openLink(Links.VIDEOS) }
        row("Comunidad", R.drawable.ic_nav_community, "Ir a Comunidad", 3) { act.goTo(3) }
        row("Contacto", R.drawable.ic_nav_contact, "Ir a Contacto", 4) { act.goTo(4) }
        row("Frecuencias FM", R.drawable.ic_gold_radio, "Ir a Frecuencias", 5) { act.goTo(5) }
        row("Acerca de", R.drawable.ic_info, "Acerca de Radio Enciclopedia") { act.showAbout() }

        val fav = GlassCard(act, 24f)
        fav.addView(act.label("★  Favoritos", 21f, Pal.GOLD, true).apply { gravity = Gravity.CENTER }, LayoutParams(MATCH, MATCH))
        fav.contentDescription = "Ver favoritos"
        fav.setOnClickListener { act.showFavorites() }
        col.addView(fav, LinearLayout.LayoutParams(MATCH, act.dpi(48)).apply { topMargin = act.dpi(6) })
        allRows.add(fav)
    }

    private fun panelWidth(): Float = minOf(resources.displayMetrics.widthPixels * 0.90f, act.dp(420f))

    /** Entrada: el panel llega con un resorte y las filas suben una tras otra, como un dial que se enciende. */
    fun animateIn() {
        scrim.animate().cancel()
        panel.animate().cancel()
        panelAnim.cancel()
        if (!Motion.enabled()) {
            scrim.alpha = 1f
            panelAnim.jump(1f)
            for (v in allRows) v.springIn(0L)
            return
        }
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(240L).start()
        panelAnim.jump(0f)
        panelAnim.animateTo(1f)
        allRows.forEachIndexed { i, v ->
            v.springIn(70L + i * 38L, dx = -act.dp(34f), fromScale = 0.94f, ratio = 0.7f, stiffness = 320f)
        }
    }

    /** Salida rápida; [end] se ejecuta al terminar (para ocultar el menú). */
    fun animateOut(end: () -> Unit) {
        scrim.animate().cancel()
        panel.animate().cancel()
        panelAnim.cancel()
        if (!Motion.enabled()) {
            end()
            return
        }
        scrim.animate().alpha(0f).setDuration(170L).start()
        panel.animate().translationX(-panelWidth() * 0.7f).alpha(0f).setDuration(170L)
            .withEndAction { end() }.start()
    }

    /** Vista previa del gesto «atrás»: el panel se aparta y el fondo se aclara mientras el dedo avanza. */
    fun backPreview(p: Float) {
        panel.translationX = -act.dp(56f) * p
        panel.scaleX = 1f - 0.04f * p
        panel.scaleY = 1f - 0.04f * p
        panel.pivotX = 0f
        scrim.alpha = 1f - 0.55f * p
    }

    fun backReset() {
        panel.animate().translationX(0f).scaleX(1f).scaleY(1f).setDuration(180L).start()
        scrim.animate().alpha(1f).setDuration(180L).start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        panel.layoutParams = (panel.layoutParams as LayoutParams).apply { width = minOf((w * 0.90f).toInt(), act.dpi(420)) }
    }

    fun setSafeInsets(l: Int, t: Int, r: Int, b: Int) { panel.setPadding(l + act.dpi(12), t + act.dpi(8), act.dpi(12), b + act.dpi(12)) }

    fun markCurrent(current: Int) {
        for ((index, card) in rows) {
            card.background = if (index == current) GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Pal.BRAND, Pal.MAGENTA, Pal.VIOLET)).apply { cornerRadius = act.dp(19f) } else null
        }
    }
}
