package cu.radioenciclopedia.app

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fondo vivo para Android 13+ (AGSL): ondas de sonido que fluyen sobre el degradado malva,
 * un disco de vinilo difuso que gira y notas que suben. La energía de las ondas sigue al
 * estado de la radio: tranquilas en reposo, amplias y rápidas al sonar.
 *
 * Si el shader no compila o falla al dibujar, la vista pinta el degradado base y
 * [ok] queda en falso para que la app use AuroraView en su lugar.
 */
@TargetApi(33)
class LiveBackdropView(ctx: Context) : View(ctx), Animated {

    val ok: Boolean
    private var shader: RuntimeShader? = null
    private var failed = false
    private val paint = Paint()
    private val basePaint = Paint()
    private val energy = Spring(0.12f, 36f, 1.0f)
    private var accFrom = Stations.current.accent
    private var accTo = accFrom
    private val accMix = Spring(1f, 40f, 1.0f)
    private val stationListener: (Station) -> Unit = { s ->
        accFrom = lerpColor(accFrom, accTo, accMix.value)
        accTo = s.accent
        accMix.value = 0f
        accMix.target = 1f
    }

    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val rnd = Random(11)
    private val noteCount = 7
    private val nx = FloatArray(noteCount) { 0.06f + rnd.nextFloat() * 0.88f }
    private val nSpeed = FloatArray(noteCount) { 0.030f + rnd.nextFloat() * 0.030f }
    private val nOff = FloatArray(noteCount) { rnd.nextFloat() }
    private val nSize = FloatArray(noteCount) { 9f + rnd.nextFloat() * 8f }
    private val nSway = FloatArray(noteCount) { rnd.nextFloat() * 6.283f }
    private val starCount = 14
    private val sx = FloatArray(starCount) { rnd.nextFloat() }
    private val sy = FloatArray(starCount) { rnd.nextFloat() }
    private val sz = FloatArray(starCount) { 0.5f + rnd.nextFloat() * 1.1f }
    private val sp = FloatArray(starCount) { 0.8f + rnd.nextFloat() * 2.2f }
    private val ph = FloatArray(starCount) { rnd.nextFloat() * 6.283f }

    init {
        var s: RuntimeShader? = null
        try {
            s = RuntimeShader(SRC)
        } catch (e: Throwable) {
            s = null
        }
        shader = s
        ok = s != null
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        if (w <= 0 || h <= 0) return
        basePaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Pal.MID, 0xFF4C2D57.toInt(), 0xFF643A70.toInt(), 0xFF72436C.toInt()),
            floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = Clock.t

        val goal = when (RadioPlayback.info.status) {
            PlaybackStatus.PLAYING -> 1f
            PlaybackStatus.CONNECTING -> 0.55f
            else -> 0.12f
        }
        energy.target = goal
        energy.step(1f / 30f)
        accMix.step(1f / 30f)
        val acc = lerpColor(accFrom, accTo, accMix.value.coerceIn(0f, 1f))

        val s = shader
        var drawn = false
        if (s != null && !failed) {
            try {
                s.setFloatUniform("iResolution", w, h)
                s.setFloatUniform("iTime", t)
                s.setFloatUniform("iEnergy", energy.value.coerceIn(0f, 1f))
                s.setFloatUniform("iAccent", Color.red(acc) / 255f, Color.green(acc) / 255f, Color.blue(acc) / 255f)
                paint.shader = s
                c.drawRect(0f, 0f, w, h, paint)
                drawn = true
            } catch (e: Throwable) {
                failed = true
            }
        }
        if (!drawn) c.drawRect(0f, 0f, w, h, basePaint)

        val d = resources.displayMetrics.density
        for (i in 0 until starCount) {
            val a = 0.2f + 0.8f * (0.5f + 0.5f * sin(t * sp[i] + ph[i]))
            starPaint.alpha = (a * 85f).toInt()
            c.drawCircle(sx[i] * w, sy[i] * h * 0.8f, sz[i] * d, starPaint)
        }
        val lift = 1f + 1.5f * energy.value
        for (i in 0 until noteCount) {
            val k = (t * nSpeed[i] * lift + nOff[i]) % 1f
            val fade = sin(k * 3.1416f)
            val x = nx[i] * w + sin(t * 0.7f + nSway[i]) * 14f * d
            val y = h * (1.02f - k * 1.1f)
            notePaint.color = withAlpha(Pal.ROSE, 0.22f * fade)
            drawMusicNote(c, notePaint, x, y, nSize[i] * d, i % 2 == 0)
        }
    }

    override fun tick(t: Float) {
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Stations.listen(stationListener)
        Clock.register(this)
    }

    override fun onDetachedFromWindow() {
        Stations.unlisten(stationListener)
        Clock.unregister(this)
        super.onDetachedFromWindow()
    }

    private companion object {
        const val SRC = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iAccent;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

// Resplandor suave (igual que los reflejos del fondo de siempre): mezcla un tinte con caída radial.
float3 blob(float3 col, float2 fc, float2 ctr, float rad, float3 tint, float a) {
    float f = clamp(1.0 - length(fc - ctr) / rad, 0.0, 1.0);
    return mix(col, tint, a * f);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;

    // Mismo degradado malva de la app (#241239, #4C2D57, #643A70, #72436C)...
    float3 c0 = float3(0.141, 0.071, 0.224);
    float3 c1 = float3(0.298, 0.176, 0.341);
    float3 c2 = float3(0.392, 0.227, 0.439);
    float3 c3 = float3(0.447, 0.263, 0.424);
    float3 col = mix(c0, c1, smoothstep(0.0, 0.35, uv.y));
    col = mix(col, c2, smoothstep(0.35, 0.70, uv.y));
    col = mix(col, c3, smoothstep(0.70, 1.0, uv.y));

    // ...y los mismos reflejos lila, rosa, magenta y cálido, que se mecen despacio.
    float m = max(iResolution.x, iResolution.y);
    float2 R = iResolution;
    col = blob(col, fragCoord, float2(0.18 + 0.16 * sin(iTime * 0.113), 0.16 + 0.10 * cos(iTime * 0.157)) * R, 0.80 * m, float3(0.498, 0.361, 0.608), 0.31);
    col = blob(col, fragCoord, float2(0.88 + 0.14 * sin(iTime * 0.126 + 1.0), 0.42 + 0.12 * cos(iTime * 0.138 + 2.0)) * R, 0.72 * m, float3(0.482, 0.255, 0.424), 0.25);
    col = blob(col, fragCoord, float2(0.28 + 0.20 * sin(iTime * 0.151 + 2.0), 0.80 + 0.09 * cos(iTime * 0.126 + 4.0)) * R, 0.90 * m, float3(0.557, 0.192, 0.561), 0.31);
    col = blob(col, fragCoord, float2(0.78 + 0.12 * sin(iTime * 0.126 + 3.0), 0.92 + 0.05 * cos(iTime * 0.157 + 6.0)) * R, 0.55 * m, float3(0.910, 0.647, 0.459), 0.22);

    float amp = 0.030 + 0.050 * iEnergy;
    float speed = 0.45 + 0.9 * iEnergy;
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float baseY = 0.26 + fi * 0.19;
        float w1 = sin(uv.x * (4.5 + fi * 1.6) + iTime * speed * (0.55 + fi * 0.20) + fi * 1.9);
        float w2 = sin(uv.x * (10.0 - fi) - iTime * speed * (0.80 + fi * 0.12) + fi * 0.7);
        float y = baseY + amp * (w1 + 0.45 * w2);
        float d = abs(uv.y - y);
        float core = 1.0 - smoothstep(0.0, 0.006 + 0.004 * iEnergy, d);
        float halo = exp(-d * 34.0);
        float3 base = mix(float3(0.80, 0.40, 1.00), float3(1.00, 0.88, 0.64), fi / 3.0);
        float k = clamp(length(iAccent - float3(0.80, 0.40, 1.00)) * 1.2, 0.0, 1.0);
        float3 tint = mix(base, iAccent, 0.65 * k);
        col += tint * (core * (0.16 + 0.12 * iEnergy) + halo * (0.07 + 0.16 * iEnergy));
    }

    float2 c = float2((uv.x - 0.82) * aspect, uv.y - 0.86);
    float r = length(c);
    float groove = 0.5 + 0.5 * sin(r * 160.0 - iTime * (0.6 + 1.6 * iEnergy));
    float fade = 1.0 - smoothstep(0.05, 0.55, r);
    col += float3(0.55, 0.30, 0.70) * groove * fade * 0.05;

    col += (hash(fragCoord + iTime) - 0.5) / 255.0;
    return half4(half3(col), half(1.0));
}
"""
    }
}
