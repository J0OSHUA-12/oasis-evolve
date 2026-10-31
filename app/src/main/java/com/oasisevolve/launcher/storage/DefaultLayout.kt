package com.oasisevolve.launcher.storage

import com.oasisevolve.launcher.drawer.normalizeForSearch

/** Lo mínimo de una app instalada para decidir la composición inicial. */
data class InstalledApp(val key: String, val label: String, val packageName: String)

const val WHATSAPP_PACKAGE = "com.whatsapp"
const val SPOTIFY_PACKAGE = "com.spotify.music"

private data class Slot(val col: Int, val row: Int, val packages: List<String>, val labels: List<String>)

private val REFERENCE_SLOTS = listOf(
    Slot(2, 2, listOf("com.google.android.apps.docs"), listOf("drive")),
    Slot(3, 3, listOf("com.google.android.gm"), listOf("gmail")),
    Slot(0, 5, listOf("notion.id"), listOf("notion")),
    Slot(2, 5, listOf("com.google.android.apps.classroom"), listOf("classroom")),
    Slot(3, 5, emptyList(), listOf("bnb")),
    Slot(0, 6, listOf("com.brave.browser"), listOf("brave")),
    Slot(1, 6, emptyList(), listOf("unimovil", "uni movil")),
    Slot(2, 6, listOf("md.obsidian"), listOf("obsidian")),
)

private const val AI_FOLDER_ID = 2L
private val AI_FOLDER_CELL = 1 to 5
private val AI_APPS = listOf("chatgpt", "claude", "gemini", "perplexity", "copilot", "grok", "deepseek")

private fun List<InstalledApp>.findBy(packages: List<String>, labels: List<String>): InstalledApp? =
    firstOrNull { it.packageName in packages }
        ?: firstOrNull { app ->
            val label = normalizeForSearch(app.label)
            labels.any { it in label }
        }

/**
 * Composición de referencia: reloj y fotos arriba, tiempo de pantalla y apps en medio, búsqueda y
 * más apps abajo, y dock con Teléfono, WhatsApp, Spotify y Cámara. Solo usa apps que existan: una
 * que falte deja su hueco libre. Lo que el usuario ya tenía colocado se conserva en los huecos restantes.
 */
fun LauncherSettings.withDefaultComposition(
    apps: List<InstalledApp>,
    dialerPackage: String?,
    cameraPackage: String?,
): LauncherSettings {
    val previous = homeLayout
    val items = mutableListOf(
        HomeItem("clock:default", HomeItemKind.CLOCK, col = 0, row = 0, w = 2, h = 2),
        HomeItem("photo:default", HomeItemKind.PHOTO, col = 2, row = 0, w = 2, h = 2),
        HomeItem("screentime:default", HomeItemKind.SCREEN_TIME, col = 0, row = 2, w = 2, h = 2),
        HomeItem("search:default", HomeItemKind.SEARCH, col = 0, row = 4, w = 4, h = 1),
    )

    val alreadyInFolders = folders.flatMap { it.apps }.toSet()
    REFERENCE_SLOTS.forEach { slot ->
        val app = apps.findBy(slot.packages, slot.labels)
        if (app != null && app.key !in alreadyInFolders) items += appItem(app.key, slot.col, slot.row)
    }

    val aiApps = AI_APPS
        .mapNotNull { name -> apps.firstOrNull { normalizeForSearch(it.label).contains(name) } }
        .filter { it.key !in alreadyInFolders }
        .map { it.key }
    val newFolders = folders.toMutableList()
    if (aiApps.size >= 2 && newFolders.none { it.id == AI_FOLDER_ID }) {
        newFolders += AppFolder(AI_FOLDER_ID, "IA", aiApps)
        items += folderItem(AI_FOLDER_ID, AI_FOLDER_CELL.first, AI_FOLDER_CELL.second)
    }

    // Nada de lo que ya estaba en la pantalla se pierde.
    val placedApps = items.filter { it.kind == HomeItemKind.APP }.map { it.ref }.toSet() + aiApps
    val placedIds = items.map { it.id }.toSet()
    val kept = previous.filterNot { (it.kind == HomeItemKind.APP && it.ref in placedApps) || it.id in placedIds }
    val merged = kept.fold(items.toList()) { acc, item ->
        val (col, row) = firstFree(acc, item.w, item.h, gridColumns, gridRows)
        acc + item.copy(col = col, row = row)
    }

    val dockPackages = listOfNotNull(dialerPackage, WHATSAPP_PACKAGE, SPOTIFY_PACKAGE, cameraPackage)
    val dock = dockPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg }?.key }

    return copy(
        homeLayout = merged,
        folders = newFolders,
        dockApps = if (dock.isNotEmpty()) dock else dockApps,
        showLabels = false,
        // Las referencias son oscuras. Un tema elegido a mano se respeta.
        themeMode = if (themeMode == ThemeMode.SYSTEM) ThemeMode.DARK else themeMode,
        layoutSeeded = true,
    )
}
