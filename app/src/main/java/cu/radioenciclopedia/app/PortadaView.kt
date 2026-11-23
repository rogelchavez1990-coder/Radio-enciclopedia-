package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.max
import kotlin.math.sin

/** Amanecer habanero: ilustración, movimiento de cámara lento y luz cálida. */
class PortadaView(ctx: Context) : View(ctx), Animated {
    private val image = BitmapFactory.decodeResource(resources, R.drawable.art_amanecer)
    private val photo = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunlight = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        setMeasuredDimension(w, (w * 1.10f).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w == 0 || h == 0) return
        val scale = max(w.toFloat() / image.width, h.toFloat() / image.height)
        val bw = image.width * scale
        val bh = image.height * scale
        // Alineación superior: el logotipo queda en el cielo, sin tapar la cúpula.
        bounds.set(-bw / 2, -h / 2f, bw / 2, bh - h / 2f)
        shade.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x24180B2E, 0x00180B2E, 0x00180B2E, 0xC21A0C2C.toInt(), Pal.DEEP),
            floatArrayOf(0f, 0.20f, 0.56f, 0.88f, 1f), Shader.TileMode.CLAMP
        )
        sunlight.shader = RadialGradient(
            w * 0.90f, h * 0.51f, w * 0.48f,
            intArrayOf(0x24FFD7A3, 0x00FFD7A3), null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        if (width == 0 || height == 0) return
        val t = Clock.t
        val zoom = 1.035f + 0.012f * sin(t * 0.11f)
        c.save()
        c.translate(width / 2f + sin(t * 0.075f) * width * 0.006f, height / 2f)
        c.scale(zoom, zoom)
        c.drawBitmap(image, null, bounds, photo)
        c.restore()
        sunlight.alpha = (180 + 30 * sin(t * 0.21f)).toInt()
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sunlight)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shade)
    }

    override fun tick(t: Float) { invalidate() }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Clock.register(this)
    }

    override fun onDetachedFromWindow() {
        Clock.unregister(this)
        super.onDetachedFromWindow()
    }
}
