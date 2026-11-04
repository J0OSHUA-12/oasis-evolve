package com.oasisevolve.launcher

import com.oasisevolve.launcher.focus.AppPolicy
import com.oasisevolve.launcher.focus.GUARD_GRACE_MS
import com.oasisevolve.launcher.focus.GuardBook
import com.oasisevolve.launcher.focus.Verdict
import com.oasisevolve.launcher.focus.isInForeground
import com.oasisevolve.launcher.focus.policiesOf
import com.oasisevolve.launcher.focus.wasAwayLongerThan
import com.oasisevolve.launcher.quiet.QuietWindow
import com.oasisevolve.launcher.quiet.postponeUntil
import com.oasisevolve.launcher.storage.Backup
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.Note
import com.oasisevolve.launcher.storage.appItem
import com.oasisevolve.launcher.storage.parseBackup
import com.oasisevolve.launcher.storage.toJson
import com.oasisevolve.launcher.usage.EventType.ACTIVITY_PAUSED
import com.oasisevolve.launcher.usage.EventType.ACTIVITY_RESUMED
import com.oasisevolve.launcher.usage.EventType.ACTIVITY_STOPPED
import com.oasisevolve.launcher.usage.EventType.SCREEN_NON_INTERACTIVE
import com.oasisevolve.launcher.usage.RawEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class GuardAndQuietTest {
    private val tiktok = "com.zhiliaoapp.musically"
    private val guarded = AppPolicy(guarded = true, timerMinutes = null)
    private val timed = AppPolicy(guarded = false, timerMinutes = 10)
    private fun ev(type: Int, ts: Long, pkg: String? = tiktok, cls: String = "Main") = RawEvent(type, pkg, cls, ts)

    @Test
    fun `una app de nivel 4 abierta sin pasar por el launcher se bloquea`() {
        assertEquals(Verdict.Block(false), GuardBook().onForeground(tiktok, 1_000, guarded, awayTooLong = false))
    }

    @Test
    fun `tras pasar la interrupcion se puede usar y volver enseguida`() {
        val book = GuardBook()
        book.grant(tiktok, 0, null)
        assertEquals(Verdict.Allow, book.onForeground(tiktok, 5_000, guarded, awayTooLong = false))
        assertEquals(Verdict.Allow, book.onForeground(tiktok, 30_000, guarded, awayTooLong = false))
    }

    @Test
    fun `volver desde Recientes despues de un rato se bloquea`() {
        // El caso que pediste: dejar TikTok en Recientes y volver más tarde.
        val book = GuardBook()
        book.grant(tiktok, 0, null)
        assertEquals(Verdict.Block(false), book.onForeground(tiktok, 600_000, guarded, awayTooLong = true))
        // Y no queda una sesión colgada: la siguiente vez también se bloquea.
        assertEquals(Verdict.Block(false), book.onForeground(tiktok, 601_000, guarded, awayTooLong = false))
    }

    @Test
    fun `el temporizador termina la sesion y la siguiente apertura pasa por la interrupcion`() {
        val book = GuardBook()
        val started = book.onForeground(tiktok, 0, timed, awayTooLong = false)
        assertEquals(Verdict.Started(600_000), started)
        assertEquals(Verdict.Allow, book.onForeground(tiktok, 300_000, timed, awayTooLong = false))
        assertTrue(book.expire(tiktok, 600_000))
        // Aunque solo tenga temporizador, al acabarse el tiempo pasa por la interrupción.
        assertEquals(Verdict.Block(true), book.onForeground(tiktok, 610_000, timed, awayTooLong = false))
        book.grant(tiktok, 620_000, 10)
        assertEquals(Verdict.Allow, book.onForeground(tiktok, 630_000, timed, awayTooLong = false))
    }

    @Test
    fun `un aviso de fin antiguo no corta una sesion nueva`() {
        val book = GuardBook()
        book.grant(tiktok, 0, 5)
        book.grant(tiktok, 100_000, 5)
        assertFalse(book.expire(tiktok, 300_000))
        assertEquals(Verdict.Allow, book.onForeground(tiktok, 200_000, timed, awayTooLong = false))
    }

    @Test
    fun `sin bloquear, una app con temporizador vuelve a empezar tras salir un rato`() {
        val book = GuardBook()
        book.onForeground(tiktok, 0, timed, awayTooLong = false)
        assertEquals(Verdict.Started(900_000), book.onForeground(tiktok, 300_000, timed, awayTooLong = true))
    }

    @Test
    fun `salir menos de un minuto no cuenta como haberse ido`() {
        val events = listOf(ev(ACTIVITY_PAUSED, 10_000), ev(ACTIVITY_RESUMED, 40_000))
        assertFalse(wasAwayLongerThan(events, tiktok, 0, 40_000, GUARD_GRACE_MS))
    }

    @Test
    fun `salir mas de un minuto o apagar la pantalla si cuenta`() {
        val left = listOf(ev(ACTIVITY_PAUSED, 10_000), ev(ACTIVITY_RESUMED, 90_000))
        assertTrue(wasAwayLongerThan(left, tiktok, 0, 90_000, GUARD_GRACE_MS))
        val screenOff = listOf(ev(SCREEN_NON_INTERACTIVE, 10_000, pkg = null))
        assertTrue(wasAwayLongerThan(screenOff, tiktok, 0, 100_000, GUARD_GRACE_MS))
    }

    @Test
    fun `cambiar de pantalla dentro de la app no cuenta como salir`() {
        val events = listOf(
            ev(ACTIVITY_PAUSED, 10_000, cls = "A"),
            ev(ACTIVITY_RESUMED, 10_100, cls = "B"),
            ev(ACTIVITY_STOPPED, 10_500, cls = "A"),
        )
        assertFalse(wasAwayLongerThan(events, tiktok, 0, 500_000, GUARD_GRACE_MS))
        assertTrue(isInForeground(events, tiktok))
    }

    @Test
    fun `saber si la app sigue delante al acabarse el tiempo`() {
        assertFalse(isInForeground(listOf(ev(ACTIVITY_RESUMED, 0), ev(ACTIVITY_PAUSED, 10)), tiktok))
        assertFalse(isInForeground(listOf(ev(ACTIVITY_RESUMED, 0), ev(ACTIVITY_RESUMED, 10, pkg = "otra")), tiktok))
        assertFalse(isInForeground(listOf(ev(ACTIVITY_RESUMED, 0), ev(SCREEN_NON_INTERACTIVE, 10, pkg = null)), tiktok))
    }

    @Test
    fun `se vigilan las apps de nivel 4 y las que tienen temporizador`() {
        val settings = LauncherSettings(
            accessLevels = mapOf("$tiktok/.Main#0" to 4, "com.whatsapp/.Home#0" to 2),
            appTimers = mapOf("com.instagram.android/.Main#0" to 15, "$tiktok/.Main#0" to 10),
        )
        assertEquals(
            mapOf(tiktok to AppPolicy(true, 10), "com.instagram.android" to AppPolicy(false, 15)),
            policiesOf(settings),
        )
    }

    private val zone = ZoneId.of("America/La_Paz")
    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 3, hour, minute).atZone(zone).toInstant()
    private val beforeSleep = QuietWindow(21 * 60, 60)

    @Test
    fun `fuera de la franja se pospone hasta su inicio`() {
        assertEquals(at(21), postponeUntil(tiktok, true, setOf(tiktok), beforeSleep, at(14, 30), zone))
        // Después de la franja, hasta la del día siguiente.
        assertEquals(at(21).plusSeconds(86_400), postponeUntil(tiktok, true, setOf(tiktok), beforeSleep, at(22, 10), zone))
    }

    @Test
    fun `dentro de la franja, de otras apps o no descartables pasan`() {
        assertNull(postponeUntil(tiktok, true, setOf(tiktok), beforeSleep, at(21, 30), zone))
        assertNull(postponeUntil("com.whatsapp", true, setOf(tiktok), beforeSleep, at(14), zone))
        assertNull(postponeUntil(tiktok, false, setOf(tiktok), beforeSleep, at(14), zone))
    }

    @Test
    fun `una franja que cruza la medianoche`() {
        val late = QuietWindow(23 * 60, 120)
        assertTrue(late.isOpen(at(0, 30), zone))
        assertFalse(late.isOpen(at(1, 0), zone))
        assertEquals(at(23), late.nextOpening(at(1, 0), zone))
    }

    @Test
    fun `la copia de seguridad conserva ajustes y notas`() {
        val settings = LauncherSettings(
            homeLayout = listOf(appItem("a", 1, 2)),
            appTimers = mapOf("a" to 15),
            quietApps = setOf(tiktok),
            quietStartMinute = 22 * 60,
            layoutSeeded = true,
        )
        val notes = listOf(Note(text = "Estudiar «álgebra»\ncon saltos", updatedAt = 42))
        val restored = parseBackup(Backup(settings, notes).toJson(exportedAt = 1))!!
        assertEquals(settings, restored.settings)
        assertEquals(notes.map { it.text to it.updatedAt }, restored.notes.map { it.text to it.updatedAt })
        assertNull(parseBackup("""{"settings": {}}"""))
        assertNull(parseBackup("no es json"))
    }

    @Test
    fun `una hora sin el instante exacto de inicio usa la apertura correcta`() {
        assertEquals(at(21), beforeSleep.nextOpening(at(20, 59), zone))
        assertEquals(Instant.ofEpochSecond(at(21).epochSecond + 86_400), beforeSleep.nextOpening(at(21), zone))
    }
}
