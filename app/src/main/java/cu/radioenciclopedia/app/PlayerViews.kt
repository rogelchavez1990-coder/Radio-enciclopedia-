package cu.radioenciclopedia.app

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView

private fun MainActivity.playerText(value: String, min: Int, max: Int) = label(value, max.toFloat()).apply {
    maxLines = 1
    val lo = dpi(min)
    val hi = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, max.toFloat(), resources.displayMetrics).toInt().coerceAtLeast(lo)
    setAutoSizeTextTypeUniformWithConfiguration(lo, hi, 1, TypedValue.COMPLEX_UNIT_PX)
}

/** Un único acceso de reproducción en Inicio, sincronizado con el servicio. */
class InlinePlayerView(private val act: MainActivity) : GlassCard(act, 24f, 2f) {
    private val play = PlayButtonView(act)
    private val meter = EqualizerView(act, 7)
    private val stationListener: (Station) -> Unit = { refresh() }
    private val flag = act.playerText("94,1 FM · La Habana", 9, 14).apply { setTextColor(Pal.GOLD); typeface = Fonts.bold }
    private val program = act.playerText("", 12, 21).apply { typeface = Fonts.script }
    private val status = act.playerText("", 8, 12).apply { setTextColor(Pal.LILAC) }
    private val listener: (PlaybackInfo) -> Unit = { refresh() }

    init {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(act.dpi(5), act.dpi(5), act.dpi(12), act.dpi(5))
        }
        play.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        meter.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        row.addView(play, LinearLayout.LayoutParams(act.dpi(58), MATCH))
        val words = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        words.addView(flag, LinearLayout.LayoutParams(MATCH, 0, 1f))
        words.addView(program, LinearLayout.LayoutParams(MATCH, 0, 1.5f))
        words.addView(status, LinearLayout.LayoutParams(MATCH, 0, 1f))
        row.addView(words, LinearLayout.LayoutParams(0, MATCH, 1f).apply { marginStart = act.dpi(3) })
        row.addView(meter, LinearLayout.LayoutParams(act.dpi(30), act.dpi(38)).apply { marginStart = act.dpi(4) })
        addView(row, FrameLayout.LayoutParams(MATCH, MATCH))
        contentDescription = "Reproductor de Radio Enciclopedia"
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, RadioPlayback.info.action))
                info.text = RadioPlayback.info.label + ". " + Stations.current.nowTitle()
            }
        }
        setOnClickListener { act.toggleRadio(play) }
        refresh()
    }

    fun refresh() {
        val info = RadioPlayback.info
        val st = Stations.current
        val live = info.status == PlaybackStatus.PLAYING
        flag.text = if (live) "● En directo · ${st.band}" else st.flag
        flag.setTextColor(if (live) Pal.GREEN else if (st === Stations.ENCICLOPEDIA) Pal.GOLD else st.accent)
        program.text = st.nowTitle()
        status.text = info.label
        contentDescription = "Reproductor de " + st.name
        play.invalidate(); meter.invalidate()
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); RadioPlayback.listen(listener); Stations.listen(stationListener); refresh() }
    override fun onDetachedFromWindow() { RadioPlayback.unlisten(listener); Stations.unlisten(stationListener); super.onDetachedFromWindow() }
}

class ListeningView(private val act: MainActivity) : ScrollView(act) {
    private val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
    private val dial = DialView(act)
    private val status = act.label("", 15f, Pal.LILAC).apply { gravity = Gravity.CENTER; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
    private val now = act.label("", 22f).apply { typeface = Fonts.script; gravity = Gravity.CENTER }
    private val button = act.label("", 18f, Pal.INK, true).apply {
        gravity = Gravity.CENTER; setPadding(act.dpi(12), act.dpi(16), act.dpi(12), act.dpi(16))
        background = GradientDrawable().apply { cornerRadius = act.dp(28f); setColor(Pal.ROSE) }
        isFocusable = true; setOnClickListener { act.toggleRadio(dial) }
    }
    private val tuning = TuningScaleView(act).apply { onTune = { s -> act.chooseStation(s) } }
    private val deck = StationDeckView(act)
    private val stationLabel = act.label("RADIO ENCICLOPEDIA", 13f, Pal.GOLD, true).apply { letterSpacing = 0.12f }
    private val ficha = act.label("", 14f, Pal.GOLD, true).apply {
        gravity = Gravity.CENTER; isFocusable = true
        contentDescription = "Ver la ficha de la emisora"
        setPadding(act.dpi(12), act.dpi(10), act.dpi(12), act.dpi(10))
        setOnClickListener { act.showStationInfo(Stations.current) }
    }
    private val chips = PlaceChips(act)
    private val hint = act.label("", 12f, Pal.LILAC).apply {
        gravity = Gravity.CENTER; setPadding(act.dpi(24), 0, act.dpi(24), 0)
    }
    private val stationListener: (Station) -> Unit = { refresh() }
    val playerCard = GlassCard(act, 28f, interactive = false)
    private val listener: (PlaybackInfo) -> Unit = { refresh() }

    init {
        isVerticalScrollBarEnabled = false
        addView(col, FrameLayout.LayoutParams(if (resources.configuration.screenWidthDp > 560) act.dpi(560) else MATCH, WRAP, Gravity.CENTER_HORIZONTAL))
        val head = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(EmblemView(act), LinearLayout.LayoutParams(act.dpi(66), act.dpi(78)))
        head.addView(act.sectionTitle("Escuchar", 32, 20), LinearLayout.LayoutParams(0, WRAP, 1f))
        head.addView(act.label("☰", 28f).apply {
            gravity = Gravity.CENTER; contentDescription = "Abrir menú"; isFocusable = true; setOnClickListener { act.openMenu() }
        }, LinearLayout.LayoutParams(act.dpi(48), act.dpi(48)))
        col.addView(head, lp(6))

        col.addView(deck, LinearLayout.LayoutParams(MATCH, act.dpi(184)).apply { topMargin = act.dpi(2) })
        col.addView(chips, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(4) })
        col.addView(hint, LinearLayout.LayoutParams(MATCH, WRAP))
        col.addView(ficha, lp(2))

        val panel = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(act.dpi(16), act.dpi(12), act.dpi(16), act.dpi(16)) }
        val diameter = minOf(260, (resources.configuration.screenWidthDp - 72).coerceAtLeast(160))
        dial.isFocusable = true
        dial.setOnClickListener { act.toggleRadio(dial) }
        panel.addView(dial, LinearLayout.LayoutParams(act.dpi(diameter), act.dpi(diameter)))
        panel.addView(stationLabel, LinearLayout.LayoutParams(WRAP, WRAP))
        panel.addView(tuning, LinearLayout.LayoutParams(MATCH, act.dpi(92)).apply { topMargin = act.dpi(12) })
        panel.addView(now, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(8) })
        panel.addView(status, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(8); bottomMargin = act.dpi(16) })
        panel.addView(button, LinearLayout.LayoutParams(MATCH, WRAP))
        panel.addView(act.label("Detener radio", 14f, Pal.LILAC).apply {
            gravity = Gravity.CENTER; isFocusable = true; contentDescription = "Detener radio"
            setPadding(0, act.dpi(12), 0, act.dpi(8))
            setOnClickListener { act.startService(Intent(act, RadioService::class.java).setAction(RadioService.STOP)) }
        }, LinearLayout.LayoutParams(MATCH, WRAP))
        playerCard.addView(panel, FrameLayout.LayoutParams(MATCH, WRAP))
        col.addView(playerCard, lp(10))

        addLink("Horarios de los programas", "Consulta qué se emite en hora de Cuba", "Ver horarios de los programas") { act.goTo(2) }
        addLink("Frecuencias nacionales FM", "Encuentra la frecuencia de tu provincia", "Ver frecuencias nacionales") { act.goTo(5) }
        addLink("Página oficial de audio", "Alternativa para escuchar desde el navegador", "Abrir " + Links.AUDIO.name) {
            act.openLink(Link(Links.AUDIO.name, Links.AUDIO.url))
        }
        addLink("Sitio web", "radioenciclopedia.cu", "Abrir " + Links.WEB.name) { act.openLink(Links.WEB) }
        col.addView(act.label("La señal continúa al cambiar de sección o apagar la pantalla. Al reanudar, vuelves al directo.\n\nLa escucha por Internet utiliza tu conexión de datos.", 13f, Pal.LILAC).apply {
            gravity = Gravity.CENTER; setPadding(0, act.dpi(6), 0, act.dpi(16))
        }, lp(10))
        refresh()
    }

    private fun lp(top: Int) = LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(act.dpi(14), act.dpi(top), act.dpi(14), 0) }
    private fun addLink(title: String, subtitle: String, description: String, action: () -> Unit) {
        val card = GlassCard(act, 20f)
        val words = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dpi(16), act.dpi(12), act.dpi(16), act.dpi(12)) }
        words.addView(act.label(title, 19f).apply { typeface = Fonts.script })
        words.addView(act.label(subtitle, 13f, Pal.LILAC))
        card.addView(words, FrameLayout.LayoutParams(MATCH, WRAP))
        card.contentDescription = description; card.setOnClickListener { action() }
        col.addView(card, lp(10))
    }
    /** Al mostrarse «Escuchar»: la aguja barre la banda y se engancha en 94,1. */
    fun tuneIn() = tuning.tuneIn()

    fun refresh() {
        val info = RadioPlayback.info
        val st = Stations.current
        now.text = st.nowTitle()
        hint.text = "Emisora " + (Stations.indexOf(st) + 1) + " de " + Stations.all.size + " · desliza las tarjetas" +
            if (st.fm != null) " o arrastra el dial" else ""
        stationLabel.text = st.name.uppercase()
        stationLabel.setTextColor(if (st === Stations.ENCICLOPEDIA) Pal.GOLD else st.accent)
        ficha.text = "Ficha · " + st.name
        status.text = info.label
        button.text = info.action
        button.contentDescription = info.action
        dial.contentDescription = info.action + " desde el dial"
        dial.invalidate()
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); RadioPlayback.listen(listener); Stations.listen(stationListener); refresh() }
    override fun onDetachedFromWindow() { RadioPlayback.unlisten(listener); Stations.unlisten(stationListener); super.onDetachedFromWindow() }
}

/** Selector de lugar: La Habana, México y España. Al tocar uno se sintoniza su primera emisora. */
class PlaceChips(private val act: MainActivity) : LinearLayout(act) {
    private val items = ArrayList<Pair<Place, android.widget.TextView>>()
    private val listener: (Station) -> Unit = { mark() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        for (place in Place.values()) {
            val chip = act.label(place.label, 13f, Pal.LILAC, true).apply {
                gravity = Gravity.CENTER; isFocusable = true; isClickable = true
                setPadding(act.dpi(14), act.dpi(8), act.dpi(14), act.dpi(8))
                contentDescription = "Emisoras de " + place.label
                setOnClickListener {
                    if (Stations.current.place != place) Stations.inPlace(place).firstOrNull()?.let { act.chooseStation(it) }
                }
            }
            items.add(place to chip)
            addView(chip, LayoutParams(WRAP, WRAP).apply { marginStart = act.dpi(4); marginEnd = act.dpi(4) })
        }
        mark()
    }

    private fun mark() {
        val here = Stations.current.place
        for ((place, chip) in items) {
            val on = place == here
            chip.setTextColor(if (on) Pal.INK else Pal.LILAC)
            chip.background = GradientDrawable().apply {
                cornerRadius = act.dp(18f)
                if (on) setColor(Stations.current.accent) else { setColor(0x00000000); setStroke(act.dpi(1), withAlpha(Pal.LILAC, 0.4f)) }
            }
            chip.isSelected = on
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); Stations.listen(listener); mark() }
    override fun onDetachedFromWindow() { Stations.unlisten(listener); super.onDetachedFromWindow() }
}
