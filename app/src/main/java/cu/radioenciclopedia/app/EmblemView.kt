package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

/** Símbolo de la variante blanca publicada por la emisora, integrado sobre el fondo. */
class EmblemView(ctx: Context) : View(ctx) {
    private val logo = BitmapFactory.decodeResource(resources, R.drawable.logo_oficial_blanco)
    private val source = Rect(0, 0, 84, logo.height)
    private val target = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 255f,
            0f, 0f, 0f, 0f, 255f,
            0f, 0f, 0f, 0f, 255f,
            0.228f, 0.766f, 0.077f, 0f, -18.2f
        )))
    }
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(c: Canvas) {
        val scale = minOf(width * 0.82f / source.width(), height * 0.88f / source.height())
        val w = source.width() * scale; val h = source.height() * scale
        target.set((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2)
        c.drawBitmap(logo, source, target, paint)
    }
}
