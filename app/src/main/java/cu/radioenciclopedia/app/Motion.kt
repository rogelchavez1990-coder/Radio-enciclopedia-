package cu.radioenciclopedia.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Motor de movimiento de la app: resortes físicos (sin AndroidX), háptica moderna y
 * utilidades de entrada. Respeta el ajuste del sistema que desactiva las animaciones.
 */
object Motion {
    fun enabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
    val main = Handler(Looper.getMainLooper())
}

/**
 * Resorte amortiguado integrado en pasos pequeños (estable con cualquier cuadro de pantalla).
 * [ratio] < 1 rebota (0,4 muy vivo · 0,6 elástico · 0,85 casi sin rebote); [rest] es el umbral de reposo.
 */
class Spring(
    var value: Float,
    private val stiffness: Float = 380f,
    ratio: Float = 0.62f,
    private val rest: Float = 0.002f
) {
    var target: Float = value
    var velocity: Float = 0f
    private val damping = 2f * ratio * sqrt(stiffness)

    /** Avanza [dt] segundos. Devuelve true mientras siga en movimiento. */
    fun step(dt: Float): Boolean {
        var left = dt
        while (left > 0f) {
            val h = min(left, 1f / 240f)
            val accel = -stiffness * (value - target) - damping * velocity
            velocity += accel * h
            value += velocity * h
            left -= h
        }
        if (abs(velocity) < rest * 4f && abs(value - target) < rest) {
            value = target
            velocity = 0f
            return false
        }
        return true
    }
}

/** Mueve un [Spring] con el reloj de pantalla (Choreographer) y notifica cada valor. */
class SpringAnim(val spring: Spring, private val onValue: (Float) -> Unit) : Choreographer.FrameCallback {
    var onEnd: (() -> Unit)? = null
    private var last = 0L
    private var running = false
    private var dead = false

    val value: Float get() = spring.value

    fun animateTo(target: Float, velocity: Float? = null) {
        if (dead) return
        spring.target = target
        if (velocity != null) spring.velocity = velocity
        if (!Motion.enabled()) {
            jump(target)
            return
        }
        if (!running) {
            running = true
            last = 0L
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Coloca el valor al instante, sin animar. */
    fun jump(v: Float) {
        cancel()
        spring.value = v
        spring.target = v
        spring.velocity = 0f
        onValue(v)
    }

    fun cancel() {
        if (running) {
            running = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    /** Detiene y desactiva para siempre (una animación de entrada que ya no debe arrancar). */
    fun kill() {
        dead = true
        cancel()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val dt = if (last == 0L) 1f / 60f else ((frameTimeNanos - last) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        last = frameTimeNanos
        val moving = spring.step(dt)
        onValue(spring.value)
        if (moving) {
            Choreographer.getInstance().postFrameCallback(this)
        } else {
            running = false
            onEnd?.invoke()
        }
    }
}

/** Háptica según la versión de Android: confirmaciones y «clics» de sintonía en 11+ y 14+. */
object Haptics {
    fun tap(v: View) {
        v.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        )
    }

    fun tick(v: View) {
        v.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            else HapticFeedbackConstants.CLOCK_TICK
        )
    }

    fun lock(v: View) {
        v.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        )
    }

    fun gestureStart(v: View) {
        v.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_START else HapticFeedbackConstants.VIRTUAL_KEY
        )
    }
}

/** Anula la animación de entrada pendiente o en curso de esta vista. */
fun View.cancelMotion() {
    (getTag(R.id.motion_anim) as? SpringAnim)?.kill()
    setTag(R.id.motion_anim, null)
}

/**
 * Entrada con resorte: la vista llega desde un desplazamiento ([dx], [dy]) y una escala
 * inicial, con un ligero rebote al asentarse. Con animaciones desactivadas no hace nada.
 */
fun View.springIn(
    delayMs: Long,
    dx: Float = 0f,
    dy: Float = 0f,
    fromScale: Float = 0.96f,
    ratio: Float = 0.62f,
    stiffness: Float = 300f
) {
    cancelMotion()
    if (!Motion.enabled()) {
        alpha = 1f; translationX = 0f; translationY = 0f; scaleX = 1f; scaleY = 1f
        return
    }
    val anim = SpringAnim(Spring(0f, stiffness, ratio)) { p ->
        translationX = dx * (1f - p)
        translationY = dy * (1f - p)
        val s = fromScale + (1f - fromScale) * p
        scaleX = s
        scaleY = s
        alpha = (p * 2f).coerceIn(0f, 1f)
    }
    setTag(R.id.motion_anim, anim)
    alpha = 0f
    translationX = dx
    translationY = dy
    scaleX = fromScale
    scaleY = fromScale
    Motion.main.postDelayed({ anim.animateTo(1f) }, delayMs)
}

/**
 * Pulsación con física: la tarjeta se hunde, se inclina hacia el dedo en 3D y deja un
 * foco de luz bajo él. Al soltar vuelve con un resorte.
 */
class PressTilt(private val v: View, private val maxDeg: Float = 6f) {
    var touchX = 0f
        private set
    var touchY = 0f
        private set
    var glowAmount = 0f
        private set

    private val scale = SpringAnim(Spring(1f, 520f, 0.55f)) { v.scaleX = it; v.scaleY = it }
    private val rx = SpringAnim(Spring(0f, 420f, 0.6f, 0.01f)) { v.rotationX = it }
    private val ry = SpringAnim(Spring(0f, 420f, 0.6f, 0.01f)) { v.rotationY = it }
    private val glow = SpringAnim(Spring(0f, 260f, 0.9f)) { glowAmount = it; v.invalidate() }

    fun onTouch(e: MotionEvent) {
        if (!Motion.enabled()) return
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.cameraDistance = 9000f * v.resources.displayMetrics.density
                aim(e)
                scale.animateTo(0.965f)
                glow.animateTo(1f)
            }
            MotionEvent.ACTION_MOVE -> aim(e)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                scale.animateTo(1f)
                rx.animateTo(0f)
                ry.animateTo(0f)
                glow.animateTo(0f)
            }
        }
    }

    private fun aim(e: MotionEvent) {
        val w = v.width.toFloat().coerceAtLeast(1f)
        val h = v.height.toFloat().coerceAtLeast(1f)
        val nx = ((e.x / w) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
        val ny = ((e.y / h) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
        touchX = e.x
        touchY = e.y
        ry.animateTo(nx * maxDeg)
        rx.animateTo(-ny * maxDeg)
    }

    fun cancel() {
        scale.jump(1f)
        rx.jump(0f)
        ry.jump(0f)
        glow.jump(0f)
    }
}

/** Corchea dibujada con formas, sin depender de que la fuente tenga el símbolo. */
fun drawMusicNote(c: Canvas, paint: Paint, x: Float, y: Float, s: Float, flag: Boolean) {
    c.save()
    c.rotate(-18f, x, y)
    c.drawOval(x - s * 0.5f, y - s * 0.34f, x + s * 0.5f, y + s * 0.34f, paint)
    c.restore()
    c.drawRect(x + s * 0.38f, y - s * 2.2f, x + s * 0.50f, y, paint)
    if (flag) c.drawOval(x + s * 0.50f, y - s * 2.2f, x + s * 1.05f, y - s * 1.55f, paint)
}

/** Icono de teléfono que «suena»: un breve temblor cada pocos segundos. */
class RingingIcon(ctx: Context) : ImageView(ctx), Animated {
    override fun tick(t: Float) {
        val c = t % 7f
        rotation = if (c < 0.8f) sin(c / 0.8f * 6.2832f * 3f) * 15f * (1f - c / 0.8f) else 0f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Clock.register(this)
    }

    override fun onDetachedFromWindow() {
        Clock.unregister(this)
        rotation = 0f
        super.onDetachedFromWindow()
    }
}
