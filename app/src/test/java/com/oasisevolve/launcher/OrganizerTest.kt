package com.oasisevolve.launcher

import com.oasisevolve.launcher.storage.Accent
import com.oasisevolve.launcher.storage.AppFolder
import com.oasisevolve.launcher.storage.AppFont
import com.oasisevolve.launcher.storage.IconShape
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.addCategory
import com.oasisevolve.launcher.storage.addToFolder
import com.oasisevolve.launcher.storage.applyVisibleOrder
import com.oasisevolve.launcher.storage.createFolder
import com.oasisevolve.launcher.storage.deleteCategory
import com.oasisevolve.launcher.storage.deleteFolder
import com.oasisevolve.launcher.storage.isBuiltInCategory
import com.oasisevolve.launcher.storage.removeFromFolder
import com.oasisevolve.launcher.storage.renameCategory
import com.oasisevolve.launcher.storage.toggleCategoryHidden
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizerTest {
    @Test
    fun `reordenar conserva la posicion de lo que no se muestra`() {
        val result = applyVisibleOrder(listOf("a", "oculta", "b", "c"), listOf("c", "a", "b"))
        assertEquals(listOf("c", "oculta", "a", "b"), result)
    }

    @Test
    fun `crear carpeta la coloca donde estaba la app`() {
        val s = LauncherSettings(homeApps = listOf("a", "b", "c")).createFolder("b", "Trabajo", 1)
        assertEquals(listOf("a", "folder:1", "c"), s.homeApps)
        assertEquals(listOf("b"), s.folders.single().apps)
    }

    @Test
    fun `crear carpeta de una app sin colocar la lleva a utiles`() {
        val s = LauncherSettings().createFolder("x", "Nueva", 2)
        assertEquals(listOf("folder:2"), s.usefulApps)
    }

    @Test
    fun `una app solo vive en una carpeta`() {
        val s = LauncherSettings(
            homeApps = listOf("folder:1", "folder:2", "z"),
            folders = listOf(AppFolder(1, "A", listOf("x", "y")), AppFolder(2, "B", listOf("w"))),
        ).addToFolder(2, "x").addToFolder(2, "z")
        assertEquals(listOf("y"), s.folders.first { it.id == 1L }.apps)
        assertEquals(listOf("w", "x", "z"), s.folders.first { it.id == 2L }.apps)
        assertEquals(listOf("folder:1", "folder:2"), s.homeApps)
    }

    @Test
    fun `mover la ultima app de una carpeta elimina la carpeta vacia`() {
        val s = LauncherSettings(
            homeApps = listOf("folder:1", "folder:2"),
            folders = listOf(AppFolder(1, "A", listOf("x")), AppFolder(2, "B", listOf("w"))),
        ).addToFolder(2, "x")
        assertEquals(listOf("folder:2"), s.homeApps)
        assertEquals(1, s.folders.size)
    }

    @Test
    fun `sacar una app la deja junto a la carpeta`() {
        val s = LauncherSettings(
            homeApps = listOf("folder:1", "z"),
            folders = listOf(AppFolder(1, "A", listOf("x", "y"))),
        ).removeFromFolder(1, "x")
        assertEquals(listOf("folder:1", "x", "z"), s.homeApps)
        assertEquals(listOf("y"), s.folders.single().apps)
    }

    @Test
    fun `sacar la ultima app elimina la carpeta`() {
        val s = LauncherSettings(
            usefulApps = listOf("folder:1"),
            folders = listOf(AppFolder(1, "A", listOf("x"))),
        ).removeFromFolder(1, "x")
        assertEquals(listOf("x"), s.usefulApps)
        assertTrue(s.folders.isEmpty())
    }

    @Test
    fun `borrar una carpeta nunca pierde sus apps`() {
        val s = LauncherSettings(
            homeApps = listOf("a", "folder:1", "b"),
            folders = listOf(AppFolder(1, "A", listOf("x", "y"))),
        ).deleteFolder(1)
        assertEquals(listOf("a", "x", "y", "b"), s.homeApps)
        assertTrue(s.folders.isEmpty())
    }

    @Test
    fun `no se admiten categorias vacias ni repetidas`() {
        val s = LauncherSettings()
        assertNull(s.addCategory("  "))
        assertNull(s.addCategory("juegos"))
        assertTrue("Robótica" in s.addCategory("Robótica")!!.categories)
    }

    @Test
    fun `renombrar una categoria arrastra sus apps y su estado oculto`() {
        val s = LauncherSettings(
            categoryOverrides = mapOf("app" to "Juegos"),
            hiddenCategories = setOf("Juegos"),
        ).renameCategory("Juegos", "Ocio")!!
        assertEquals("Ocio", s.categoryOverrides["app"])
        assertTrue("Ocio" in s.hiddenCategories)
        assertEquals("Ocio", s.categoryRenames["Juegos"])
        assertFalse("Juegos" in s.categories)
        val again = s.renameCategory("Ocio", "Diversión")!!
        assertEquals("Diversión", again.categoryRenames["Juegos"])
        assertNull(s.renameCategory("Ocio", "Finanzas"))
    }

    @Test
    fun `una categoria predeterminada renombrada no se puede borrar`() {
        val s = LauncherSettings().renameCategory("Juegos", "Ocio")!!
        assertTrue(s.isBuiltInCategory("Ocio"))
        assertEquals(s, s.deleteCategory("Ocio"))
    }

    @Test
    fun `borrar una categoria propia devuelve sus apps a la automatica`() {
        val s = LauncherSettings(categoryOverrides = mapOf("app" to "Robótica"))
            .addCategory("Robótica")!!
            .deleteCategory("Robótica")
        assertFalse("Robótica" in s.categories)
        assertNull(s.categoryOverrides["app"])
    }

    @Test
    fun `alternar ocultar una categoria es reversible`() {
        val s = LauncherSettings()
        assertEquals(s, s.toggleCategoryHidden("Juegos").toggleCategoryHidden("Juegos"))
    }

    @Test
    fun `todos los ajustes nuevos sobreviven a un ciclo de guardado`() {
        val original = LauncherSettings(
            folders = listOf(AppFolder(3, "Estudio", listOf("a", "b"))),
            categories = listOf("Ocio", "Robótica"),
            hiddenCategories = setOf("Ocio"),
            categoryRenames = mapOf("Juegos" to "Ocio"),
            homeWidgetIds = listOf(4, 9),
            widgetHeights = mapOf(4 to 180),
            iconSizeDp = 64,
            gridSpacingDp = 20,
            pageMarginDp = 32,
            showHomeWidgets = false,
            libraryExpanded = true,
            glassBlur = true,
            accent = Accent.VIOLET,
            font = AppFont.OUTFIT,
            fontScale = 1.1f,
            iconShape = IconShape.CIRCLE,
            iconPack = "com.pack",
        )
        assertEquals(original, LauncherSettings.fromJson(original.toJson()))
    }

    @Test
    fun `los ajustes de la version anterior siguen siendo validos`() {
        val old = LauncherSettings.fromJson("""{"homeApps":["a"],"waitSeconds":15,"categoryOverrides":{"a":"Juegos"}}""")
        assertEquals(listOf("a"), old.homeApps)
        assertEquals(LauncherSettings().categories, old.categories)
        assertEquals(56, old.iconSizeDp)
        assertNull(old.iconPack)
    }
}
