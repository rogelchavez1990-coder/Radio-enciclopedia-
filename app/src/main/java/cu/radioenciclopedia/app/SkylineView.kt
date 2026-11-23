package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ilustración de La Habana (arte vectorial renderizado a WebP) con vida encima:
 * deriva suave de la cámara, estrellas que titilan, luces de ventanas que parpadean
 * y, opcionalmente, una bandera cubana que ondea (malla deformada).
 */
class SkylineView(
    ctx: Context,
    artRes: Int,
    private val aspect: Float,
    private val stars: Boolean,
    private val flag: Boolean
) : View(ctx), Animated {

    private val art: Bitmap = BitmapFactory.decodeResource(resources, artRes)
    private val flagBmp: Bitmap? =
        if (flag) BitmapFactory.decodeResource(resources, R.drawable.art_bandera) else null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pole = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Pal.GOLD
    }
    private val finial = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Pal.GOLD_LIGHT }
    private val dst = RectF()

    private val rnd = Random(21)
    private val nStars = 30
    private val stx = FloatArray(nStars) { rnd.nextFloat() }
    private val sty = FloatArray(nStars) { rnd.nextFloat() * 0.5f }
    private val stp = FloatArray(nStars) { 1f + rnd.nextFloat() * 2.5f }
    private val sth = FloatArray(nStars) { rnd.nextFloat() * 6.283f }

    private val nGlints = 24
    private val gx = FloatArray(nGlints) { 0.04f + rnd.nextFloat() * 0.92f }
    private val gy = FloatArray(nGlints) { 0.70f + rnd.nextFloat() * 0.17f }
    private val gp = FloatArray(nGlints) { 0.6f + rnd.nextFloat() * 1.6f }
    private val gh = FloatArray(nGlints) { rnd.nextFloat() * 6.283f }

    private val meshW = 14
    private val meshH = 5
    private val verts = FloatArray((meshW + 1) * (meshH + 1) * 2)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (w * aspect).toInt())
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = Clock.t
        val d = resources.displayMetrics.density

        // deriva lenta de la "cámara"
        val s = 1.035f + 0.02f * sin(t * 0.21f)
        val dx = sin(t * 0.17f) * w * 0.012f
        c.save()
        c.scale(s, s, w / 2f, h)
        c.translate(dx, 0f)
        dst.set(0f, 0f, w, h)
        c.drawBitmap(art, null, dst, paint)
        c.restore()

        if (stars) {
            for (i in 0 until nStars) {
                val a = 0.15f + 0.85f * (0.5f + 0.5f * sin(t * stp[i] + sth[i]))
                glow.color = withAlpha(Pal.WHITE, a * 0.9f)
                c.drawCircle(stx[i] * w, sty[i] * h, (0.7f + 0.9f * a) * d, glow)
            }
        }
        // luces de ventanas que parpadean
        for (i in 0 until nGlints) {
            val v = max(0f, sin(t * gp[i] + gh[i])).pow(8)
            if (v > 0.03f) {
                glow.color = withAlpha(0xFFFFE3A0.toInt(), v * 0.85f)
                c.drawCircle(gx[i] * w, gy[i] * h, (1.2f + 1.6f * v) * d, glow)
            }
        }

        val fb = flagBmp
        if (fb != null) {
            val poleX = w * 0.60f
            val top = h * 0.62f
            val fw = w * 0.15f
            val fh = fw * 0.5f
            pole.strokeWidth = 2.6f * d
            c.drawLine(poleX, top - 3f * d, poleX, h * 0.92f, pole)
            c.drawCircle(poleX, top - 5f * d, 3.4f * d, finial)
            var k = 0
            for (j in 0..meshH) {
                for (i in 0..meshW) {
                    val fx = i / meshW.toFloat()
                    val wave = sin(fx * 7f - t * 4.2f) * fh * 0.08f * fx
                    verts[k++] = poleX + fx * fw
                    verts[k++] = top + (j / meshH.toFloat()) * fh + wave
                }
            }
            c.drawBitmapMesh(fb, meshW, meshH, verts, 0, null, 0, paint)
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
