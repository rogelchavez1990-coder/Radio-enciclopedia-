package cu.radioenciclopedia.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.DayOfWeek
import java.time.ZonedDateTime
import kotlin.math.min
import kotlin.math.sin

/** Parrilla sin conexión, documentada por la emisora y el dossier visual aportado. */
class ProgramsView(private val act: MainActivity) : ScrollView(act) {
    private val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
    private val list = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
    private val now = act.label("", 17f, Pal.WHITE, true)
    private val today = act.label("", 13f, Pal.LILAC)
    private val progress = ProgressLine(act)
    private val endsAt = act.label("", 12f, Pal.LILAC)
    private val filters = ArrayList<TextView>()
    private var mode = 0
    private var dateKey = ""
    // Cada fila: emisión, título y distintivo «AL AIRE» (visible solo en la emisión actual).
    private val rowViews = ArrayList<Triple<Broadcast, TextView, TextView>>()

    init {
        isVerticalScrollBarEnabled = false
        addView(col, FrameLayout.LayoutParams(if (resources.configuration.screenWidthDp > 560) act.dpi(560) else MATCH, WRAP, Gravity.CENTER_HORIZONTAL))
        val header = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(EmblemView(act), LinearLayout.LayoutParams(act.dpi(66), act.dpi(76)))
        val heading = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(act.sectionTitle("Programas", 32, 20), LinearLayout.LayoutParams(MATCH, WRAP))
        heading.addView(act.label("Horarios de los 15 programas · Cuba", 14f, Pal.LILAC))
        header.addView(heading, LinearLayout.LayoutParams(0, WRAP, 1f))
        header.addView(act.label("☰", 28f).apply {
            gravity = Gravity.CENTER; contentDescription = "Abrir menú"; isFocusable = true
            setOnClickListener { act.openMenu() }
        }, LinearLayout.LayoutParams(act.dpi(48), act.dpi(48)))
        col.addView(header, lp(6))

        val live = GlassCard(act, 22f)
        val info = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dpi(16), act.dpi(14), act.dpi(16), act.dpi(14)) }
        info.addView(act.label("AHORA, SEGÚN LA PARRILLA", 11f, Pal.GOLD, true))
        info.addView(now, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(4) })
        info.addView(today, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(3) })
        info.addView(progress, LinearLayout.LayoutParams(MATCH, act.dpi(4)).apply { topMargin = act.dpi(12) })
        info.addView(endsAt, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(6) })
        live.addView(info, FrameLayout.LayoutParams(MATCH, WRAP))
        live.contentDescription = "Escuchar la emisora en vivo"
        live.setOnClickListener { act.openLink(Links.AUDIO) }
        col.addView(live, lp(10))

        addLink("Lista de horarios", "Todos los programas y sus horas de emisión", "Ver lista de horarios") {
            val hours = ProgramSchedule.programs.joinToString("\n\n") { it.title + "\n" + ProgramSchedule.hours(it.id) }
            AlertDialog.Builder(act).setTitle("Horarios de los programas")
                .setMessage("Hora de Cuba · R: retransmisión.\n\n" + hours)
                .setPositiveButton("Parrilla oficial") { _, _ -> act.openLink(Links.SCHEDULE) }
                .setNegativeButton("Cerrar", null).show()
        }

        val filterRow = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, text) in listOf("Hoy", "Lun–sáb", "Domingo").withIndex()) {
            val chip = act.label(text, 15f, Pal.WHITE, true).apply {
                gravity = Gravity.CENTER; contentDescription = "Mostrar programación: $text"
                isFocusable = true; setPadding(0, act.dpi(10), 0, act.dpi(10))
                setOnClickListener { mode = i; rebuild(); refreshNow() }
            }
            filterRow.addView(chip, LinearLayout.LayoutParams(0, act.dpi(48), 1f))
            filters.add(chip)
        }
        col.addView(filterRow, lp(10))
        col.addView(list, lp(0))
        col.addView(act.label("Horas de inicio en Cuba · R: retransmisión.\nParrilla consultada: ${ProgramSchedule.REVIEWED}. La emisión puede variar.", 12f, Pal.LILAC).apply {
            gravity = Gravity.CENTER; setPadding(0, act.dpi(8), 0, act.dpi(12))
        }, lp(8))
        refreshNow()
    }

    /** Entrada al mostrarse: tarjetas y filas suben con resorte y la barra de progreso barre hasta su sitio. */
    fun playEntrance() {
        var i = 0
        for (k in 0 until col.childCount) {
            val v = col.getChildAt(k)
            if (v === list) continue
            v.springIn(40L + min(i, 6) * 45L, dy = act.dp(22f), fromScale = 0.95f, ratio = 0.7f)
            i++
        }
        for (k in 0 until min(list.childCount, 9)) {
            list.getChildAt(k).springIn(300L + k * 40L, dy = act.dp(26f), fromScale = 0.94f, ratio = 0.72f)
        }
        progress.replay()
    }

    private fun lp(top: Int) = LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(act.dpi(14), act.dpi(top), act.dpi(14), 0) }
    private fun selectedDay(): DayOfWeek = when (mode) { 1 -> DayOfWeek.MONDAY; 2 -> DayOfWeek.SUNDAY; else -> ZonedDateTime.now(ProgramSchedule.zone).dayOfWeek }

    fun refreshNow() {
        val local = ZonedDateTime.now(ProgramSchedule.zone)
        now.text = ProgramSchedule.currentTitle()
        today.text = "%02d:%02d · Hora de La Habana".format(local.hour, local.minute)
        if (dateKey != local.toLocalDate().toString()) { dateKey = local.toLocalDate().toString(); rebuild() }
        val current = ProgramSchedule.current()
        showProgress(current, local)
        for ((slot, text, pill) in rowViews) {
            val sameDayType = (selectedDay() == DayOfWeek.SUNDAY) == (local.dayOfWeek == DayOfWeek.SUNDAY)
            val active = sameDayType && slot == current
            text.setTextColor(if (active) Pal.GOLD else Pal.WHITE)
            pill.visibility = if (active) View.VISIBLE else View.GONE
        }
    }

    /** Muestra cuánto va de la emisión actual y a qué hora termina; se oculta en la continuidad. */
    private fun showProgress(current: Broadcast?, local: ZonedDateTime) {
        if (current == null) {
            progress.visibility = View.GONE
            endsAt.visibility = View.GONE
            return
        }
        val minuteNow = local.hour * 60 + local.minute + local.second / 60f
        val length = (current.endMinute - current.startMinute).coerceAtLeast(1)
        progress.fraction = (minuteNow - current.startMinute) / length
        // Algunos espacios terminan en el minuto 59; la hora que se muestra es la del siguiente inicio.
        val end = (if (current.endMinute % 60 == 59) current.endMinute + 1 else current.endMinute) % 1440
        endsAt.text = "Termina a las %02d:%02d".format(end / 60, end % 60)
        progress.visibility = View.VISIBLE
        endsAt.visibility = View.VISIBLE
    }

    /** Distintivo dorado que late y marca el programa que está en emisión según la parrilla. */
    private fun airPill(): TextView = AirPill(act)

    private fun rebuild() {
        list.removeAllViews(); rowViews.clear()
        for ((i, chip) in filters.withIndex()) {
            chip.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = act.dp(16f); setColor(if (i == mode) Pal.BRAND else withAlpha(Pal.BRAND, 0.15f))
            }
        }
        for (slot in ProgramSchedule.day(selectedDay())) {
            val program = ProgramSchedule.program(slot)
            val card = GlassCard(act, 18f)
            val row = LinearLayout(act).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(act.dpi(12), act.dpi(12), act.dpi(12), act.dpi(12))
            }
            val time = act.label(slot.time(), 17f, Pal.GOLD, true)
            row.addView(time, LinearLayout.LayoutParams(act.dpi(52), WRAP))
            val texts = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            val title = act.label(program.title, 19f).apply { typeface = Fonts.script }
            texts.addView(title)
            val pill = airPill()
            texts.addView(pill, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = act.dpi(4) })
            if (slot.replay) texts.addView(act.label("Retransmisión", 12f, Pal.LILAC))
            row.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))
            row.addView(act.label("›", 24f, Pal.LILAC), LinearLayout.LayoutParams(act.dpi(15), WRAP))
            card.addView(row, FrameLayout.LayoutParams(MATCH, WRAP))
            card.contentDescription = "Ver programa: ${program.title}, ${slot.time()}" + if (slot.replay) ", retransmisión" else ""
            card.setOnClickListener { showProgram(program) }
            list.addView(card, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(7) })
            rowViews.add(Triple(slot, title, pill))
        }
    }

    private fun showProgram(program: RadioProgram) {
        AlertDialog.Builder(act).setTitle(program.title)
            .setMessage(program.description + "\n\nHorarios de inicio (Cuba)\n" + ProgramSchedule.hours(program.id) + "\n\nR: retransmisión.")
            .setPositiveButton("Escuchar emisora") { _, _ -> act.openLink(Links.AUDIO) }
            .setNeutralButton("Parrilla oficial") { _, _ -> act.openLink(Links.SCHEDULE) }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun addLink(title: String, subtitle: String, desc: String, click: () -> Unit) {
        val card = GlassCard(act, 18f)
        val text = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dpi(16), act.dpi(12), act.dpi(16), act.dpi(12)) }
        text.addView(act.label(title, 19f).apply { typeface = Fonts.script })
        text.addView(act.label(subtitle, 13f, Pal.LILAC))
        card.addView(text, FrameLayout.LayoutParams(MATCH, WRAP))
        card.contentDescription = desc; card.setOnClickListener { click() }
        col.addView(card, lp(9))
    }
}

/** Línea fina dorada que indica cuánto va de la emisión actual; al entrar barre con un resorte. */
private class ProgressLine(ctx: Context) : View(ctx) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Pal.WHITE, 0.14f) }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Pal.GOLD }
    private val rect = RectF()
    private var target = 0f
    private var shown = 0f
    private val anim = SpringAnim(Spring(0f, 110f, 0.85f)) { shown = it; invalidate() }

    var fraction: Float
        get() = target
        set(value) {
            target = value.coerceIn(0f, 1f)
            anim.jump(target)
        }

    /** Vuelve a cero y barre hasta el valor actual. */
    fun replay() {
        anim.jump(0f)
        anim.animateTo(target)
    }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDetachedFromWindow() {
        anim.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val radius = h / 2f
        rect.set(0f, 0f, w, h)
        c.drawRoundRect(rect, radius, radius, track)
        if (shown > 0f) {
            rect.set(0f, 0f, maxOf(w * shown, h), h)
            c.drawRoundRect(rect, radius, radius, bar)
        }
    }
}

/** «● AL AIRE»: el borde y el relleno dorado laten con el reloj único de animaciones. */
private class AirPill(private val act: MainActivity) : TextView(act), Animated {
    private val shape = GradientDrawable().apply {
        cornerRadius = act.dp(10f)
        setColor(withAlpha(Pal.GOLD, 0.16f))
        setStroke(act.dpi(1), withAlpha(Pal.GOLD, 0.70f))
    }

    init {
        text = "● AL AIRE"
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
        setTextColor(Pal.GOLD)
        typeface = Fonts.bold
        letterSpacing = 0.08f
        includeFontPadding = false
        visibility = View.GONE
        setPadding(act.dpi(8), act.dpi(3), act.dpi(8), act.dpi(3))
        background = shape
    }

    override fun tick(t: Float) {
        val k = 0.5f + 0.5f * sin(t * 3.2f)
        shape.setStroke(act.dpi(1), withAlpha(Pal.GOLD, 0.35f + 0.60f * k))
        shape.setColor(withAlpha(Pal.GOLD, 0.10f + 0.14f * k))
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
