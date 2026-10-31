package com.oasisevolve.launcher

import com.oasisevolve.launcher.drawer.filterBySearch
import com.oasisevolve.launcher.drawer.railLetter
import com.oasisevolve.launcher.drawer.searchScore
import com.oasisevolve.launcher.focus.AccessLevel
import com.oasisevolve.launcher.library.autoCategory
import com.oasisevolve.launcher.home.DragPayload
import com.oasisevolve.launcher.home.DropTarget
import com.oasisevolve.launcher.home.resolveDrop
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.appItem
import com.oasisevolve.launcher.storage.folderItem
import com.oasisevolve.launcher.storage.widgetItem
import com.oasisevolve.launcher.system.normalizeAppFilterComponent
import com.oasisevolve.launcher.wellbeing.AcademicSemester
import com.oasisevolve.launcher.wellbeing.TimePeriod
import com.oasisevolve.launcher.wellbeing.describeSpan
import com.oasisevolve.launcher.wellbeing.periodBounds
import com.oasisevolve.launcher.wellbeing.periodProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class LogicTest {
    @Test
    fun `la busqueda ignora acentos y mayusculas`() {
        assertEquals(0, searchScore("Cámara", "cama"))
        assertEquals(0, searchScore("CALCULADORA", "calc"))
    }

    @Test
    fun `la busqueda encuentra por palabra, iniciales y subcadena`() {
        assertEquals(1, searchScore("Google Maps", "maps"))
        assertEquals(2, searchScore("Google Maps", "gm"))
        assertEquals(3, searchScore("Obsidian", "sid"))
        assertNull(searchScore("Obsidian", "xyz"))
    }

    @Test
    fun `los resultados priorizan la mejor coincidencia`() {
        val result = filterBySearch(listOf("Google Maps", "Maps Pro", "Amapsito"), "maps") { it }
        assertEquals(listOf("Maps Pro", "Google Maps", "Amapsito"), result)
    }

    @Test
    fun `el riel usa letras sin acento y agrupa el resto`() {
        assertEquals('A', railLetter("Ágora"))
        assertEquals('#', railLetter("7zip"))
    }

    @Test
    fun `el formato de los paquetes de iconos se normaliza`() {
        assertEquals(
            "com.app/com.app.Main",
            normalizeAppFilterComponent("ComponentInfo{com.app/com.app.Main}"),
        )
        assertEquals("com.app/com.app.Main", normalizeAppFilterComponent(" ComponentInfo{com.app/.Main} "))
        assertNull(normalizeAppFilterComponent("no es un componente"))
        assertNull(normalizeAppFilterComponent("ComponentInfo{sin-clase}"))
    }

    @Test
    fun `la categoria usa pistas y despues la declarada por la app`() {
        assertEquals("Redes sociales", autoCategory("com.instagram.android", -1))
        assertEquals("Juegos", autoCategory("com.studio.aventura", 0))
        assertEquals("Otras", autoCategory("com.desconocida.app", -1))
    }

    @Test
    fun `los niveles controlan donde se ve cada app`() {
        assertTrue(AccessLevel.VISIBLE.inVisualScreens)
        assertFalse(AccessLevel.DRAWER_ONLY.inVisualScreens)
        assertTrue(AccessLevel.DRAWER_ONLY.inDrawer)
        assertFalse(AccessLevel.HIDDEN.inDrawer)
        assertFalse(AccessLevel.GUARDED.inDrawer)
        assertEquals(AccessLevel.VISIBLE, AccessLevel.of(null))
    }

    @Test
    fun `el dia va de medianoche a medianoche`() {
        val noon = LocalDateTime.of(2026, 10, 31, 12, 0)
        assertEquals(50, periodProgress(TimePeriod.DAY, noon).percent)
        assertEquals(0, periodProgress(TimePeriod.DAY, LocalDateTime.of(2026, 10, 31, 0, 0)).percent)
        assertEquals(99, periodProgress(TimePeriod.DAY, LocalDateTime.of(2026, 10, 31, 23, 59)).percent)
    }

    @Test
    fun `la semana respeta su inicio configurable`() {
        val saturday = LocalDateTime.of(2026, 10, 31, 12, 0)
        assertEquals(78, periodProgress(TimePeriod.WEEK, saturday, DayOfWeek.MONDAY).percent)
        assertEquals(92, periodProgress(TimePeriod.WEEK, saturday, DayOfWeek.SUNDAY).percent)
        assertEquals(7, periodProgress(TimePeriod.WEEK, saturday, DayOfWeek.SATURDAY).percent)
    }

    @Test
    fun `el mes usa su duracion real`() {
        val february = LocalDateTime.of(2026, 2, 15, 0, 0)
        assertEquals(50, periodProgress(TimePeriod.MONTH, february).percent)
        val leap = LocalDateTime.of(2028, 2, 15, 12, 0)
        assertEquals(50, periodProgress(TimePeriod.MONTH, leap).percent)
        val october = periodProgress(TimePeriod.MONTH, LocalDateTime.of(2026, 10, 31, 0, 0))
        assertEquals(96, october.percent)
    }

    @Test
    fun `el ano respeta los bisiestos`() {
        val endOfFebNormal = LocalDateTime.of(2026, 3, 1, 0, 0)
        val endOfFebLeap = LocalDateTime.of(2028, 3, 1, 0, 0)
        assertEquals(59f / 365f, periodProgress(TimePeriod.YEAR, endOfFebNormal).fraction, 0.0001f)
        assertEquals(60f / 366f, periodProgress(TimePeriod.YEAR, endOfFebLeap).fraction, 0.0001f)
    }

    @Test
    fun `el semestre calendario cambia en julio`() {
        assertEquals(LocalDate.of(2026, 1, 1) to LocalDate.of(2026, 7, 1), periodBounds(TimePeriod.SEMESTER, LocalDateTime.of(2026, 6, 30, 23, 0)))
        assertEquals(LocalDate.of(2026, 7, 1) to LocalDate.of(2027, 1, 1), periodBounds(TimePeriod.SEMESTER, LocalDateTime.of(2026, 7, 1, 0, 0)))
    }

    @Test
    fun `el semestre academico usa sus fechas y no se sale de rango`() {
        val academic = AcademicSemester(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15))
        val during = periodProgress(TimePeriod.SEMESTER, LocalDateTime.of(2026, 10, 8, 0, 0), academic = academic)
        // Del 1 de agosto al 8 de octubre hay 31 + 30 + 7 = 68 días, de un total de 137.
        assertEquals(68f / 137f, during.fraction, 0.0001f)
        val before = periodProgress(TimePeriod.SEMESTER, LocalDateTime.of(2026, 7, 1, 0, 0), academic = academic)
        assertEquals(0, before.percent)
        assertTrue(before.hasNotStarted)
        val after = periodProgress(TimePeriod.SEMESTER, LocalDateTime.of(2027, 1, 1, 0, 0), academic = academic)
        assertEquals(100, after.percent)
        assertTrue(after.hasEnded)
    }

    @Test
    fun `un semestre academico invalido usa el calendario`() {
        val invalid = AcademicSemester(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 8, 1))
        assertEquals(
            LocalDate.of(2026, 7, 1) to LocalDate.of(2027, 1, 1),
            periodBounds(TimePeriod.SEMESTER, LocalDateTime.of(2026, 10, 1, 0, 0), academic = invalid),
        )
    }

    @Test
    fun `transcurrido y restante suman el total`() {
        val progress = periodProgress(TimePeriod.YEAR, LocalDateTime.of(2026, 10, 31, 0, 0))
        assertEquals("303 días", describeSpan(progress.elapsed, TimePeriod.YEAR))
        assertEquals("62 días", describeSpan(progress.remaining, TimePeriod.YEAR))
        val day = periodProgress(TimePeriod.DAY, LocalDateTime.of(2026, 10, 31, 14, 10))
        assertEquals("14 h 10 min", describeSpan(day.elapsed, TimePeriod.DAY))
        val lastHours = periodProgress(TimePeriod.MONTH, LocalDateTime.of(2026, 10, 31, 20, 15))
        assertEquals("3 h 45 min", describeSpan(lastHours.remaining, TimePeriod.MONTH))
    }

    @Test
    fun `soltar en el dock solo admite apps`() {
        val dockApp = DragPayload.DockApp("a")
        val widget = DragPayload.GridElement(widgetItem(1, 0, 0, 2, 2))
        assertEquals(DropTarget.Dock(1), resolveDrop(emptyList(), dockApp, 0, 0, 0, 0, 4, 7, 1))
        assertEquals(DropTarget.None, resolveDrop(emptyList(), widget, 0, 0, 0, 0, 4, 7, 1))
    }

    @Test
    fun `soltar una app sobre otra agrupa y un widget encima es invalido`() {
        val a = appItem("a", 0, 0)
        val b = appItem("b", 1, 0)
        val w = widgetItem(1, 2, 2, 2, 2)
        val items = listOf(a, b, w)
        assertEquals(DropTarget.Merge(b), resolveDrop(items, DragPayload.GridElement(a), 1, 0, 1, 0, 4, 7, null))
        assertEquals(
            DropTarget.Cell(2, 2, false),
            resolveDrop(items, DragPayload.GridElement(a), 2, 2, 2, 2, 4, 7, null),
        )
        assertEquals(
            DropTarget.Cell(1, 0, false),
            resolveDrop(items, DragPayload.GridElement(w), 1, 0, 1, 0, 4, 7, null),
        )
        assertEquals(
            DropTarget.Cell(0, 3, true),
            resolveDrop(items, DragPayload.GridElement(a), 0, 3, 0, 3, 4, 7, null),
        )
    }

    @Test
    fun `una carpeta no se agrupa con otra`() {
        val folder = folderItem(1, 0, 0)
        val app = appItem("b", 1, 0)
        assertEquals(
            DropTarget.Cell(1, 0, false),
            resolveDrop(listOf(folder, app), DragPayload.GridElement(folder), 1, 0, 1, 0, 4, 7, null),
        )
    }

    @Test
    fun `un json danado o antiguo no rompe los ajustes`() {
        assertEquals(LauncherSettings(), LauncherSettings.fromJson("no es json"))
        assertEquals(LauncherSettings(), LauncherSettings.fromJson(null))
        assertEquals(15, LauncherSettings.fromJson("""{"waitSeconds":15,"campoNuevo":1}""").waitSeconds)
    }
}
