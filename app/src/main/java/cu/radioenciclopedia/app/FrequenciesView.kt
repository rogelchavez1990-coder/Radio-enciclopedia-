package cu.radioenciclopedia.app

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView

class FrequenciesView(private val act: MainActivity) : ScrollView(act) {
    init {
        isVerticalScrollBarEnabled = false
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        addView(col, FrameLayout.LayoutParams(if (resources.configuration.screenWidthDp > 560) act.dpi(560) else MATCH, WRAP, Gravity.CENTER_HORIZONTAL))
        val head = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val words = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        words.addView(act.label("Frecuencias", 32f).apply { typeface = Fonts.script })
        words.addView(act.label("Cobertura nacional en FM · MHz", 14f, Pal.LILAC))
        head.addView(words, LinearLayout.LayoutParams(0, WRAP, 1f))
        head.addView(act.label("☰", 28f).apply {
            gravity = Gravity.CENTER; contentDescription = "Abrir menú"; isFocusable = true; setOnClickListener { act.openMenu() }
        }, LinearLayout.LayoutParams(act.dpi(48), act.dpi(48)))
        col.addView(head, lp(12))
        col.addView(act.label("Para sintonizar la emisora en un receptor FM. Dentro de esta aplicación escuchas por Internet.", 14f, Pal.LILAC), lp(10))
        for (entry in StationFrequencies.all) {
            val card = GlassCard(act, 18f, interactive = false)
            val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(act.dpi(14), act.dpi(13), act.dpi(14), act.dpi(13)) }
            row.addView(act.label(entry.province, 17f, Pal.WHITE, true), LinearLayout.LayoutParams(0, WRAP, 1f))
            row.addView(act.label(entry.mhz, 18f, Pal.GOLD, true).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = act.dpi(10) })
            card.addView(row, FrameLayout.LayoutParams(MATCH, WRAP))
            card.contentDescription = entry.province + ": " + entry.mhz + " MHz"
            card.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            col.addView(card, lp(8))
        }
        col.addView(act.label("FM (MHz) · Radio Enciclopedia", 13f, Pal.LILAC).apply { gravity = Gravity.CENTER; setPadding(0, act.dpi(12), 0, act.dpi(14)) }, lp(6))
    }
    private fun lp(top: Int) = LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(act.dpi(16), act.dpi(top), act.dpi(16), 0) }
}
