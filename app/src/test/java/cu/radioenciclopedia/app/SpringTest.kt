package cu.radioenciclopedia.app

import org.junit.Assert.*
import org.junit.Test

class SpringTest {
    private fun run(ratio: Float, frames: Int, dt: Float = 1f / 60f): Pair<Spring, Float> {
        val s = Spring(0f, 300f, ratio)
        s.target = 1f
        var peak = 0f
        repeat(frames) {
            s.step(dt)
            peak = maxOf(peak, s.value)
        }
        return s to peak
    }

    @Test fun settlesExactlyOnTheTargetAndStops() {
        val s = Spring(0f, 300f, 0.6f)
        s.target = 1f
        var frames = 0
        while (s.step(1f / 60f) && frames < 600) frames++
        assertTrue("debe llegar al reposo", frames < 600)
        assertEquals(1f, s.value, 0f)
        assertEquals(0f, s.velocity, 0f)
    }

    @Test fun underdampedSpringBouncesPastTheTarget() {
        val (_, peak) = run(0.4f, 120)
        assertTrue("rebote esperado, pico $peak", peak > 1.05f)
    }

    @Test fun criticallyDampedSpringDoesNotOvershoot() {
        val (_, peak) = run(1.0f, 240)
        assertTrue("sin rebote, pico $peak", peak <= 1.01f)
    }

    @Test fun staysStableWithVeryLongFrames() {
        val (s, peak) = run(0.5f, 60, dt = 0.05f)
        assertFalse(s.value.isNaN())
        assertTrue("estable, pico $peak", peak < 1.6f)
        assertEquals(1f, s.value, 0.01f)
    }

    @Test fun anAlreadySettledSpringReportsNoMovement() {
        val s = Spring(0.5f, 300f, 0.6f)
        assertFalse(s.step(1f / 60f))
        assertEquals(0.5f, s.value, 0f)
    }
}
