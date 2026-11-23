package cu.radioenciclopedia.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Cita del ensayo Maestros ambulantes, La América, mayo de 1884. */
class MartiCultureCard(act: MainActivity) : GlassCard(act, 24f, interactive = false) {
    private val quote = RevealText(act, "«Ser culto es el único modo de ser libre».").apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        typeface = Fonts.script
    }

    init {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dpi(14), act.dpi(14), act.dpi(14), act.dpi(14)) }
        col.addView(act.label("MARTÍ Y LA CULTURA", 12f, Pal.GOLD, true).apply { letterSpacing = 0.09f })
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val words = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, act.dpi(10), act.dpi(12), act.dpi(10)) }
        words.addView(quote)
        words.addView(act.label("José Martí\nMaestros ambulantes · 1884", 13f, Pal.LILAC), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(10) })
        row.addView(words, LinearLayout.LayoutParams(0, WRAP, 1f))
        val portraitWidth = (resources.configuration.screenWidthDp * 0.30f).toInt().coerceIn(82, 126)
        row.addView(MartiPortraitView(act), LinearLayout.LayoutParams(act.dpi(portraitWidth), act.dpi((portraitWidth * 1.32f).toInt())))
        col.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = act.dpi(12) })
        col.addView(act.label("Leer pensamiento completo  ↗", 14f, Pal.GOLD, true).apply {
            gravity = Gravity.CENTER; setPadding(0, act.dpi(12), 0, act.dpi(8)); isFocusable = true
            contentDescription = "Leer pensamiento de José Martí"; setOnClickListener { act.openLink(Links.MARTI) }
        }, LinearLayout.LayoutParams(MATCH, WRAP))
        addView(col, FrameLayout.LayoutParams(MATCH, WRAP))
    }

    /** Al mostrarse Comunidad, la cita se escribe palabra por palabra. */
    fun playEntrance() {
        quote.reveal(320L)
    }
}

/**
 * Texto que aparece palabra por palabra (cada una con su propio fundido). El texto
 * accesible es siempre la frase completa; solo cambia la transparencia visible.
 */
class RevealText(ctx: Context, private val full: String) : TextView(ctx) {
    private val words = ArrayList<IntRange>()
    private var anim: ValueAnimator? = null

    init {
        setTextColor(Pal.WHITE)
        includeFontPadding = false
        var start = -1
        full.forEachIndexed { i, ch ->
            if (ch == ' ') {
                if (start >= 0) words.add(start until i)
                start = -1
            } else if (start < 0) start = i
        }
        if (start >= 0) words.add(start until full.length)
        text = full
    }

    fun reveal(delay: Long) {
        anim?.cancel()
        if (!Motion.enabled() || words.isEmpty()) {
            text = full
            return
        }
        render(0f)
        anim = ValueAnimator.ofFloat(0f, words.size + 1f).apply {
            duration = 1500L
            startDelay = delay
            interpolator = LinearInterpolator()
            addUpdateListener { render(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    text = full
                }

                override fun onAnimationCancel(animation: Animator) {
                    text = full
                }
            })
            start()
        }
    }

    private fun render(p: Float) {
        val s = SpannableString(full)
        words.forEachIndexed { i, r ->
            val a = (p - i).coerceIn(0f, 1f)
            s.setSpan(ForegroundColorSpan(withAlpha(Pal.WHITE, a)), r.first, r.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        text = s
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }
}
