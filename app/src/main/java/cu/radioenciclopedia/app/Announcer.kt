package cu.radioenciclopedia.app

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.accessibility.AccessibilityManager
import java.util.Locale

/**
 * Locutor: dice en voz alta lo que pasa con la radio (cambio de emisora, pausa, errores) para quien
 * no mira la pantalla. Usa el motor de voz del teléfono; no graba nada ni usa la red de la app.
 *
 * Cuándo habla:
 *  - Órdenes que llegan de fuera de la pantalla (auriculares, notificación, asistente): siempre que
 *    la app no se esté viendo, o si TalkBack está activo.
 *  - Con la app a la vista y TalkBack activo, TalkBack ya lee la interfaz: no se duplica.
 */
object Announcer {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    /** La app está en primer plano y con la pantalla encendida. */
    @Volatile var uiVisible = false

    /** El servicio baja el volumen de la radio mientras el locutor habla. */
    var onSpeaking: ((Boolean) -> Unit)? = null

    fun talkBackOn(context: Context): Boolean =
        context.getSystemService(AccessibilityManager::class.java)?.let { it.isEnabled && it.isTouchExplorationEnabled } == true

    /**
     * Dice [text]. [fromUi]: lo pidió la propia pantalla (allí TalkBack, si está activo, ya lee los cambios).
     * [force]: es la respuesta a una pregunta («qué suena», «ayuda»), así que se dice siempre.
     */
    fun say(context: Context, text: String, fromUi: Boolean = false, force: Boolean = false) {
        val app = context.applicationContext
        val talkBack = talkBackOn(app)
        val speak = if (fromUi) force && !talkBack else force || !uiVisible || talkBack
        if (!speak || text.isBlank()) return
        main.post { speak(app, text) }
    }

    fun stationLine(s: Station): String = s.spoken

    private fun speak(app: Context, text: String) {
        val engine = tts
        if (engine == null) {
            pending = text
            start(app)
        } else if (!ready) {
            pending = text
        } else {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "locutor")
        }
    }

    private fun start(app: Context) {
        tts = TextToSpeech(app) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) { tts = null; pending = null; return@TextToSpeech }
            for (l in listOf(Locale("es", "CU"), Locale("es", "MX"), Locale("es", "US"), Locale("es", "ES"), Locale("es"))) {
                if (engine.setLanguage(l) >= TextToSpeech.LANG_AVAILABLE) break
            }
            engine.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { main.post { onSpeaking?.invoke(true) } }
                override fun onDone(utteranceId: String?) { main.post { onSpeaking?.invoke(false) } }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { main.post { onSpeaking?.invoke(false) } }
            })
            ready = true
            pending?.let { engine.speak(it, TextToSpeech.QUEUE_FLUSH, null, "locutor") }
            pending = null
        }
    }
}
