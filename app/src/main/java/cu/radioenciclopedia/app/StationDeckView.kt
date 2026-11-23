package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tarjeta de una emisora. Cada una dibuja su propio motivo animado, relacionado con su carácter:
 * disco y notas (Enciclopedia), sol y ondas de alegría (Progreso), estrella y morse (Rebelde),
 * pelota que rebota y ecualizador (COCO). Los colores son una paleta de diseño, no logotipos oficiales.
 */
class StationCardView(ctx: Context, val station: Station) : View(ctx), Animated {

    var current = false

    private val d = resources.displayMetrics.density
    private val rect = RectF()
    private val rect2 = RectF()
    private val clip = Path()
    private val path = Path()
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val txt = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val energy = Spring(0.3f, 36f, 1f)
    private val radius = 24f * d

    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        if (w <= 0 || h <= 0) return
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        clip.reset()
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        bg.shader = LinearGradient(0f, 0f, w * 0.3f, h.toFloat(), station.top, station.bottom, Shader.TileMode.CLAMP)
        scrim.shader = LinearGradient(
            0f, h * 0.48f, 0f, h.toFloat(),
            withAlpha(station.bottom, 0f), withAlpha(station.bottom, 0.94f), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = Clock.t
        val status = RadioPlayback.info.status
        val playing = current && status == PlaybackStatus.PLAYING
        energy.target = if (playing) 1f else 0.3f
        energy.step(1f / 30f)
        val e = energy.value

        c.save()
        c.clipPath(clip)
        c.drawRect(0f, 0f, w, h, bg)
        p.style = Paint.Style.FILL
        line.style = Paint.Style.STROKE
        when (station.id) {
            "enciclopedia" -> artEnciclopedia(c, w, h, t, e)
            "progreso" -> artProgreso(c, w, h, t, e)
            "rebelde", "rebelde-fm" -> artRebelde(c, w, h, t, e)
            "coco" -> artCoco(c, w, h, t, e)
            "reloj" -> artReloj(c, w, h, t, e)
            else -> artGenerico(c, w, h, t, e)
        }
        c.drawRect(0f, h * 0.48f, w, h, scrim)
        c.restore()

        line.style = Paint.Style.STROKE
        line.strokeWidth = (if (current) 2.2f else 1f) * d
        line.color = if (current) station.accent else withAlpha(Pal.WHITE, 0.28f)
        rect2.set(rect)
        rect2.inset(line.strokeWidth / 2f, line.strokeWidth / 2f)
        c.drawRoundRect(rect2, radius, radius, line)

        // Textos
        val left = 16f * d
        val maxW = w - 2f * left
        txt.typeface = Fonts.script
        txt.color = Pal.WHITE
        fit(station.name, maxW, 22f, 14f)
        c.drawText(station.name, left, h - 52f * d, txt)
        txt.typeface = Fonts.bold
        txt.color = station.accent2
        fit(station.dial, maxW, 12f, 9f)
        c.drawText(station.dial, left, h - 33f * d, txt)
        txt.typeface = Fonts.regular
        txt.color = withAlpha(Pal.WHITE, 0.88f)
        txt.textSize = sp(11f)
        c.drawText(TextUtils.ellipsize(station.tagline, txt, maxW, TextUtils.TruncateAt.END).toString(), left, h - 15f * d, txt)

        // Etiqueta de estado
        if (current) {
            val label = when (status) {
                PlaybackStatus.PLAYING -> "EN VIVO"
                PlaybackStatus.CONNECTING -> "CONECTANDO"
                else -> "ELEGIDA"
            }
            txt.typeface = Fonts.bold
            txt.textSize = sp(10f)
            txt.letterSpacing = 0.1f
            val tw = txt.measureText(label)
            val x = 14f * d
            val y = 14f * d
            p.color = withAlpha(0xFF000000.toInt(), 0.40f)
            c.drawRoundRect(x, y, x + tw + 28f * d, y + 22f * d, 11f * d, 11f * d, p)
            val dot = when (status) {
                PlaybackStatus.PLAYING -> Pal.GREEN
                PlaybackStatus.CONNECTING -> Pal.GOLD
                else -> station.accent
            }
            val pulse = if (status == PlaybackStatus.PLAYING) 0.6f + 0.4f * sin(t * 5f) else 1f
            p.color = withAlpha(dot, pulse)
            c.drawCircle(x + 11f * d, y + 11f * d, 3.5f * d, p)
            txt.color = Pal.WHITE
            c.drawText(label, x + 20f * d, y + 15f * d, txt)
            txt.letterSpacing = 0f
        }
    }

    private fun fit(text: String, maxWidth: Float, startSp: Float, minSp: Float) {
        var size = sp(startSp)
        val lo = sp(minSp)
        txt.textSize = size
        while (txt.measureText(text) > maxWidth && size > lo) {
            size -= 1f
            txt.textSize = size
        }
    }

    // ---------------------------------------------------------------- motivos

    /** Disco de vinilo que gira, arcos de sonido y notas que suben. */
    private fun artEnciclopedia(c: Canvas, w: Float, h: Float, t: Float, e: Float) {
        val cx = w * 0.70f
        val cy = h * 0.34f
        val r = min(w, h) * 0.22f
        line.strokeWidth = 2f * d
        for (k in 0..2) {
            val rr = r * (1.35f + 0.28f * k) + 3f * d * sin(t * (1.6f + e) - k)
            line.color = withAlpha(station.accent2, 0.38f - 0.09f * k)
            rect2.set(cx - rr, cy - rr, cx + rr, cy + rr)
            c.drawArc(rect2, -50f, 100f, false, line)
        }
        p.color = withAlpha(Pal.DEEP, 0.55f)
        c.drawCircle(cx, cy, r, p)
        c.save()
        c.rotate(t * (18f + 70f * e), cx, cy)
        line.strokeWidth = 1f * d
        for (k in 1..3) {
            line.color = withAlpha(Pal.WHITE, 0.14f)
            c.drawCircle(cx, cy, r * (0.45f + 0.17f * k), line)
        }
        line.strokeWidth = 3f * d
        line.color = withAlpha(Pal.WHITE, 0.30f)
        rect2.set(cx - r * 0.86f, cy - r * 0.86f, cx + r * 0.86f, cy + r * 0.86f)
        c.drawArc(rect2, -35f, 38f, false, line)
        p.color = station.accent2
        c.drawCircle(cx, cy, r * 0.3f, p)
        p.color = Pal.DEEP
        c.drawCircle(cx, cy, r * 0.06f, p)
        c.restore()
        for (k in 0..2) {
            val q = (t * 0.16f + k / 3f) % 1f
            p.color = withAlpha(Pal.ROSE, 0.6f * sin(q * 3.1416f))
            drawMusicNote(c, p, w * (0.18f + 0.12f * k) + sin(t + k * 2f) * 6f * d, h * (0.5f - q * 0.38f), (7f + 2f * k) * d, k % 2 == 0)
        }
    }

    /** Sol de «La Onda de la Alegría»: rayos, olas y confeti. */
    private fun artProgreso(c: Canvas, w: Float, h: Float, t: Float, e: Float) {
        val cx = w * 0.70f
        val cy = h * 0.34f
        val r = min(w, h) * 0.15f
        for (k in 3 downTo 1) {
            p.color = withAlpha(station.accent, 0.10f)
            c.drawCircle(cx, cy, r * (1f + 0.45f * k), p)
        }
        c.save()
        c.rotate(t * (10f + 22f * e), cx, cy)
        line.strokeWidth = 3f * d
        line.color = withAlpha(station.accent, 0.85f)
        for (k in 0 until 12) {
            val len = r * (0.55f + 0.25f * sin(t * 2f + k))
            c.save()
            c.rotate(k * 30f, cx, cy)
            c.drawLine(cx, cy - r * 1.25f, cx, cy - r * 1.25f - len, line)
            c.restore()
        }
        c.restore()
        p.color = station.accent
        c.drawCircle(cx, cy, r, p)
        p.color = withAlpha(Pal.WHITE, 0.35f)
        c.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, p)
        for (k in 0..2) {
            path.reset()
            val base = h * 0.50f + k * 9f * d
            path.moveTo(0f, base)
            var x = 0f
            while (x <= w) {
                path.lineTo(x, base + sin(x / w * 12.566f + t * (1.4f + 0.5f * k)) * (4f + 5f * e) * d)
                x += w / 24f
            }
            line.strokeWidth = 2.2f * d
            line.color = withAlpha(if (k == 1) station.accent else station.accent2, 0.55f - 0.12f * k)
            c.drawPath(path, line)
        }
        for (k in 0 until 9) {
            val q = (t * 0.09f + k * 0.113f) % 1f
            val x = w * ((k * 0.37f) % 0.9f + 0.05f) + sin(t * 1.1f + k) * 8f * d
            val y = h * 0.55f - q * h * 0.5f
            p.color = withAlpha(if (k % 3 == 0) Pal.WHITE else if (k % 3 == 1) station.accent2 else station.accent, 0.8f * sin(q * 3.1416f))
            c.save()
            c.rotate(t * 80f + k * 40f, x, y)
            c.drawRect(x - 3f * d, y - 2f * d, x + 3f * d, y + 2f * d, p)
            c.restore()
        }
    }

    /** Franjas rojinegras, estrella que irradia ondas y una línea de morse que avanza. */
    private fun artRebelde(c: Canvas, w: Float, h: Float, t: Float, e: Float) {
        c.save()
        c.rotate(-24f, w * 0.6f, h * 0.3f)
        p.color = withAlpha(0xFF000000.toInt(), 0.28f)
        for (k in 0..3) {
            val x = w * 0.15f + k * w * 0.2f
            c.drawRect(x, -h * 0.3f, x + w * 0.09f, h * 1.2f, p)
        }
        c.restore()
        val cx = w * 0.72f
        val cy = h * 0.33f
        val rr = min(w, h) * 0.17f
        line.strokeWidth = 2f * d
        for (k in 0..2) {
            val q = (t * (0.35f + 0.3f * e) + k / 3f) % 1f
            line.color = withAlpha(Pal.WHITE, (1f - q) * 0.5f)
            c.drawCircle(cx, cy, rr * (1.1f + q * 2.3f), line)
        }
        val s = 1f + 0.04f * sin(t * 3f) * (0.5f + e)
        path.reset()
        for (k in 0 until 10) {
            val rad = if (k % 2 == 0) rr * s else rr * 0.42f * s
            val a = -PI / 2 + k * PI / 5
            val x = cx + (cos(a) * rad).toFloat()
            val y = cy + (sin(a) * rad).toFloat()
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        p.color = withAlpha(Pal.WHITE, 0.94f)
        c.drawPath(path, p)
        val unit = 5f * d
        val sym = intArrayOf(1, 1, 3, 1, 3, 3, 1, 1, 1, 3)
        var period = 0f
        for (s2 in sym) period += (s2 + 1) * unit
        var x = -((t * (18f + 30f * e) * d) % period)
        val y = h * 0.50f
        line.strokeWidth = 2.6f * d
        line.color = withAlpha(station.accent2, 0.85f)
        var guard = 0
        while (x < w && guard < 200) {
            for (s2 in sym) {
                val len = s2 * unit
                if (x + len > 0f && x < w) c.drawLine(x, y, x + len, y, line)
                x += (s2 + 1) * unit
            }
            guard++
        }
    }

    /** Pelota que rebota con squash y ecualizador: deporte, juventud y música. */
    private fun artCoco(c: Canvas, w: Float, h: Float, t: Float, e: Float) {
        val gy = h * 0.52f
        val r = min(w, h) * 0.085f
        val cx = w * 0.72f
        val q = abs(sin(t * (1.9f + 0.9f * e)))
        val by = gy - r - q * h * 0.26f
        line.strokeWidth = 1.2f * d
        line.color = withAlpha(Pal.WHITE, 0.25f)
        c.drawLine(0f, gy, w, gy, line)
        val sw = r * (1.5f - 0.7f * q)
        p.color = withAlpha(0xFF000000.toInt(), 0.28f)
        c.drawOval(cx - sw, gy - r * 0.18f, cx + sw, gy + r * 0.18f, p)
        val sy = if (q < 0.1f) 0.8f + q * 2f else 1f
        val sx = 2f - sy
        c.save()
        c.scale(sx, sy, cx, by + r)
        p.color = station.accent
        c.drawCircle(cx, by, r, p)
        line.strokeWidth = 1.6f * d
        line.color = withAlpha(Pal.DEEP, 0.55f)
        c.save()
        c.rotate(t * 140f, cx, by)
        c.drawLine(cx - r, by, cx + r, by, line)
        c.drawLine(cx, by - r, cx, by + r, line)
        c.drawCircle(cx, by, r * 0.5f, line)
        c.restore()
        c.restore()
        val n = 9
        val bw = w * 0.045f
        val x0 = w * 0.07f
        p.color = withAlpha(station.accent2, 0.75f)
        for (i in 0 until n) {
            val hh = (0.25f + 0.75f * abs(sin(t * (1.7f + 0.37f * i) * (0.6f + 0.8f * e) + i))) * h * 0.26f
            val x = x0 + i * bw * 1.5f
            c.drawRoundRect(x, gy - hh, x + bw, gy, bw / 2f, bw / 2f, p)
        }
        line.strokeWidth = 2f * d
        line.color = withAlpha(station.accent, 0.30f + 0.3f * sin(t * 2f))
        c.drawCircle(cx, gy - h * 0.14f, h * 0.30f + 3f * d * sin(t * 2f), line)
    }

    /** Esfera de reloj con la hora de Cuba y un pulso por segundo: la hora exacta de Radio Reloj. */
    private fun artReloj(c: Canvas, w: Float, h: Float, t: Float, e: Float) {
        val cx = w * 0.72f
        val cy = h * 0.34f
        val r = min(w, h) * 0.2f
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Havana"))
        val ms = cal.get(java.util.Calendar.MILLISECOND) / 1000f
        val sec = cal.get(java.util.Calendar.SECOND) + ms
        val minutes = cal.get(java.util.Calendar.MINUTE) + sec / 60f
        val hour = (cal.get(java.util.Calendar.HOUR) % 12) + minutes / 60f
        val pulse = (ms * (1f + e)).coerceIn(0f, 1f)
        line.strokeWidth = 2f * d
        line.color = withAlpha(station.accent, (1f - pulse) * 0.5f)
        c.drawCircle(cx, cy, r * (1.05f + pulse * 0.9f), line)
        p.color = withAlpha(Pal.DEEP, 0.5f)
        c.drawCircle(cx, cy, r, p)
        line.strokeWidth = 2f * d
        line.color = withAlpha(Pal.WHITE, 0.55f)
        c.drawCircle(cx, cy, r, line)
        for (k in 0 until 12) {
            val a = k * PI / 6
            val inner = if (k % 3 == 0) 0.74f else 0.86f
            line.strokeWidth = (if (k % 3 == 0) 2.4f else 1.2f) * d
            c.drawLine(cx + (sin(a) * r * inner).toFloat(), cy - (cos(a) * r * inner).toFloat(),
                cx + (sin(a) * r * 0.95f).toFloat(), cy - (cos(a) * r * 0.95f).toFloat(), line)
        }
        fun hand(turns: Float, len: Float, width: Float, color: Int) {
            val a = turns * 2 * PI
            line.strokeWidth = width * d
            line.color = color
            c.drawLine(cx, cy, cx + (sin(a) * r * len).toFloat(), cy - (cos(a) * r * len).toFloat(), line)
        }
        hand(hour / 12f, 0.5f, 3.4f, Pal.WHITE)
        hand(minutes / 60f, 0.75f, 2.4f, Pal.WHITE)
        hand(sec / 60f, 0.88f, 1.4f, station.accent)
        p.color = station.accent
        c.drawCircle(cx, cy, 3.2f * d, p)
        // Barritas de noticias que suben y bajan.
        p.color = withAlpha(station.accent2, 0.7f)
        val bw = w * 0.04f
        for (i in 0 until 8) {
            val hh = (0.2f + 0.8f * abs(sin(t * (1.3f + 0.31f * i) * (0.6f + 0.8f * e) + i))) * h * 0.2f
            val x = w * 0.07f + i * bw * 1.6f
            c.drawRoundRect(x, h * 0.5f - hh, x + bw, h * 0.5f, bw / 2f, bw / 2f, p)
        }
    }

    /** Motivo común de las demás emisoras: ondas que salen de un disco con sus iniciales. */
    private fun artGenerico(c: Canvas, w: Float, h: Float, t: Float, e: Float) {
        val cx = w * 0.72f
        val cy = h * 0.33f
        val r = min(w, h) * 0.16f
        line.strokeWidth = 2f * d
        for (k in 0..2) {
            val q = (t * (0.28f + 0.3f * e) + k / 3f) % 1f
            line.color = withAlpha(station.accent, (1f - q) * 0.5f)
            c.drawCircle(cx, cy, r * (1.1f + q * 2.2f), line)
        }
        p.color = withAlpha(station.accent, 0.20f)
        c.drawCircle(cx, cy, r * 1.25f, p)
        p.color = withAlpha(Pal.DEEP, 0.55f)
        c.drawCircle(cx, cy, r, p)
        line.strokeWidth = 2.4f * d
        line.color = withAlpha(station.accent, 0.9f)
        rect2.set(cx - r, cy - r, cx + r, cy + r)
        c.drawArc(rect2, t * (40f + 90f * e), 120f, false, line)
        txt.typeface = Fonts.script
        txt.color = Pal.WHITE
        txt.textAlign = Paint.Align.CENTER
        txt.textSize = r * 0.95f
        c.drawText(station.mark, cx, cy + r * 0.32f, txt)
        txt.textAlign = Paint.Align.LEFT
        // Ola y ecualizador suaves.
        for (k in 0..1) {
            path.reset()
            val base = h * 0.50f + k * 8f * d
            path.moveTo(0f, base)
            var x = 0f
            while (x <= w) {
                path.lineTo(x, base + sin(x / w * 9.4f + t * (1.2f + 0.6f * k)) * (3f + 5f * e) * d)
                x += w / 22f
            }
            line.strokeWidth = 2f * d
            line.color = withAlpha(if (k == 0) station.accent else station.accent2, 0.5f - 0.15f * k)
            c.drawPath(path, line)
        }
        p.color = withAlpha(station.accent2, 0.6f)
        val bw = w * 0.035f
        for (i in 0 until 9) {
            val hh = (0.25f + 0.75f * abs(sin(t * (1.1f + 0.29f * i) * (0.6f + 0.9f * e) + i * 1.7f))) * h * 0.17f
            val x = w * 0.07f + i * bw * 1.55f
            c.drawRoundRect(x, h * 0.45f - hh, x + bw, h * 0.45f, bw / 2f, bw / 2f, p)
        }
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

/**
 * Carrusel «coverflow» de emisoras con física: arrastra, suelta con inercia y la tarjeta
 * más cercana se centra con un resorte. Al soltar, la emisora elegida se sintoniza.
 */
class StationDeckView(private val act: MainActivity) : FrameLayout(act) {

    private val cards = ArrayList<StationCardView>()
    private val n = Stations.all.size
    private val pos = SpringAnim(Spring(Stations.indexOf(Stations.current).toFloat(), 200f, 0.8f, 0.001f)) { layoutCards() }
    private var cardW = 0
    private val slop = ViewConfiguration.get(act).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var startPos = 0f
    private var dragging = false
    private var tracker: VelocityTracker? = null
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val listener: (Station) -> Unit = { s -> onStationChanged(s) }

    init {
        clipChildren = true
        clipToPadding = true
        for ((i, s) in Stations.all.withIndex()) {
            val card = StationCardView(act, s)
            card.current = s === Stations.current
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { onCardClick(i) }
            cards.add(card)
            addView(card, LayoutParams(act.dpi(220), act.dpi(166), android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP))
        }
        describe()
        layoutCards()
    }

    private fun onCardClick(i: Int) {
        val s = Stations.all[i]
        if (s === Stations.current) {
            act.toggleRadio(cards[i])
        } else {
            pos.animateTo(i.toFloat())
            act.chooseStation(s)
        }
    }

    private fun onStationChanged(s: Station) {
        for (card in cards) card.current = card.station === s
        pos.animateTo(Stations.indexOf(s).toFloat())
        describe()
    }

    // TalkBack no puede arrastrar: «desplazar adelante/atrás» (deslizar con dos dedos) cambia de emisora.
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
    }

    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> { act.chooseStation(Stations.next()); return true }
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> { act.chooseStation(Stations.previous()); return true }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    private fun describe() {
        for (card in cards) {
            val s = card.station
            card.contentDescription = if (s === Stations.current)
                "${s.spoken}. Emisora actual. Toca para reproducir o pausar"
            else "Cambiar a ${s.spoken}"
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        cardW = (w * 0.64f).toInt().coerceIn(act.dpi(180), act.dpi(300))
        val ch = act.dpi(166)
        for (card in cards) {
            val lp = card.layoutParams
            lp.width = cardW
            lp.height = ch
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        // El ancho de las tarjetas se conoce al medir: se colocan en abanico en cuanto hay tamaño.
        layoutCards()
    }

    private fun layoutCards() {
        val step = cardW * 0.58f
        val cam = 9000f * act.resources.displayMetrics.density
        for ((i, v) in cards.withIndex()) {
            val dd = i - pos.value
            val a = abs(dd)
            v.cameraDistance = cam
            v.translationX = dd * step
            val s = 1f - 0.16f * min(a, 2.2f)
            v.scaleX = s
            v.scaleY = s
            v.rotationY = (-dd * 24f).coerceIn(-48f, 48f)
            v.alpha = (1f - 0.35f * min(a, 2f)).coerceAtLeast(0.2f)
            v.translationZ = act.dp(12f) - a * act.dp(4f)
        }
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // Pista de posición: un punto por emisora; el activo crece y toma el color de su emisora.
        val cy = height - act.dp(9f)
        val trackW = minOf(width - act.dp(48f), act.dp(15f) * (n - 1))
        val x0 = (width - trackW) / 2f
        val gap = if (n > 1) trackW / (n - 1) else 0f
        for ((i, s) in Stations.all.withIndex()) {
            val near = (1f - abs(i - pos.value)).coerceIn(0f, 1f)
            dot.color = lerpColor(withAlpha(Pal.LILAC, 0.45f), s.accent, near)
            canvas.drawCircle(x0 + i * gap, cy, act.dp(1.8f) + act.dp(2.8f) * near, dot)
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                dragging = false
                tracker?.recycle()
                tracker = VelocityTracker.obtain()
                tracker?.addMovement(e)
            }
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(e)
                if (!dragging && abs(e.x - downX) > slop && abs(e.x - downX) > abs(e.y - downY)) {
                    beginDrag()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finish(e)
        }
        return dragging
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(e)
                if (!dragging && abs(e.x - downX) > slop && abs(e.x - downX) > abs(e.y - downY)) beginDrag()
                if (dragging) {
                    val step = cardW * 0.58f
                    pos.jump((startPos - (e.x - downX) / step).coerceIn(-0.4f, n - 0.6f))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finish(e)
        }
        return true
    }

    private fun beginDrag() {
        dragging = true
        startPos = pos.value
        pos.cancel()
        parent?.requestDisallowInterceptTouchEvent(true)
        Haptics.gestureStart(this)
    }

    private fun finish(e: MotionEvent) {
        if (dragging) {
            val step = cardW * 0.58f
            tracker?.computeCurrentVelocity(1000)
            val vx = tracker?.xVelocity ?: 0f
            val target = (pos.value - vx / step * 0.18f).roundToInt().coerceIn(0, n - 1)
            pos.animateTo(target.toFloat(), -vx / step)
            parent?.requestDisallowInterceptTouchEvent(false)
            act.chooseStation(Stations.all[target])
        }
        dragging = false
        tracker?.recycle()
        tracker = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Stations.listen(listener)
        for (card in cards) card.current = card.station === Stations.current
        pos.jump(Stations.indexOf(Stations.current).toFloat())
        describe()
    }

    override fun onDetachedFromWindow() {
        Stations.unlisten(listener)
        pos.cancel()
        super.onDetachedFromWindow()
    }
}

/**
 * Capa de color y ondas: al cambiar de emisora, el fondo se tiñe con su color y unas ondas de
 * radio se expanden desde la parte alta. Con Enciclopedia el fondo queda como siempre.
 */
class AccentWashView(ctx: Context) : View(ctx), Animated {
    private var from = Stations.current.accent
    private var to = from
    private val mix = SpringAnim(Spring(1f, 55f, 1f)) { invalidate() }
    private val wash = SpringAnim(Spring(if (Stations.current === Stations.ENCICLOPEDIA) 0f else 1f, 55f, 1f)) { invalidate() }
    private var rippleStart = -10f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val listener: (Station) -> Unit = { s -> onChanged(s) }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun onChanged(s: Station) {
        from = lerpColor(from, to, mix.value)
        to = s.accent
        mix.jump(0f)
        mix.animateTo(1f)
        wash.animateTo(if (s === Stations.ENCICLOPEDIA) 0f else 1f)
        if (Motion.enabled()) rippleStart = Clock.t
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val col = lerpColor(from, to, mix.value)
        val a = (wash.value * 0.30f).coerceIn(0f, 1f)
        if (a > 0.003f) {
            paint.shader = RadialGradient(
                w * 0.5f, h * 0.22f, maxOf(w, h) * 0.95f,
                intArrayOf(withAlpha(col, a), withAlpha(col, a * 0.35f), withAlpha(col, 0f)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
            c.drawRect(0f, 0f, w, h, paint)
        }
        val el = Clock.t - rippleStart
        if (el in 0f..2.2f) {
            val d = resources.displayMetrics.density
            for (k in 0..2) {
                val q = ((el - k * 0.2f) / 1.5f).coerceIn(0f, 1f)
                if (q <= 0f || q >= 1f) continue
                val ease = 1f - (1f - q) * (1f - q)
                ring.strokeWidth = 3f * d * (1f - q * 0.6f)
                ring.color = withAlpha(to, (1f - q) * 0.55f)
                c.drawCircle(w * 0.5f, h * 0.24f, ease * maxOf(w, h) * 0.8f, ring)
            }
        }
    }

    override fun tick(t: Float) {
        if (t - rippleStart in 0f..2.2f) invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Stations.listen(listener)
        Clock.register(this)
    }

    override fun onDetachedFromWindow() {
        Stations.unlisten(listener)
        Clock.unregister(this)
        super.onDetachedFromWindow()
    }
}
