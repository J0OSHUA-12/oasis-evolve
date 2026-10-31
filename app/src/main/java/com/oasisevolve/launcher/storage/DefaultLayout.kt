package com.oasisevolve.launcher.storage

import com.oasisevolve.launcher.drawer.normalizeForSearch

/** Lo mínimo de una app instalada para decidir la composición inicial. */
data class InstalledApp(
    val key: String,
    val label: String,
    val packageName: String,
    val activityName: String = "",
)

/** Actividad del sistema que atiende una acción, como marcar o abrir la cámara. */
data class IntentHandler(val packageName: String, val activityName: String)

/**
 * Icono abrible que corresponde a quien atiende una acción. Un paquete puede tener varios: en HiOS,
 * Teléfono y Contactos son el mismo. Por eso se busca primero la actividad exacta.
 */
fun appForHandlers(apps: List<InstalledApp>, handlers: List<IntentHandler>): InstalledApp? {
    handlers.forEach { handler ->
        apps.firstOrNull { it.packageName == handler.packageName && it.activityName == handler.activityName }
            ?.let { return it }
    }
    return handlers.firstNotNullOfOrNull { handler -> apps.firstOrNull { it.packageName == handler.packageName } }
}

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
 * que falte deja su hueco libre. Lo que el usuario ya tenía colocado se conserva: va a los huecos
 * restantes y, si no cabe en la pantalla, a Apps útiles o al Widget Center. Inicio no se desplaza por ello.
 */
fun LauncherSettings.withDefaultComposition(
    apps: List<InstalledApp>,
    dialerKey: String?,
    cameraKey: String?,
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
    val hasAiFolder = newFolders.any { it.id == AI_FOLDER_ID }
    if (!hasAiFolder && aiApps.size >= 2) newFolders += AppFolder(AI_FOLDER_ID, "IA", aiApps)
    if (hasAiFolder || aiApps.size >= 2) items += folderItem(AI_FOLDER_ID, AI_FOLDER_CELL.first, AI_FOLDER_CELL.second)

    val dockKeys = listOfNotNull(
        dialerKey,
        apps.firstOrNull { it.packageName == WHATSAPP_PACKAGE }?.key,
        apps.firstOrNull { it.packageName == SPOTIFY_PACKAGE }?.key,
        cameraKey,
    )
    val dock = if (dockKeys.isNotEmpty()) dockKeys else dockApps

    // Nada de lo que ya estaba en la pantalla se pierde. Una app que ya está en el dock no se repite.
    val placedApps = items.filter { it.kind == HomeItemKind.APP }.map { it.ref }.toSet() + aiApps + dock
    val placedIds = items.map { it.id }.toSet()
    val kept = previous.filterNot { (it.kind == HomeItemKind.APP && it.ref in placedApps) || it.id in placedIds }
    val placedOnHome = items.toMutableList()
    var useful = usefulLayout.filterNot { it.id in placedIds }
    val center = widgetIds.toMutableList()
    kept.forEach { item ->
        val (col, row) = firstFree(placedOnHome, item.w, item.h, gridColumns, gridRows)
        when {
            row + item.h <= gridRows -> placedOnHome += item.copy(col = col, row = row)
            item.kind == HomeItemKind.WIDGET -> item.ref.toIntOrNull()?.let { center += it }
            else -> useful = addItems(useful, gridColumns, gridRows, listOf { c, r -> item.copy(col = c, row = r) })
        }
    }

    return copy(
        homeLayout = placedOnHome,
        usefulLayout = useful,
        widgetIds = center,
        folders = newFolders,
        dockApps = dock,
        showLabels = false,
        // Las referencias son oscuras. Un tema elegido a mano se respeta.
        themeMode = if (themeMode == ThemeMode.SYSTEM) ThemeMode.DARK else themeMode,
        layoutSeeded = true,
    )
}
