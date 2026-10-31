package com.oasisevolve.launcher

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oasisevolve.launcher.focus.AccessLevel
import com.oasisevolve.launcher.home.DragPayload
import com.oasisevolve.launcher.home.DropTarget
import com.oasisevolve.launcher.library.autoCategory
import com.oasisevolve.launcher.storage.AppFolder
import com.oasisevolve.launcher.storage.GridTarget
import com.oasisevolve.launcher.storage.HomeItem
import com.oasisevolve.launcher.storage.HomeItemKind
import com.oasisevolve.launcher.storage.InstalledApp
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.Note
import com.oasisevolve.launcher.storage.addGridItem
import com.oasisevolve.launcher.storage.addToFolder
import com.oasisevolve.launcher.storage.appItem
import com.oasisevolve.launcher.storage.containsApp
import com.oasisevolve.launcher.storage.createFolder
import com.oasisevolve.launcher.storage.deleteFolder
import com.oasisevolve.launcher.storage.mergeIntoNewFolder
import com.oasisevolve.launcher.storage.moveGridItem
import com.oasisevolve.launcher.storage.placeInDock
import com.oasisevolve.launcher.storage.removeApp
import com.oasisevolve.launcher.storage.removeFromFolder
import com.oasisevolve.launcher.storage.removeItem
import com.oasisevolve.launcher.storage.renameFolder
import com.oasisevolve.launcher.storage.replaceInDock
import com.oasisevolve.launcher.storage.resizeGridItem
import com.oasisevolve.launcher.storage.setFolderOrder
import com.oasisevolve.launcher.storage.toggleApp
import com.oasisevolve.launcher.storage.withDefaultComposition
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.system.IconPackInfo
import com.oasisevolve.launcher.usage.AppStats
import com.oasisevolve.launcher.usage.UsageOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Una carpeta con las apps que se pueden mostrar. */
sealed interface GridEntry {
    data class Folder(val folder: AppFolder, val apps: List<AppEntry>) : GridEntry
}

/** De dónde sale la animación al abrir una app. */
data class LaunchSource(val view: View, val bounds: Rect)

data class LauncherState(
    val settings: LauncherSettings = LauncherSettings(),
    val allApps: List<AppEntry> = emptyList(),
) {
    private val byKey = allApps.associateBy { it.key }

    fun app(key: String): AppEntry? = byKey[key]

    fun level(app: AppEntry): AccessLevel = AccessLevel.of(settings.accessLevels[app.key])

    fun category(app: AppEntry): String {
        settings.categoryOverrides[app.key]?.let { return it }
        val auto = autoCategory(app.packageName, app.appCategory)
        return settings.categoryRenames[auto] ?: auto
    }

    fun isIn(list: AppList, app: AppEntry): Boolean = when (val target = list.target) {
        null -> app.key in settings.dockApps
        else -> settings.containsApp(target, app.key)
    }

    private fun visibleApps(keys: List<String>): List<AppEntry> =
        keys.mapNotNull(byKey::get).filter { level(it).inVisualScreens }

    fun visibleDock(): List<AppEntry> = visibleApps(settings.dockApps)

    fun folder(id: Long): GridEntry.Folder? =
        settings.folders.firstOrNull { it.id == id }?.let { GridEntry.Folder(it, visibleApps(it.apps)) }

    fun folderOf(app: AppEntry): AppFolder? = settings.folders.firstOrNull { app.key in it.apps }

    /** Lo que se dibuja: una app oculta, desinstalada o una carpeta sin apps visibles deja la celda libre. */
    fun visibleItems(target: GridTarget): List<HomeItem> = settings.layout(target).filter { item ->
        when (item.kind) {
            HomeItemKind.APP -> byKey[item.ref]?.let { level(it).inVisualScreens } == true
            HomeItemKind.FOLDER -> item.ref.toLongOrNull()?.let(::folder)?.apps?.isNotEmpty() == true
            else -> true
        }
    }
}

data class GateRequest(val app: AppEntry, val stats: AppStats?)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application as EvolveApp
    private val repo = container.settings
    private val appRepo = container.apps
    private val usageRepo = container.usage
    private val notesDao = container.database.notes()
    private val photos = container.photos

    val state: StateFlow<LauncherState> = combine(repo.settings, appRepo.apps) { settings, apps ->
        LauncherState(settings, apps)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherState())

    val iconRevision: StateFlow<Int> = appRepo.iconRevision

    val notes: StateFlow<List<Note>> = notesDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _gate = MutableStateFlow<GateRequest?>(null)
    val gate: StateFlow<GateRequest?> = _gate

    private val _usage = MutableStateFlow<UsageOverview?>(null)
    val usage: StateFlow<UsageOverview?> = _usage

    private val _hasUsageAccess = MutableStateFlow(usageRepo.hasAccess())
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess

    init {
        viewModelScope.launch {
            repo.settings.map { it.iconPack }.distinctUntilChanged().collect { appRepo.setIconPack(it) }
        }
        viewModelScope.launch { seedDefaultCompositionOnce() }
    }

    /** La composición de referencia se aplica una sola vez. Después, la pantalla es del usuario. */
    private suspend fun seedDefaultCompositionOnce() {
        val apps = appRepo.apps.first { it.isNotEmpty() }
        if (repo.settings.first().layoutSeeded) return
        applyDefaultComposition(apps)
    }

    private fun applyDefaultComposition(apps: List<AppEntry>) {
        val installed = apps.map { InstalledApp(it.key, it.label, it.packageName) }
        val dialer = defaultPackageFor(Intent(Intent.ACTION_DIAL), apps)
        val camera = defaultPackageFor(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), apps)
        updateSettings { it.withDefaultComposition(installed, dialer, camera) }
    }

    fun restoreDefaultComposition() {
        applyDefaultComposition(state.value.allApps)
    }

    private fun defaultPackageFor(intent: Intent, apps: List<AppEntry>): String? {
        val pm = getApplication<Application>().packageManager
        val launchable = apps.map { it.packageName }.toSet()
        val handlers = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
        return handlers.firstOrNull { it in launchable }
    }

    suspend fun icon(app: AppEntry) = appRepo.icon(app)

    fun installedIconPacks(): List<IconPackInfo> = appRepo.installedIconPacks()

    fun open(app: AppEntry, source: LaunchSource? = null) {
        if (state.value.level(app) != AccessLevel.GUARDED) {
            appRepo.launch(app, source)
            return
        }
        viewModelScope.launch {
            val stats = if (usageRepo.hasAccess()) currentUsage().statsFor(app.packageName) else null
            _gate.value = GateRequest(app, stats)
        }
    }

    fun continueGate() {
        _gate.value?.let { appRepo.launch(it.app, null) }
        _gate.value = null
    }

    fun dismissGate() {
        _gate.value = null
    }

    fun onResume() {
        refreshUsage()
        appRepo.dropIconPackIfUninstalled()
    }

    private suspend fun currentUsage(): UsageOverview {
        _usage.value?.takeIf { System.currentTimeMillis() - it.generatedAt < USAGE_MAX_AGE_MS }?.let { return it }
        return usageRepo.overview().also { _usage.value = it }
    }

    /** Las estadísticas son costosas: se reutilizan si son recientes, salvo que se pida lo contrario. */
    fun refreshUsage(force: Boolean = false) {
        val allowed = usageRepo.hasAccess()
        _hasUsageAccess.value = allowed
        if (!allowed) {
            _usage.value = null
            return
        }
        viewModelScope.launch {
            _usage.value = if (force) usageRepo.overview() else currentUsage()
        }
    }

    fun openUsageAccess() = appRepo.openUsageAccessSettings()
    fun openAppInfo(app: AppEntry) = appRepo.openAppInfo(app)
    fun uninstall(app: AppEntry) = appRepo.uninstall(app)
    fun refreshApps() = appRepo.refresh()

    fun updateSettings(change: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { repo.update(change) }
    }

    fun toggle(list: AppList, app: AppEntry) = updateSettings { s ->
        when (val target = list.target) {
            null -> if (app.key in s.dockApps) s.copy(dockApps = s.dockApps - app.key) else s.placeInDock(app.key, s.dockApps.size)
            else -> s.toggleApp(target, app.key)
        }
    }

    fun toggleInGrid(target: GridTarget, app: AppEntry) = updateSettings { it.toggleApp(target, app.key) }

    /** Mueve una app dentro del dock. */
    fun moveInDock(app: AppEntry, delta: Int) = updateSettings { s ->
        val index = s.dockApps.indexOf(app.key)
        if (index < 0) s else s.placeInDock(app.key, (index + delta).coerceAtLeast(0))
    }

    fun removeFromDock(app: AppEntry) = updateSettings { it.copy(dockApps = it.dockApps - app.key) }

    fun replaceInDock(old: AppEntry, new: AppEntry) = updateSettings { it.replaceInDock(old.key, new.key) }

    fun applyDrop(target: GridTarget, payload: DragPayload, drop: DropTarget) = updateSettings { s ->
        val draggedKey = when (payload) {
            is DragPayload.DockApp -> payload.key
            is DragPayload.GridElement -> payload.item.ref.takeIf { payload.item.kind == HomeItemKind.APP }
        }
        when (drop) {
            is DropTarget.None -> s
            is DropTarget.Cell -> if (!drop.valid) s else s.withDropAtCell(target, payload, drop, draggedKey)
            is DropTarget.Merge -> when {
                draggedKey == null -> s
                drop.item.kind == HomeItemKind.APP ->
                    s.mergeIntoNewFolder(target, draggedKey, drop.item.ref, "Carpeta", System.currentTimeMillis())
                else -> s.copy(dockApps = s.dockApps - draggedKey)
                    .addToFolder(drop.item.ref.toLong(), draggedKey)
            }
            is DropTarget.Dock -> when {
                draggedKey == null -> s
                else -> s.removeApp(target, draggedKey).placeInDock(draggedKey, drop.index)
            }
        }
    }

    private fun LauncherSettings.withDropAtCell(
        target: GridTarget,
        payload: DragPayload,
        drop: DropTarget.Cell,
        draggedKey: String?,
    ): LauncherSettings {
        val moving = (payload as? DragPayload.GridElement)?.item
        val w = moving?.w ?: 1
        val h = moving?.h ?: 1
        // Lo que ocupa la celda sin verse (app oculta o desinstalada) cede su sitio.
        val visibleIds = LauncherState(this, state.value.allApps).visibleItems(target).map { it.id }.toSet()
        val cleaned = layout(target).filterNot { item ->
            item.id !in visibleIds && item.id != moving?.id &&
                item.col < drop.col + w && drop.col < item.col + item.w &&
                item.row < drop.row + h && drop.row < item.row + item.h
        }
        val settings = withLayout(target, cleaned)
        return when (payload) {
            is DragPayload.GridElement -> settings.moveGridItem(target, payload.item.id, drop.col, drop.row)
            is DragPayload.DockApp -> {
                val key = draggedKey ?: return settings
                val without = settings.removeApp(target, key)
                without.withLayout(target, without.layout(target) + appItem(key, drop.col, drop.row))
                    .copy(dockApps = without.dockApps - key)
            }
        }
    }

    fun resizeGridItem(target: GridTarget, id: String, w: Int, h: Int) =
        updateSettings { it.resizeGridItem(target, id, w, h) }

    fun removeFromGrid(target: GridTarget, item: HomeItem) {
        if (item.kind == HomeItemKind.WIDGET) {
            item.ref.toIntOrNull()?.let { container.widgets.remove(it, this) }
            return
        }
        // Quitar una carpeta devuelve sus apps: nunca se pierden.
        if (item.kind == HomeItemKind.FOLDER) {
            item.ref.toLongOrNull()?.let(::deleteFolder)
            return
        }
        updateSettings { it.removeItem(target, item.id) }
    }

    fun addBuiltinWidget(target: GridTarget, kind: HomeItemKind, w: Int, h: Int) = updateSettings { s ->
        s.addGridItem(target) { col, row ->
            HomeItem("${kind.name.lowercase()}:${System.currentTimeMillis()}", kind, col = col, row = row, w = w, h = h)
        }
    }

    fun createFolder(app: AppEntry, name: String) = updateSettings {
        it.createFolder(app.key, name, System.currentTimeMillis())
    }

    fun addToFolder(folderId: Long, app: AppEntry) = updateSettings { it.addToFolder(folderId, app.key) }

    fun removeFromFolder(folderId: Long, app: AppEntry) = updateSettings { it.removeFromFolder(folderId, app.key) }

    fun renameFolder(folderId: Long, name: String) = updateSettings { it.renameFolder(folderId, name) }

    fun deleteFolder(folderId: Long) = updateSettings { it.deleteFolder(folderId) }

    fun setFolderOrder(folderId: Long, order: List<String>) = updateSettings { it.setFolderOrder(folderId, order) }

    fun setLevel(app: AppEntry, level: AccessLevel) = updateSettings { s ->
        val levels = if (level == AccessLevel.VISIBLE) s.accessLevels - app.key
        else s.accessLevels + (app.key to level.level)
        s.copy(accessLevels = levels)
    }

    fun setCategory(app: AppEntry, category: String) = updateSettings { s ->
        s.copy(categoryOverrides = s.categoryOverrides + (app.key to category))
    }

    fun moveWidget(id: Int, delta: Int) = updateSettings { s ->
        val ids = s.widgetIds.toMutableList()
        val from = ids.indexOf(id)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return@updateSettings s
        ids.add(to, ids.removeAt(from))
        s.copy(widgetIds = ids)
    }

    fun setWidgetHeight(id: Int, heightDp: Int) = updateSettings { s ->
        s.copy(widgetHeights = s.widgetHeights + (id to heightDp))
    }

    fun photoPath(file: String) = photos.path(file)

    /** Copia las fotos elegidas a almacenamiento privado, para no depender de permisos ni de la galería. */
    fun setPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val previous = state.value.settings.photos
            val imported = withContext(Dispatchers.IO) { uris.mapNotNull(photos::import) }
            if (imported.isEmpty()) return@launch
            repo.update { it.copy(photos = imported) }
            withContext(Dispatchers.IO) { previous.forEach { photos.delete(it.file) } }
        }
    }

    fun addNote(text: String) {
        viewModelScope.launch { notesDao.insert(Note(text = text, updatedAt = System.currentTimeMillis())) }
    }

    fun editNote(note: Note, text: String) {
        viewModelScope.launch { notesDao.update(note.copy(text = text, updatedAt = System.currentTimeMillis())) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { notesDao.delete(note) }
    }

    private companion object {
        const val USAGE_MAX_AGE_MS = 60_000L
    }
}
