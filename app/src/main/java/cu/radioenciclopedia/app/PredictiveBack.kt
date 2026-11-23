package cu.radioenciclopedia.app

import android.annotation.TargetApi
import android.os.Build
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/**
 * Gesto «atrás» predictivo (Android 13+; con progreso del dedo en Android 14+).
 *
 * El callback solo está registrado cuando la app tiene algo que cerrar (el menú abierto o
 * una pantalla distinta de Inicio). En Inicio, sin menú, no se registra y el sistema muestra
 * su propia animación de salida a la pantalla de inicio del teléfono.
 */
@TargetApi(33)
class PredictiveBack(private val act: MainActivity) {
    private var registered = false

    private val callback: OnBackInvokedCallback =
        if (Build.VERSION.SDK_INT >= 34) Preview() else OnBackInvokedCallback { act.handleBack() }

    fun update(enabled: Boolean) {
        val dispatcher = act.onBackInvokedDispatcher
        if (enabled && !registered) {
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
            registered = true
        } else if (!enabled && registered) {
            dispatcher.unregisterOnBackInvokedCallback(callback)
            registered = false
        }
    }

    @TargetApi(34)
    private inner class Preview : OnBackAnimationCallback {
        override fun onBackStarted(backEvent: BackEvent) {
            act.backProgress(0f, backEvent.swipeEdge)
        }

        override fun onBackProgressed(backEvent: BackEvent) {
            act.backProgress(backEvent.progress, backEvent.swipeEdge)
        }

        override fun onBackCancelled() {
            act.backCancel()
        }

        override fun onBackInvoked() {
            act.backCommit()
        }
    }
}
