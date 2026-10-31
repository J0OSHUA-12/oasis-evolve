package com.oasisevolve.launcher

import com.oasisevolve.launcher.drawer.filterBySearch
import com.oasisevolve.launcher.drawer.railLetter
import com.oasisevolve.launcher.drawer.searchScore
import com.oasisevolve.launcher.focus.AccessLevel
import com.oasisevolve.launcher.library.autoCategory
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.ThemeMode
import com.oasisevolve.launcher.wellbeing.ProgressPeriod
import com.oasisevolve.launcher.wellbeing.periodProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
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
    fun `el progreso de los periodos es correcto`() {
        val midJuly = LocalDateTime.of(2026, 7, 16, 12, 0)
        assertEquals(0.5f, periodProgress(ProgressPeriod.MONTH, midJuly), 0.01f)
        assertEquals(0.08f, periodProgress(ProgressPeriod.SEMESTER, midJuly), 0.01f)
        assertEquals(0.54f, periodProgress(ProgressPeriod.YEAR, midJuly), 0.01f)
        assertEquals(0f, periodProgress(ProgressPeriod.YEAR, LocalDateTime.of(2026, 1, 1, 0, 0)), 0.001f)
    }

    @Test
    fun `los ajustes sobreviven a un ciclo de guardado`() {
        val original = LauncherSettings(
            homeApps = listOf("a/.B#0", "c/.D#0"),
            accessLevels = mapOf("x" to 4),
            categoryOverrides = mapOf("x" to "Finanzas"),
            widgetIds = listOf(7),
            waitSeconds = 15,
            libraryEnabled = false,
            themeMode = ThemeMode.DARK,
        )
        assertEquals(original, LauncherSettings.fromJson(original.toJson()))
    }

    @Test
    fun `un json danado o antiguo no rompe los ajustes`() {
        assertEquals(LauncherSettings(), LauncherSettings.fromJson("no es json"))
        assertEquals(LauncherSettings(), LauncherSettings.fromJson(null))
        assertEquals(15, LauncherSettings.fromJson("""{"waitSeconds":15,"campoNuevo":1}""").waitSeconds)
    }
}
