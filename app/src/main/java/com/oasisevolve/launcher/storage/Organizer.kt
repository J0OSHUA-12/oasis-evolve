package com.oasisevolve.launcher.storage

import com.oasisevolve.launcher.library.DEFAULT_CATEGORIES

/** Operaciones puras de organización. Sin Android, para poder probarlas en la JVM. */

const val MAX_DOCK_APPS = 5

private val TARGETS = listOf(GridTarget.HOME, GridTarget.USEFUL)

private fun LauncherSettings.rows(target: GridTarget) = effectiveRows(layout(target), gridRows)

fun LauncherSettings.containsApp(target: GridTarget, key: String): Boolean =
    layout(target).any { it.kind == HomeItemKind.APP && it.ref == key }

fun LauncherSettings.addApp(target: GridTarget, key: String): LauncherSettings {
    if (containsApp(target, key)) return this
    val items = addItems(layout(target), gridColumns, gridRows, listOf { col, row -> appItem(key, col, row) })
    return withLayout(target, items)
}

fun LauncherSettings.removeApp(target: GridTarget, key: String): LauncherSettings =
    withLayout(target, layout(target).filterNot { it.kind == HomeItemKind.APP && it.ref == key })

fun LauncherSettings.toggleApp(target: GridTarget, key: String): LauncherSettings =
    if (containsApp(target, key)) removeApp(target, key) else addApp(target, key)

fun LauncherSettings.removeItem(target: GridTarget, id: String): LauncherSettings =
    withLayout(target, layout(target).filterNot { it.id == id })

fun LauncherSettings.moveGridItem(target: GridTarget, id: String, col: Int, row: Int): LauncherSettings =
    withLayout(target, moveItem(layout(target), id, col, row))

fun LauncherSettings.resizeGridItem(target: GridTarget, id: String, w: Int, h: Int): LauncherSettings =
    withLayout(target, resizeItem(layout(target), id, w, h, gridColumns, rows(target)))

fun LauncherSettings.addGridItem(target: GridTarget, create: (Int, Int) -> HomeItem): LauncherSettings =
    withLayout(target, addItems(layout(target), gridColumns, gridRows, listOf(create)))

private fun LauncherSettings.removeAppFromGrids(key: String): LauncherSettings =
    TARGETS.fold(this) { settings, target -> settings.removeApp(target, key) }

private fun LauncherSettings.folderOwner(folderId: Long): GridTarget =
    TARGETS.firstOrNull { target -> layout(target).any { it.kind == HomeItemKind.FOLDER && it.ref == folderId.toString() } }
        ?: GridTarget.USEFUL

/** Crea una carpeta en el lugar que ocupa la app, o en Apps útiles si aún no está colocada. */
fun LauncherSettings.createFolder(appKey: String, name: String, id: Long): LauncherSettings {
    val folder = AppFolder(id, name, listOf(appKey))
    val owner = TARGETS.firstOrNull { containsApp(it, appKey) }
    val withFolder = copy(folders = folders.map { it.copy(apps = it.apps - appKey) } + folder)
        .dropEmptyFolders(keep = id)
    val cell = owner?.let { target -> layout(target).first { it.kind == HomeItemKind.APP && it.ref == appKey } }
    val cleaned = withFolder.removeAppFromGrids(appKey)
    return if (owner != null && cell != null) {
        cleaned.withLayout(owner, cleaned.layout(owner) + folderItem(id, cell.col, cell.row))
    } else {
        cleaned.addGridItem(GridTarget.USEFUL) { col, row -> folderItem(id, col, row) }
    }
}

/**
 * Soltar una app sobre otra: la carpeta ocupa la celda de la app de destino. La app arrastrada puede
 * venir de la cuadrícula o del dock, y sale de allí.
 */
fun LauncherSettings.mergeIntoNewFolder(
    target: GridTarget,
    draggedKey: String,
    targetKey: String,
    name: String,
    id: Long,
): LauncherSettings {
    val cell = layout(target).firstOrNull { it.kind == HomeItemKind.APP && it.ref == targetKey } ?: return this
    val folder = AppFolder(id, name, listOf(targetKey, draggedKey))
    val cleaned = copy(
        folders = folders.map { it.copy(apps = it.apps - targetKey - draggedKey) } + folder,
        dockApps = dockApps - draggedKey,
    ).dropEmptyFolders(keep = id).removeAppFromGrids(targetKey).removeAppFromGrids(draggedKey)
    return cleaned.withLayout(target, cleaned.layout(target) + folderItem(id, cell.col, cell.row))
}

/** Una app vive en una sola carpeta: al añadirla sale de las cuadrículas y de otras carpetas. */
fun LauncherSettings.addToFolder(folderId: Long, appKey: String): LauncherSettings =
    copy(
        folders = folders.map { folder ->
            when {
                folder.id == folderId -> folder.copy(apps = (folder.apps - appKey) + appKey)
                else -> folder.copy(apps = folder.apps - appKey)
            }
        },
    ).dropEmptyFolders(keep = folderId).removeAppFromGrids(appKey)

/** La app vuelve a la cuadrícula de la carpeta. Una carpeta vacía desaparece. */
fun LauncherSettings.removeFromFolder(folderId: Long, appKey: String): LauncherSettings {
    if (folders.none { it.id == folderId }) return this
    val owner = folderOwner(folderId)
    return copy(folders = folders.map { if (it.id == folderId) it.copy(apps = it.apps - appKey) else it })
        .dropEmptyFolders(keep = null)
        .addApp(owner, appKey)
}

fun LauncherSettings.renameFolder(folderId: Long, name: String): LauncherSettings =
    copy(folders = folders.map { if (it.id == folderId) it.copy(name = name) else it })

fun LauncherSettings.setFolderOrder(folderId: Long, order: List<String>): LauncherSettings =
    copy(
        folders = folders.map { folder ->
            if (folder.id != folderId) {
                folder
            } else {
                folder.copy(apps = applyVisibleOrder(folder.apps, order))
            }
        },
    )

/** Elimina la carpeta y devuelve sus apps a la cuadrícula, la primera en su celda. Nunca pierde apps. */
fun LauncherSettings.deleteFolder(folderId: Long): LauncherSettings {
    val folder = folders.firstOrNull { it.id == folderId } ?: return this
    val owner = folderOwner(folderId)
    val items = layout(owner)
    val cell = items.firstOrNull { it.kind == HomeItemKind.FOLDER && it.ref == folderId.toString() }
    val remaining = items.filterNot { it === cell }
    val placed = if (cell != null && folder.apps.isNotEmpty()) {
        remaining + appItem(folder.apps.first(), cell.col, cell.row)
    } else {
        remaining
    }
    val rest = if (cell != null) folder.apps.drop(1) else folder.apps
    return withLayout(owner, placed).copy(folders = folders - folder).let { settings ->
        rest.fold(settings) { acc, key -> acc.addApp(owner, key) }
    }
}

private fun LauncherSettings.dropEmptyFolders(keep: Long?): LauncherSettings {
    val empty = folders.filter { it.apps.isEmpty() && it.id != keep }
    if (empty.isEmpty()) return this
    val refs = empty.map { it.id.toString() }.toSet()
    return TARGETS.fold(copy(folders = folders - empty.toSet())) { settings, target ->
        settings.withLayout(
            target,
            settings.layout(target).filterNot { it.kind == HomeItemKind.FOLDER && it.ref in refs },
        )
    }
}

/**
 * Aplica un nuevo orden de lo visible sin perder la posición de lo que no se muestra
 * (apps desinstaladas temporalmente u ocultas por su nivel).
 */
fun applyVisibleOrder(full: List<String>, visibleOrder: List<String>): List<String> {
    val visible = visibleOrder.toSet()
    val next = visibleOrder.iterator()
    return full.map { key -> if (key in visible && next.hasNext()) next.next() else key }
}

/** Coloca una app en un hueco del dock. Si está lleno, sustituye a la que ocupa esa posición. */
fun LauncherSettings.placeInDock(key: String, index: Int): LauncherSettings {
    val without = (dockApps - key).toMutableList()
    val position = index.coerceIn(0, without.size)
    if (without.size >= MAX_DOCK_APPS) {
        without[position.coerceAtMost(without.lastIndex)] = key
    } else {
        without.add(position, key)
    }
    return copy(dockApps = without)
}

fun LauncherSettings.replaceInDock(old: String, new: String): LauncherSettings =
    copy(dockApps = (dockApps - new).map { if (it == old) new else it })

/** Devuelve `null` si el nombre está vacío o ya existe. */
fun LauncherSettings.addCategory(name: String): LauncherSettings? {
    val clean = name.trim()
    if (clean.isEmpty() || categories.any { it.equals(clean, ignoreCase = true) }) return null
    return copy(categories = categories + clean)
}

/** Devuelve `null` si el nuevo nombre está vacío o ya lo usa otra categoría. */
fun LauncherSettings.renameCategory(old: String, new: String): LauncherSettings? {
    val clean = new.trim()
    if (clean.isEmpty()) return null
    if (clean == old) return this
    if (categories.any { it != old && it.equals(clean, ignoreCase = true) }) return null
    val renames = categoryRenames.mapValues { if (it.value == old) clean else it.value }
    return copy(
        categories = categories.map { if (it == old) clean else it },
        hiddenCategories = hiddenCategories.map { if (it == old) clean else it }.toSet(),
        categoryOverrides = categoryOverrides.mapValues { if (it.value == old) clean else it.value },
        categoryRenames = if (old in DEFAULT_CATEGORIES) renames + (old to clean) else renames,
    )
}

/** Solo se borran las categorías propias: sus apps vuelven a su categoría automática. */
fun LauncherSettings.deleteCategory(name: String): LauncherSettings {
    if (isBuiltInCategory(name)) return this
    return copy(
        categories = categories - name,
        hiddenCategories = hiddenCategories - name,
        categoryOverrides = categoryOverrides.filterValues { it != name },
    )
}

fun LauncherSettings.isBuiltInCategory(name: String): Boolean =
    name in DEFAULT_CATEGORIES.map { categoryRenames[it] ?: it }

fun LauncherSettings.toggleCategoryHidden(name: String): LauncherSettings =
    copy(hiddenCategories = if (name in hiddenCategories) hiddenCategories - name else hiddenCategories + name)
