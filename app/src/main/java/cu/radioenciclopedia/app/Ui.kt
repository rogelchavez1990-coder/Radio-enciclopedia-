package cu.radioenciclopedia.app

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** Marca documentada en el dossier: malva, azul de firma y lila del CSS; fondos de apoyo. */
object Pal {
    val DEEP = Color.parseColor("#11091D")
    val MID = Color.parseColor("#241239")
    val VIOLET = Color.rgb(57, 64, 134)
    val BRAND = Color.rgb(158, 95, 163)
    val ROSE = Color.rgb(244, 215, 237)
    val INK = Color.rgb(40, 27, 48)
    val MAGENTA = Color.parseColor("#A00F84")
    val PINK = Color.rgb(204, 102, 255)
    val GOLD = Color.parseColor("#FFE1A3") // Acento cálido de apoyo.
    val GOLD_LIGHT = Color.parseColor("#FFE9A8")
    val GOLD_DEEP = Color.parseColor("#C98616")
    val LILAC = Color.parseColor("#DED3E8")
    val GREEN = Color.parseColor("#3DDC84")
    val WHITE = Color.WHITE
}

object Fonts {
    var regular: Typeface = Typeface.DEFAULT
    var bold: Typeface = Typeface.DEFAULT_BOLD
    /** El rótulo oficial se muestra como imagen, sin sustituir su lettering. */
    var script: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
    var brand: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    var cond: Typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)

    /** Fuentes complementarias, no atribuidas a la firma oficial rasterizada. Ambas SIL OFL. */
    fun init(ctx: Context) {
        try {
            regular = ctx.resources.getFont(R.font.source_sans_regular)
            bold = ctx.resources.getFont(R.font.source_sans_semibold)
            script = ctx.resources.getFont(R.font.fira_semibold_italic)
            brand = ctx.resources.getFont(R.font.marcellus_regular)
            cond = ctx.resources.getFont(R.font.firacond_medium)
        } catch (e: Exception) {
            // si fallara, se conservan las fuentes del sistema
        }
    }
}

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun withAlpha(color: Int, a: Float): Int =
    (color and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 24)

fun lerpColor(a: Int, b: Int, t: Float): Int {
    val k = t.coerceIn(0f, 1f)
    return Color.argb(
        (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * k).toInt(),
        (Color.red(a) + (Color.red(b) - Color.red(a)) * k).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * k).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).toInt()
    )
}

fun Context.label(s: CharSequence, sizeSp: Float, color: Int = Pal.WHITE, bold: Boolean = false): TextView {
    val tv = TextView(this)
    tv.text = s
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    tv.setTextColor(color)
    tv.typeface = if (bold) Fonts.bold else Fonts.regular
    tv.includeFontPadding = false
    return tv
}

/**
 * Título de sección en cursiva de marca: una sola línea que se reduce hasta [minSp]
 * antes de partir una palabra (por ejemplo «Comunidad» con letra ampliada o en pantallas estrechas).
 * El ancho del contenedor debe estar acotado (MATCH_PARENT o peso en un LinearLayout).
 */
fun Context.sectionTitle(s: CharSequence, maxSp: Int = 32, minSp: Int = 20): TextView {
    val tv = label(s, maxSp.toFloat(), Pal.WHITE)
    tv.typeface = Fonts.script
    tv.maxLines = 1
    tv.setAutoSizeTextTypeUniformWithConfiguration(minSp, maxSp, 1, TypedValue.COMPLEX_UNIT_SP)
    return tv
}

/** Texto con degradado dorado vertical. */
class GoldText(ctx: Context) : TextView(ctx) {
    init {
        includeFontPadding = false
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (changed || paint.shader == null) {
            paint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat().coerceAtLeast(1f),
                intArrayOf(Pal.GOLD_LIGHT, Pal.GOLD, Pal.GOLD_DEEP),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
            )
        }
    }
}
