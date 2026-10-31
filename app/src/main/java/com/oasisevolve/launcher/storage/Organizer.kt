package com.oasisevolve.launcher.storage

import com.oasisevolve.launcher.AppList
import com.oasisevolve.launcher.library.DEFAULT_CATEGORIES

/** Operaciones puras de organización. Sin Android, para poder probarlas en la JVM. */

fun LauncherSettings.list(list: AppList): List<String> = when (list) {
    AppList.HOME -> homeApps
    AppList.DOCK -> dockApps
    AppList.USEFUL -> usefulApps
    AppList.WANT_MORE -> wantMoreApps
}

fun LauncherSettings.withList(list: AppList, keys: List<String>): LauncherSettings = when (list) {
    AppList.HOME -> copy(homeApps = keys)
    AppList.DOCK -> copy(dockApps = keys)
    AppList.USEFUL -> copy(usefulApps = keys)
    AppList.WANT_MORE -> copy(wantMoreApps = keys)
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

/** Listas donde una carpeta puede vivir. El dock se mantiene sin carpetas. */
private val FOLDER_LISTS = listOf(AppList.HOME, AppList.USEFUL, AppList.WANT_MORE)

/** Crea una carpeta en el lugar que ocupa la app, o en Apps útiles si aún no está colocada. */
fun LauncherSettings.createFolder(appKey: String, name: String, id: Long): LauncherSettings {
    val folder = AppFolder(id, name, listOf(appKey))
    val owner = FOLDER_LISTS.firstOrNull { appKey in list(it) }
    val cleaned = copy(folders = folders.map { it.copy(apps = it.apps - appKey) } + folder)
        .dropEmptyFolders(keep = id)
    return if (owner != null) {
        val keys = list(owner).toMutableList()
        keys[keys.indexOf(appKey)] = folder.entryKey
        cleaned.withList(owner, keys).removeFromLists(appKey)
    } else {
        cleaned.withList(AppList.USEFUL, usefulApps + folder.entryKey)
    }
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
    ).dropEmptyFolders(keep = folderId).removeFromLists(appKey)

/** La app vuelve a la cuadrícula, justo después de su carpeta. Una carpeta vacía desaparece. */
fun LauncherSettings.removeFromFolder(folderId: Long, appKey: String): LauncherSettings {
    val folder = folders.firstOrNull { it.id == folderId } ?: return this
    val owner = FOLDER_LISTS.firstOrNull { folder.entryKey in list(it) } ?: AppList.USEFUL
    val keys = list(owner).toMutableList()
    val index = keys.indexOf(folder.entryKey)
    if (index >= 0) keys.add(index + 1, appKey) else keys.add(appKey)
    return withList(owner, keys)
        .copy(folders = folders.map { if (it.id == folderId) it.copy(apps = it.apps - appKey) else it })
        .dropEmptyFolders(keep = null)
}

fun LauncherSettings.renameFolder(folderId: Long, name: String): LauncherSettings =
    copy(folders = folders.map { if (it.id == folderId) it.copy(name = name) else it })

/** Elimina la carpeta y devuelve sus apps a la cuadrícula, en su sitio. Nunca pierde apps. */
fun LauncherSettings.deleteFolder(folderId: Long): LauncherSettings {
    val folder = folders.firstOrNull { it.id == folderId } ?: return this
    val owner = FOLDER_LISTS.firstOrNull { folder.entryKey in list(it) } ?: AppList.USEFUL
    val keys = list(owner).flatMap { if (it == folder.entryKey) folder.apps else listOf(it) }
    return withList(owner, keys).copy(folders = folders - folder)
}

private fun LauncherSettings.removeFromLists(appKey: String): LauncherSettings =
    FOLDER_LISTS.fold(this) { settings, list ->
        settings.withList(list, settings.list(list) - appKey)
    }

private fun LauncherSettings.dropEmptyFolders(keep: Long?): LauncherSettings {
    val empty = folders.filter { it.apps.isEmpty() && it.id != keep }
    if (empty.isEmpty()) return this
    val keys = empty.map { it.entryKey }.toSet()
    return FOLDER_LISTS.fold(copy(folders = folders - empty.toSet())) { settings, list ->
        settings.withList(list, settings.list(list).filterNot { it in keys })
    }
}

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
