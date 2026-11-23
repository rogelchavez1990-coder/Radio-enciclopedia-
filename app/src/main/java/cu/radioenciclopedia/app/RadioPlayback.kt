package cu.radioenciclopedia.app

import android.content.Context
import android.content.Intent

enum class PlaybackStatus { IDLE, CONNECTING, PLAYING, PAUSED, ERROR }

data class PlaybackInfo(val status: PlaybackStatus, val message: String = "") {
    val active: Boolean get() = status == PlaybackStatus.PLAYING || status == PlaybackStatus.CONNECTING
    val label: String get() = when (status) {
        PlaybackStatus.IDLE -> "Toca para escuchar en directo"
        PlaybackStatus.CONNECTING -> "Conectando con la emisora…"
        PlaybackStatus.PLAYING -> "Reproduciendo en directo"
        PlaybackStatus.PAUSED -> "En pausa · vuelve a la señal en vivo"
        PlaybackStatus.ERROR -> message.ifBlank { "No se pudo conectar · toca para reintentar" }
    }
    val action: String get() = when (status) {
        PlaybackStatus.CONNECTING -> "Cancelar conexión"
        PlaybackStatus.PLAYING -> "Pausar radio"
        PlaybackStatus.ERROR -> "Reintentar radio"
        else -> "Reproducir radio"
    }
}

/** Estado compartido del servicio y sus controles. Todas las actualizaciones ocurren en main. */
object RadioPlayback {
    var info = PlaybackInfo(PlaybackStatus.IDLE)
        private set
    private val listeners = LinkedHashSet<(PlaybackInfo) -> Unit>()

    internal fun update(value: PlaybackInfo) {
        info = value
        listeners.toList().forEach { it(value) }
    }
    fun listen(listener: (PlaybackInfo) -> Unit) { listeners.add(listener); listener(info) }
    fun unlisten(listener: (PlaybackInfo) -> Unit) { listeners.remove(listener) }

    fun toggle(context: Context) {
        if (info.active) pause(context) else play(context)
    }
    fun play(context: Context) {
        context.startForegroundService(Intent(context, RadioService::class.java).setAction(RadioService.PLAY))
    }

    /** Cambia de emisora; si estaba sonando, la señal nueva arranca sola. Devuelve true si cambió. */
    fun switchStation(context: Context, station: Station, startIfStopped: Boolean = false): Boolean {
        val changed = Stations.select(context, station)
        if (info.active) {
            if (changed) {
                context.startForegroundService(
                    Intent(context, RadioService::class.java).setAction(RadioService.PLAY).putExtra(RadioService.RESTART, true)
                )
            }
        } else if (startIfStopped) {
            play(context)
        }
        return changed
    }
    fun pause(context: Context) {
        context.startService(Intent(context, RadioService::class.java).setAction(RadioService.PAUSE))
    }
}
