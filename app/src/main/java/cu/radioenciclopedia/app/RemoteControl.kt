package cu.radioenciclopedia.app

import android.content.Context

/**
 * Cambios de emisora que llegan sin usar la pantalla: los botones de los auriculares y la notificación.
 * Cada cambio se confirma en voz alta con [Announcer].
 */
object RemoteControl {
    fun tune(app: Context, station: Station) {
        val wasActive = RadioPlayback.info.active
        val changed = RadioPlayback.switchStation(app, station, startIfStopped = true)
        Announcer.say(app, if (!changed && wasActive) "Ya estás escuchando " + Announcer.stationLine(station)
            else Announcer.stationLine(station))
    }

    fun step(app: Context, direction: Int) {
        tune(app, if (direction > 0) Stations.next() else Stations.previous())
    }
}
