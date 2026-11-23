package cu.radioenciclopedia.app

import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.View
import android.view.animation.LinearInterpolator

/** Vista con animación ambiental: recibe el tiempo (en segundos) unas 30 veces por segundo. */
interface Animated {
    fun tick(t: Float)
}

/**
 * Reloj único para TODAS las animaciones decorativas (un solo animador, ~30 fps).
 * - Se detiene cuando la app no está visible (onPause) para no gastar batería.
 * - Si el usuario desactivó las animaciones del sistema, no arranca y las vistas muestran un
 *   fotograma fijo (t = 3 s).
 */
object Clock {
    private val items = ArrayList<Animated>()
    private var animator: ValueAnimator? = null
    private var last = 0L
    private var acc = 3.0

    @JvmField
    var t: Float = 3f

    fun register(a: Animated) {
        if (!items.contains(a)) items.add(a)
    }

    fun unregister(a: Animated) {
        items.remove(a)
    }

    fun running(): Boolean = animator != null

    fun start() {
        if (animator != null) return
        if (!ValueAnimator.areAnimatorsEnabled()) return
        last = SystemClock.uptimeMillis()
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 1000L
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        a.addUpdateListener { tick() }
        animator = a
        a.start()
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val dt = now - last
        if (dt < 30L) return
        last = now
        acc += dt / 1000.0
        if (acc > 100000.0) acc -= 100000.0
        t = acc.toFloat()
        var i = items.size - 1
        while (i >= 0) {
            if (i < items.size) {
                val a = items[i]
                if ((a as View).isShown) a.tick(t)
            }
            i--
        }
    }
}
