package com.oasisevolve.launcher

import com.oasisevolve.launcher.usage.DAY_MS
import com.oasisevolve.launcher.usage.EventType.ACTIVITY_PAUSED
import com.oasisevolve.launcher.usage.EventType.ACTIVITY_RESUMED
import com.oasisevolve.launcher.usage.EventType.ACTIVITY_STOPPED
import com.oasisevolve.launcher.usage.EventType.SCREEN_NON_INTERACTIVE
import com.oasisevolve.launcher.usage.RawEvent
import com.oasisevolve.launcher.usage.comparisonMessage
import com.oasisevolve.launcher.usage.computeUsage
import com.oasisevolve.launcher.usage.computeWindows
import com.oasisevolve.launcher.usage.coverageDays
import com.oasisevolve.launcher.usage.formatDays
import com.oasisevolve.launcher.usage.formatDuration
import com.oasisevolve.launcher.usage.isPeriodComparable
import com.oasisevolve.launcher.usage.percentChange
import com.oasisevolve.launcher.usage.projectTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCalculatorTest {
    private fun ev(type: Int, ts: Long, pkg: String? = "app", cls: String? = "A") = RawEvent(type, pkg, cls, ts)

    @Test
    fun `una sesion simple suma su duracion`() {
        val s = computeUsage(sequenceOf(ev(ACTIVITY_RESUMED, 100), ev(ACTIVITY_PAUSED, 400)), 0, 1000)
        assertEquals(300, s.perApp.getValue("app").foregroundMs)
        assertEquals(1, s.perApp.getValue("app").opens)
    }

    @Test
    fun `cambiar de actividad dentro de la app no duplica ni corta la apertura`() {
        val s = computeUsage(
            sequenceOf(
                ev(ACTIVITY_RESUMED, 100, cls = "A"),
                ev(ACTIVITY_RESUMED, 200, cls = "B"),
                ev(ACTIVITY_PAUSED, 210, cls = "A"),
                ev(ACTIVITY_PAUSED, 500, cls = "B"),
            ),
            0, 1000,
        )
        assertEquals(400, s.perApp.getValue("app").foregroundMs)
        assertEquals(1, s.perApp.getValue("app").opens)
    }

    @Test
    fun `un hueco breve entre actividades cuenta como una sola apertura`() {
        val s = computeUsage(
            sequenceOf(
                ev(ACTIVITY_RESUMED, 100, cls = "A"),
                ev(ACTIVITY_PAUSED, 200, cls = "A"),
                ev(ACTIVITY_RESUMED, 300, cls = "B"),
                ev(ACTIVITY_PAUSED, 400, cls = "B"),
            ),
            0, 10_000,
        )
        assertEquals(200, s.perApp.getValue("app").foregroundMs)
        assertEquals(1, s.perApp.getValue("app").opens)
    }

    @Test
    fun `un hueco largo cuenta como otra apertura`() {
        val s = computeUsage(
            sequenceOf(
                ev(ACTIVITY_RESUMED, 100),
                ev(ACTIVITY_PAUSED, 200),
                ev(ACTIVITY_RESUMED, 5000),
                ev(ACTIVITY_PAUSED, 5100),
            ),
            0, 10_000,
        )
        assertEquals(2, s.perApp.getValue("app").opens)
    }

    @Test
    fun `apagar la pantalla cierra la sesion`() {
        val s = computeUsage(
            sequenceOf(
                ev(ACTIVITY_RESUMED, 100),
                ev(SCREEN_NON_INTERACTIVE, 300, pkg = null, cls = null),
                ev(ACTIVITY_PAUSED, 900),
            ),
            0, 1000,
        )
        assertEquals(200, s.perApp.getValue("app").foregroundMs)
    }

    @Test
    fun `una sesion abierta cuenta hasta el final de la ventana`() {
        val s = computeUsage(sequenceOf(ev(ACTIVITY_RESUMED, 700)), 0, 1000)
        assertEquals(300, s.perApp.getValue("app").foregroundMs)
    }

    @Test
    fun `una app abierta antes de la ventana cuenta desde su inicio`() {
        val s = computeUsage(sequenceOf(ev(ACTIVITY_PAUSED, 300)), 100, 1000)
        assertEquals(200, s.perApp.getValue("app").foregroundMs)
    }

    @Test
    fun `un stop posterior a la pausa no suma tiempo`() {
        val s = computeUsage(
            sequenceOf(ev(ACTIVITY_RESUMED, 100), ev(ACTIVITY_PAUSED, 200), ev(ACTIVITY_STOPPED, 900)),
            0, 1000,
        )
        assertEquals(100, s.perApp.getValue("app").foregroundMs)
    }

    @Test
    fun `se excluye el propio launcher`() {
        val s = computeUsage(sequenceOf(ev(ACTIVITY_RESUMED, 0, pkg = "yo")), 0, 1000, setOf("yo"))
        assertTrue(s.perApp.isEmpty())
    }

    @Test
    fun `la cobertura refleja solo los dias con datos`() {
        assertEquals(0, coverageDays(null, 0, 30 * DAY_MS))
        assertEquals(30, coverageDays(0, 0, 30 * DAY_MS))
        assertEquals(10, coverageDays(20 * DAY_MS, 0, 30 * DAY_MS))
    }

    @Test
    fun `no se compara un periodo anterior incompleto`() {
        assertFalse(isPeriodComparable(null, 0, 7 * DAY_MS))
        assertFalse(isPeriodComparable(3 * DAY_MS, 0, 7 * DAY_MS))
        assertTrue(isPeriodComparable(3_600_000, 0, 7 * DAY_MS))
    }

    @Test
    fun `el cambio porcentual es correcto y exige una base`() {
        assertEquals(-12.0, percentChange(88, 100)!!, 0.0001)
        assertEquals(50.0, percentChange(150, 100)!!, 0.0001)
        assertNull(percentChange(10, 0))
    }

    @Test
    fun `la proyeccion coincide con el ejemplo de dos horas diarias`() {
        val p = projectTime(2 * 3_600_000L)
        assertEquals(14 * 3_600_000L, p.weekMs)
        assertEquals(60 * 3_600_000L, p.monthMs)
        assertEquals(730 * 3_600_000L, p.yearMs)
        assertEquals(30.4, p.yearInFullDays, 0.05)
    }

    @Test
    fun `una sola pasada da el mismo resultado que calcular cada ventana`() {
        val events = listOf(
            ev(ACTIVITY_RESUMED, 100),
            ev(ACTIVITY_PAUSED, 400),
            ev(ACTIVITY_RESUMED, 600, pkg = "otra"),
            ev(ACTIVITY_PAUSED, 900, pkg = "otra"),
        )
        val windows = listOf(0L to 1000L, 500L to 1000L, 0L to 500L)
        val combined = computeWindows(events.asSequence(), windows)
        windows.forEachIndexed { index, (start, end) ->
            val single = computeUsage(events.asSequence().filter { it.timestamp in start..end }, start, end)
            assertEquals(single, combined[index])
        }
        assertEquals(300, combined[1].perApp.getValue("otra").foregroundMs)
    }

    @Test
    fun `los mensajes de comparacion son correctos y respetuosos`() {
        val reduced = comparisonMessage(85 * 60_000L, 100 * 60_000L, "la semana anterior")!!
        assertTrue("15 %" in reduced)
        assertTrue("15 min menos" in reduced)
        val increased = comparisonMessage(110 * 60_000L, 100 * 60_000L, "la semana anterior")!!
        assertTrue("10 %" in increased)
        assertFalse("mal" in increased)
        assertEquals(
            "Se mantuvo igual que la semana anterior.",
            comparisonMessage(100 * 60_000L, 100 * 60_000L, "la semana anterior"),
        )
        assertNull(comparisonMessage(10, 0, "la semana anterior"))
    }

    @Test
    fun `los dias de la proyeccion usan un decimal`() {
        assertEquals("26.5", formatDays(26.46, java.util.Locale.US))
        assertEquals("26,5", formatDays(26.46, java.util.Locale.forLanguageTag("es-ES")))
    }

    @Test
    fun `las duraciones se formatean sin exagerar`() {
        assertEquals("0 min", formatDuration(0))
        assertEquals("menos de 1 min", formatDuration(30_000))
        assertEquals("45 min", formatDuration(45 * 60_000L))
        assertEquals("2 h", formatDuration(120 * 60_000L))
        assertEquals("2 h 30 min", formatDuration(150 * 60_000L))
    }
}
