package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sintonizador de radio FM. Al abrir «Escuchar», la aguja barre la banda y se engancha en
 * 94,1 MHz con un resorte y un chasquido háptico. También se puede arrastrar: fuera de la
 * emisora aparece «estática» y, al soltar, la aguja vuelve sola a la frecuencia de la emisora.
 */
class TuningScaleView(ctx: Context) : View(ctx), Animated {

    private val minF = 88f
    private val maxF = 108f
    /** Última frecuencia FM de La Habana sintonizada: la aguja se queda ahí si la emisora no está en el dial. */
    private var parked = Stations.current.fm ?: 94.1f
    private val station: Float get() = Stations.current.fm ?: parked
    private val onDial: Boolean get() = Stations.current.fm != null
    var onTune: ((Station) -> Unit)? = null

    private val needle = SpringAnim(Spring(station, 70f, 0.40f, 0.002f)) { f ->
        val bucket = (f * 2f).toInt()
        val now = System.nanoTime()
        if (bucket != lastBucket && now - lastTick > 45_000_000L) {
            Haptics.tick(this)
            lastTick = now
        }
        lastBucket = bucket
        invalidate()
    }
    private var lastBucket = (station * 2f).toInt()
    private var lastTick = 0L
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop

    private val d = resources.displayMetrics.density
    private val panel = RectF()
    private val panelFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = d
        color = withAlpha(Pal.PINK, 0.45f)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { letterSpacing = 0.12f }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val tri = Path()

    private val changed: (Station) -> Unit = { s ->
        s.fm?.let { parked = it }
        if (!dragging) needle.animateTo(station)
        describe()
    }

    init {
        describe()
        needle.onEnd = { if (!dragging) Haptics.lock(this) }
    }

    private fun describe() {
        val s = Stations.current
        contentDescription = "Sintonizador. ${s.spoken}. Desliza hacia adelante o atrás para cambiar de emisora"
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
    }

    // Con TalkBack: deslizar hacia adelante o atrás con dos dedos sintoniza la emisora siguiente o anterior.
    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> { onTune?.invoke(Stations.next()); return true }
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> { onTune?.invoke(Stations.previous()); return true }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    /** Barre la banda desde el extremo alto hasta la emisora. */
    fun tuneIn() {
        dragging = false
        needle.jump(105.2f)
        lastBucket = (105.2f * 2f).toInt()
        needle.animateTo(station)
    }

    private fun padL() = 18f * d
    private fun padR() = 18f * d
    private fun xAt(f: Float) = padL() + (f - minF) / (maxF - minF) * (width - padL() - padR())
    private fun freqAt(x: Float) = minF + (x - padL()) / (width - padL() - padR()) * (maxF - minF)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        panel.set(d, d, w - d, h - d)
        panelFill.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            withAlpha(Pal.MID, 0.78f), withAlpha(Pal.DEEP, 0.88f), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = Clock.t
        val playing = RadioPlayback.info.status == PlaybackStatus.PLAYING

        c.drawRoundRect(panel, 18f * d, 18f * d, panelFill)
        c.drawRoundRect(panel, 18f * d, 18f * d, panelBorder)

        // Un temblor leve de la aguja cuando está enganchada y la emisora suena.
        val closest = Stations.onDial.minByOrNull { abs((it.fm ?: 0f) - needle.value) } ?: Stations.current
        val closestFm = closest.fm ?: needle.value
        val settled = onDial && abs(needle.value - closestFm) < 0.05f && !dragging
        val jitter = if (playing && settled) 0.035f * sin(t * 5f) else 0f
        val f = needle.value + jitter
        // Si la emisora elegida no tiene frecuencia en el dial FM de La Habana, la aguja se atenúa.
        val off = if (onDial || dragging) abs(f - closestFm) else 3f

        // Rótulos superiores.
        val cur = Stations.current
        small.color = if (!onDial) cur.accent else if (off < 0.5f) closest.accent else Pal.GOLD
        small.textSize = sp(10f)
        small.typeface = Fonts.bold
        c.drawText(if (!onDial && !dragging) cur.short else if (off < 0.5f) closest.short else "SINTONIZADOR FM", 18f * d, 26f * d, small)
        big.color = if (!onDial && !dragging) cur.accent else if (off < 0.12f) Pal.GOLD else Pal.WHITE
        big.textSize = sp(22f)
        big.typeface = Fonts.script
        c.drawText(if (!onDial && !dragging) cur.band else "%.1f".format(f).replace('.', ',') + " MHz", w - 18f * d, 30f * d, big)

        // Escala graduada con efecto «imán» alrededor de la aguja.
        val baseY = h - 16f * d
        text.textSize = sp(10f)
        text.typeface = Fonts.regular
        var i = 0
        while (i <= 40) {
            val fi = minF + i * 0.5f
            val x = xAt(fi)
            val near = exp(-((fi - f) * (fi - f)) * 0.55f)
            val major = i % 4 == 0
            val medium = i % 2 == 0
            val len = (if (major) 15f else if (medium) 10f else 6f) * d + 7f * d * near
            tickPaint.strokeWidth = (if (major) 1.8f else 1.2f) * d
            tickPaint.color = lerpColor(withAlpha(Pal.LILAC, if (major) 0.8f else 0.5f), Pal.GOLD, near)
            c.drawLine(x, baseY, x, baseY - len, tickPaint)
            if (major) {
                text.color = lerpColor(withAlpha(Pal.LILAC, 0.8f), Pal.GOLD, near)
                c.drawText(fi.toInt().toString(), x, baseY + 12f * d, text)
            }
            i++
        }

        // Marcas de las emisoras con frecuencia FM en La Habana; la más cercana a la aguja late al engancharse.
        val lock = if (off < 0.15f) 1f - off / 0.15f else 0f
        line.style = Paint.Style.FILL
        for (s in Stations.onDial) {
            val sx = xAt(s.fm ?: continue)
            val k = if (s === closest) lock else 0f
            val isCur = s === Stations.current
            line.color = withAlpha(s.accent, 0.25f + 0.5f * k + 0.2f * k * sin(t * 4f))
            c.drawCircle(sx, baseY - 30f * d, ((if (isCur) 5f else 3.5f) + 5f * k) * d, line)
            line.color = if (isCur) Pal.GOLD else s.accent
            c.drawCircle(sx, baseY - 30f * d, (if (isCur) 3f else 2.2f) * d, line)
        }

        // Estática: barritas aleatorias que desaparecen al engancharse.
        val noise = if (!onDial && !dragging) 0f else (off / 3f).coerceIn(0f, 1f)
        if (noise > 0.02f) {
            tickPaint.strokeWidth = 1.4f * d
            var k = 0
            while (k < 26) {
                val seed = sin(k * 12.9898f + (t * 24f).toInt() * 4.1414f) * 43758.547f
                val r = seed - kotlin.math.floor(seed)
                val x = padL() + (k + 0.5f) / 26f * (w - padL() - padR())
                tickPaint.color = withAlpha(Pal.LILAC, 0.10f + 0.28f * noise * r)
                c.drawLine(x, baseY - 40f * d, x, baseY - 40f * d - r * 10f * d * noise, tickPaint)
                k++
            }
        }

        // Aguja dorada con resplandor.
        val nx = xAt(f.coerceIn(minF, maxF))
        line.style = Paint.Style.STROKE
        val needleA = if (onDial || dragging) 1f else 0.28f
        line.strokeWidth = 7f * d
        line.color = withAlpha(Pal.GOLD, 0.20f * needleA)
        c.drawLine(nx, 40f * d, nx, baseY + 2f * d, line)
        line.strokeWidth = 2.2f * d
        line.color = withAlpha(Pal.GOLD_LIGHT, needleA)
        c.drawLine(nx, 40f * d, nx, baseY + 2f * d, line)
        line.style = Paint.Style.FILL
        line.color = withAlpha(Pal.GOLD_LIGHT, needleA)
        tri.reset()
        tri.moveTo(nx - 6f * d, 36f * d)
        tri.lineTo(nx + 6f * d, 36f * d)
        tri.lineTo(nx, 46f * d)
        tri.close()
        c.drawPath(tri, line)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) {
                    val dx = abs(e.x - downX)
                    val dy = abs(e.y - downY)
                    if (dx > slop && dx > dy) {
                        dragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        Haptics.gestureStart(this)
                        needle.cancel()
                    }
                }
                if (dragging) needle.jump(freqAt(e.x).coerceIn(minF, maxF))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    // Al soltar cerca de una emisora, se sintoniza; si no, la aguja vuelve a la actual.
                    val snap = Stations.onDial.minByOrNull { abs((it.fm ?: 0f) - needle.value) }
                    val snapFm = snap?.fm
                    if (snap != null && snapFm != null && abs(snapFm - needle.value) <= 1.0f) {
                        needle.animateTo(snapFm)
                        if (snap !== Stations.current) onTune?.invoke(snap)
                    } else {
                        needle.animateTo(station)
                    }
                }
            }
        }
        return true
    }

    override fun tick(t: Float) {
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Stations.listen(changed)
        if (!dragging) needle.jump(station)
        describe()
        Clock.register(this)
    }

    override fun onDetachedFromWindow() {
        Stations.unlisten(changed)
        needle.cancel()
        Clock.unregister(this)
        super.onDetachedFromWindow()
    }
}
