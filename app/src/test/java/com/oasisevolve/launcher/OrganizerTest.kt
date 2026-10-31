package com.oasisevolve.launcher

import com.oasisevolve.launcher.storage.Accent
import com.oasisevolve.launcher.storage.AppFolder
import com.oasisevolve.launcher.storage.AppFont
import com.oasisevolve.launcher.storage.GridTarget
import com.oasisevolve.launcher.storage.HomeItem
import com.oasisevolve.launcher.storage.HomeItemKind
import com.oasisevolve.launcher.storage.IconShape
import com.oasisevolve.launcher.storage.InstalledApp
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.ThemeMode
import com.oasisevolve.launcher.storage.WidgetPhoto
import com.oasisevolve.launcher.storage.addApp
import com.oasisevolve.launcher.storage.addCategory
import com.oasisevolve.launcher.storage.addToFolder
import com.oasisevolve.launcher.storage.appItem
import com.oasisevolve.launcher.storage.applyVisibleOrder
import com.oasisevolve.launcher.storage.canPlace
import com.oasisevolve.launcher.storage.createFolder
import com.oasisevolve.launcher.storage.deleteCategory
import com.oasisevolve.launcher.storage.deleteFolder
import com.oasisevolve.launcher.storage.effectiveRows
import com.oasisevolve.launcher.storage.firstFree
import com.oasisevolve.launcher.storage.folderItem
import com.oasisevolve.launcher.storage.isBuiltInCategory
import com.oasisevolve.launcher.storage.itemAt
import com.oasisevolve.launcher.storage.mergeIntoNewFolder
import com.oasisevolve.launcher.storage.placeInDock
import com.oasisevolve.launcher.storage.removeFromFolder
import com.oasisevolve.launcher.storage.renameCategory
import com.oasisevolve.launcher.storage.replaceInDock
import com.oasisevolve.launcher.storage.resizeItem
import com.oasisevolve.launcher.storage.setFolderOrder
import com.oasisevolve.launcher.storage.toggleCategoryHidden
import com.oasisevolve.launcher.storage.widgetItem
import com.oasisevolve.launcher.storage.withDefaultComposition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizerTest {
    private fun home(vararg items: HomeItem) = LauncherSettings(homeLayout = items.toList())

    private fun LauncherSettings.homeKeys() = homeLayout.map { it.id }

    @Test
    fun `no se puede colocar fuera de la cuadricula ni encima de otro`() {
        val items = listOf(widgetItem(1, 0, 0, 2, 2))
        assertFalse(canPlace(items, 3, 0, 2, 1, 4, 7))
        assertFalse(canPlace(items, 0, 6, 1, 2, 4, 7))
        assertFalse(canPlace(items, 1, 1, 1, 1, 4, 7))
        assertTrue(canPlace(items, 2, 0, 2, 2, 4, 7))
    }

    @Test
    fun `un widget respeta su tamano al moverse y se ignora a si mismo`() {
        val items = listOf(widgetItem(1, 0, 0, 2, 2), appItem("a", 3, 1))
        assertTrue(canPlace(items, 1, 0, 2, 2, 4, 7, ignoreId = "widget:1"))
        assertFalse(canPlace(items, 2, 0, 2, 2, 4, 7, ignoreId = "widget:1"))
    }

    @Test
    fun `sin hueco se abre una fila nueva y nada se pierde`() {
        val full = (0 until 2).flatMap { r -> (0 until 2).map { c -> appItem("$c$r", c, r) } }
        assertEquals(0 to 2, firstFree(full, 1, 1, 2, 2))
        assertEquals(3, effectiveRows(full + appItem("x", 0, 2), 2))
    }

    @Test
    fun `encontrar el elemento de una celda cubre todo su rectangulo`() {
        val items = listOf(widgetItem(1, 1, 1, 2, 2))
        assertEquals("widget:1", itemAt(items, 2, 2)?.id)
        assertNull(itemAt(items, 0, 0))
        assertNull(itemAt(items, 2, 2, ignoreId = "widget:1"))
    }

    @Test
    fun `redimensionar no pisa a otros ni sale de la cuadricula`() {
        val items = listOf(widgetItem(1, 0, 0, 2, 2), appItem("a", 3, 0))
        assertEquals(3, resizeItem(items, "widget:1", 3, 2, 4, 7).first().w)
        assertEquals(2, resizeItem(items, "widget:1", 4, 2, 4, 7).first().w)
        assertEquals(1, resizeItem(items, "widget:1", 1, 1, 4, 7).first().h)
    }

    @Test
    fun `reordenar conserva la posicion de lo que no se muestra`() {
        val result = applyVisibleOrder(listOf("a", "oculta", "b", "c"), listOf("c", "a", "b"))
        assertEquals(listOf("c", "oculta", "a", "b"), result)
    }

    @Test
    fun `crear carpeta la coloca donde estaba la app`() {
        val s = home(appItem("a", 0, 0), appItem("b", 1, 2)).createFolder("b", "Trabajo", 1)
        assertEquals(setOf("app:a", "folder:1"), s.homeKeys().toSet())
        val folder = s.homeLayout.first { it.kind == HomeItemKind.FOLDER }
        assertEquals(1 to 2, folder.col to folder.row)
    }

    @Test
    fun `crear carpeta de una app sin colocar la lleva a utiles`() {
        val s = LauncherSettings().createFolder("x", "Nueva", 2)
        assertEquals(listOf("folder:2"), s.usefulLayout.map { it.id })
    }

    @Test
    fun `soltar una app sobre otra crea una carpeta en la celda de destino`() {
        val s = home(appItem("a", 0, 0), appItem("b", 2, 3))
            .mergeIntoNewFolder(GridTarget.HOME, "a", "b", "Carpeta", 5)
        assertEquals(listOf("folder:5"), s.homeKeys())
        assertEquals(2 to 3, s.homeLayout.single().col to s.homeLayout.single().row)
        assertEquals(listOf("b", "a"), s.folders.single().apps)
    }

    @Test
    fun `soltar una app del dock sobre otra la saca del dock`() {
        val s = home(appItem("b", 0, 0)).copy(dockApps = listOf("a", "c"))
            .mergeIntoNewFolder(GridTarget.HOME, "a", "b", "Carpeta", 5)
        assertEquals(listOf("c"), s.dockApps)
    }

    @Test
    fun `una app solo vive en una carpeta`() {
        val s = home(folderItem(1, 0, 0), folderItem(2, 1, 0), appItem("z", 2, 0)).copy(
            folders = listOf(AppFolder(1, "A", listOf("x", "y")), AppFolder(2, "B", listOf("w"))),
        ).addToFolder(2, "x").addToFolder(2, "z")
        assertEquals(listOf("y"), s.folders.first { it.id == 1L }.apps)
        assertEquals(listOf("w", "x", "z"), s.folders.first { it.id == 2L }.apps)
        assertEquals(listOf("folder:1", "folder:2"), s.homeKeys())
    }

    @Test
    fun `mover la ultima app de una carpeta elimina la carpeta vacia`() {
        val s = home(folderItem(1, 0, 0), folderItem(2, 1, 0)).copy(
            folders = listOf(AppFolder(1, "A", listOf("x")), AppFolder(2, "B", listOf("w"))),
        ).addToFolder(2, "x")
        assertEquals(listOf("folder:2"), s.homeKeys())
        assertEquals(1, s.folders.size)
    }

    @Test
    fun `sacar una app la devuelve a la cuadricula de la carpeta`() {
        val s = home(folderItem(1, 0, 0)).copy(folders = listOf(AppFolder(1, "A", listOf("x", "y"))))
            .removeFromFolder(1, "x")
        assertTrue("app:x" in s.homeKeys())
        assertEquals(listOf("y"), s.folders.single().apps)
    }

    @Test
    fun `sacar la ultima app elimina la carpeta`() {
        val s = LauncherSettings(
            usefulLayout = listOf(folderItem(1, 0, 0)),
            folders = listOf(AppFolder(1, "A", listOf("x"))),
        ).removeFromFolder(1, "x")
        assertEquals(listOf("app:x"), s.usefulLayout.map { it.id })
        assertTrue(s.folders.isEmpty())
    }

    @Test
    fun `borrar una carpeta nunca pierde sus apps`() {
        val s = home(appItem("a", 0, 0), folderItem(1, 1, 0)).copy(
            folders = listOf(AppFolder(1, "A", listOf("x", "y"))),
        ).deleteFolder(1)
        assertEquals(setOf("app:a", "app:x", "app:y"), s.homeKeys().toSet())
        assertEquals(1 to 0, s.homeLayout.first { it.ref == "x" }.let { it.col to it.row })
        assertTrue(s.folders.isEmpty())
    }

    @Test
    fun `las apps de una carpeta se pueden reordenar`() {
        val s = LauncherSettings(folders = listOf(AppFolder(1, "A", listOf("x", "y", "z"))))
            .setFolderOrder(1, listOf("z", "x", "y"))
        assertEquals(listOf("z", "x", "y"), s.folders.single().apps)
    }

    @Test
    fun `el dock admite colocar reemplazar y no duplica`() {
        val s = LauncherSettings(dockApps = listOf("a", "b", "c", "d"))
        assertEquals(listOf("x", "a", "b", "c", "d"), s.placeInDock("x", 0).dockApps)
        assertEquals(listOf("a", "c", "b", "d"), s.placeInDock("c", 1).dockApps)
        assertEquals(listOf("a", "x", "c", "d"), s.replaceInDock("b", "x").dockApps)
        val full = LauncherSettings(dockApps = listOf("a", "b", "c", "d", "e"))
        assertEquals(listOf("a", "x", "c", "d", "e"), full.placeInDock("x", 1).dockApps)
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
    fun `alternar ocultar una categoria es reversible`() {
        val s = LauncherSettings()
        assertEquals(s, s.toggleCategoryHidden("Juegos").toggleCategoryHidden("Juegos"))
    }

    private val phoneApps = listOf(
        InstalledApp("dial", "Teléfono", "com.transsion.dialer"),
        InstalledApp("wa", "WhatsApp", "com.whatsapp"),
        InstalledApp("sp", "Spotify", "com.spotify.music"),
        InstalledApp("cam", "Cámara", "com.transsion.camera"),
        InstalledApp("dr", "Drive", "com.google.android.apps.docs"),
        InstalledApp("gm", "Gmail", "com.google.android.gm"),
        InstalledApp("no", "Notion", "notion.id"),
        InstalledApp("bnb", "BNB Móvil", "bo.bnb"),
        InstalledApp("gpt", "ChatGPT", "com.openai.chatgpt"),
        InstalledApp("cl", "Claude", "com.anthropic.claude"),
        InstalledApp("tt", "TikTok", "com.zhiliaoapp.musically"),
    )

    @Test
    fun `la composicion inicial sigue la referencia y su dock`() {
        val s = LauncherSettings().withDefaultComposition(phoneApps, "com.transsion.dialer", "com.transsion.camera")
        assertEquals(listOf("dial", "wa", "sp", "cam"), s.dockApps)
        assertEquals(HomeItemKind.CLOCK, itemAt(s.homeLayout, 0, 0)?.kind)
        assertEquals(HomeItemKind.PHOTO, itemAt(s.homeLayout, 3, 1)?.kind)
        assertEquals(HomeItemKind.SCREEN_TIME, itemAt(s.homeLayout, 1, 3)?.kind)
        assertEquals(HomeItemKind.SEARCH, itemAt(s.homeLayout, 3, 4)?.kind)
        assertEquals("dr", itemAt(s.homeLayout, 2, 2)?.ref)
        assertEquals("no", itemAt(s.homeLayout, 0, 5)?.ref)
        assertEquals("bnb", itemAt(s.homeLayout, 3, 5)?.ref)
        assertEquals(listOf("gpt", "cl"), s.folders.single().apps)
        assertEquals(HomeItemKind.FOLDER, itemAt(s.homeLayout, 1, 5)?.kind)
        assertTrue(s.layoutSeeded)
        assertFalse(s.showLabels)
        // TikTok no forma parte de la referencia: no se coloca sola.
        assertTrue(s.homeLayout.none { it.ref == "tt" })
    }

    @Test
    fun `las apps ausentes dejan el hueco libre y no se inventan`() {
        val s = LauncherSettings().withDefaultComposition(emptyList(), null, null)
        assertEquals(4, s.homeLayout.size)
        assertTrue(s.dockApps.isEmpty())
    }

    @Test
    fun `la composicion conserva lo que ya estaba colocado sin solapar`() {
        val before = home(appItem("tt", 0, 0), appItem("dr", 1, 0), widgetItem(9, 0, 1, 4, 2))
        val s = before.withDefaultComposition(phoneApps, null, null)
        assertTrue(s.homeLayout.any { it.ref == "tt" })
        assertEquals(1, s.homeLayout.count { it.ref == "dr" })
        assertNotNull(s.homeLayout.firstOrNull { it.kind == HomeItemKind.WIDGET })
        val cells = s.homeLayout.flatMap { item ->
            (item.col until item.col + item.w).flatMap { c -> (item.row until item.row + item.h).map { r -> c to r } }
        }
        assertEquals(cells.size, cells.toSet().size)
    }

    @Test
    fun `todos los ajustes sobreviven a un ciclo de guardado`() {
        val original = LauncherSettings(
            homeLayout = listOf(appItem("a", 1, 2), widgetItem(4, 0, 3, 4, 2), folderItem(3, 0, 0)),
            usefulLayout = listOf(appItem("b", 0, 0)),
            layoutSeeded = true,
            dockApps = listOf("a"),
            folders = listOf(AppFolder(3, "Estudio", listOf("a", "b"))),
            photos = listOf(WidgetPhoto("p.jpg", 123L), WidgetPhoto("q.jpg", null)),
            categories = listOf("Ocio", "Robótica"),
            hiddenCategories = setOf("Ocio"),
            categoryRenames = mapOf("Juegos" to "Ocio"),
            widgetHeights = mapOf(4 to 180),
            gridRows = 8,
            iconSizeDp = 64,
            gridSpacingDp = 20,
            pageMarginDp = 32,
            libraryExpanded = true,
            glassBlur = true,
            immersiveMode = true,
            weekStartIso = 7,
            academicSemester = true,
            academicStartDay = 20_000,
            academicEndDay = 20_100,
            themeMode = ThemeMode.LIGHT,
            accent = Accent.VIOLET,
            font = AppFont.OUTFIT,
            fontScale = 1.1f,
            iconShape = IconShape.CIRCLE,
            iconPack = "com.pack",
        )
        assertEquals(original, LauncherSettings.fromJson(original.toJson()))
    }

    @Test
    fun `los ajustes de la version 0_2 se migran sin perder nada`() {
        val old = LauncherSettings.fromJson(
            """{"homeApps":["a","folder:7","b"],"usefulApps":["c"],"wantMoreApps":["d","e"],
               "homeWidgetIds":[9],"dockApps":["z"],"waitSeconds":15,"themeMode":"LIGHT",
               "folders":[{"id":7,"name":"F","apps":["x"]}],
               "accessLevels":{"a":4},"categoryOverrides":{"a":"Juegos"}}""",
        )
        assertEquals(setOf("widget:9", "app:a", "folder:7", "app:b"), old.homeLayout.map { it.id }.toSet())
        assertTrue(old.usefulLayout.any { it.ref == "c" })
        val wantMore = old.folders.first { it.name == "Quiero usar más" }
        assertEquals(listOf("d", "e"), wantMore.apps)
        assertEquals(listOf("z"), old.dockApps)
        assertEquals(15, old.waitSeconds)
        assertEquals(4, old.accessLevels["a"])
        assertEquals(ThemeMode.LIGHT, old.themeMode)
        assertFalse(old.layoutSeeded)
    }

    @Test
    fun `quien venia de la 0_2 con tema del sistema lo conserva`() {
        assertEquals(ThemeMode.SYSTEM, LauncherSettings.fromJson("""{"homeApps":[]}""").themeMode)
        assertEquals(ThemeMode.DARK, LauncherSettings().themeMode)
    }

    @Test
    fun `anadir una app dos veces no la duplica`() {
        val s = LauncherSettings().addApp(GridTarget.HOME, "a").addApp(GridTarget.HOME, "a")
        assertEquals(1, s.homeLayout.size)
    }
}
