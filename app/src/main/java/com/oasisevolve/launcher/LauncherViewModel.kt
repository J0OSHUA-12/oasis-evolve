package com.oasisevolve.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oasisevolve.launcher.focus.AccessLevel
import com.oasisevolve.launcher.library.autoCategory
import com.oasisevolve.launcher.storage.AppFolder
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.Note
import com.oasisevolve.launcher.storage.WidgetTarget
import com.oasisevolve.launcher.storage.addToFolder
import com.oasisevolve.launcher.storage.applyVisibleOrder
import com.oasisevolve.launcher.storage.createFolder
import com.oasisevolve.launcher.storage.deleteFolder
import com.oasisevolve.launcher.storage.list
import com.oasisevolve.launcher.storage.removeFromFolder
import com.oasisevolve.launcher.storage.renameFolder
import com.oasisevolve.launcher.storage.withList
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.system.IconPackInfo
import com.oasisevolve.launcher.usage.AppStats
import com.oasisevolve.launcher.usage.UsageReport
import com.oasisevolve.launcher.usage.UsageWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un elemento de una cuadrícula: una app o una carpeta. */
sealed interface GridEntry {
    val key: String

    data class App(val app: AppEntry) : GridEntry {
        override val key: String get() = app.key
    }

    data class Folder(val folder: AppFolder, val apps: List<AppEntry>) : GridEntry {
        override val key: String get() = folder.entryKey
    }
}

data class LauncherState(
    val settings: LauncherSettings = LauncherSettings(),
    val allApps: List<AppEntry> = emptyList(),
) {
    private val byKey = allApps.associateBy { it.key }

    fun level(app: AppEntry): AccessLevel = AccessLevel.of(settings.accessLevels[app.key])

    fun category(app: AppEntry): String {
        settings.categoryOverrides[app.key]?.let { return it }
        val auto = autoCategory(app.packageName, app.appCategory)
        return settings.categoryRenames[auto] ?: auto
    }

    fun keys(list: AppList): List<String> = settings.list(list)

    private fun visibleApps(keys: List<String>): List<AppEntry> =
        keys.mapNotNull(byKey::get).filter { level(it).inVisualScreens }

    /** Apps de una lista que siguen instaladas y cuyo nivel permite mostrarlas. */
    fun visible(list: AppList): List<AppEntry> = visibleApps(keys(list))

    fun folder(id: Long): GridEntry.Folder? =
        settings.folders.firstOrNull { it.id == id }?.let { GridEntry.Folder(it, visibleApps(it.apps)) }

    fun folderOf(app: AppEntry): AppFolder? = settings.folders.firstOrNull { app.key in it.apps }

    /** Lo que se dibuja en una cuadrícula. Una carpeta sin apps visibles no se muestra. */
    fun entries(list: AppList): List<GridEntry> = keys(list).mapNotNull { key ->
        if (key.startsWith(AppFolder.FOLDER_PREFIX)) {
            val id = key.removePrefix(AppFolder.FOLDER_PREFIX).toLongOrNull()
            id?.let(::folder)?.takeIf { it.apps.isNotEmpty() }
        } else {
            byKey[key]?.takeIf { level(it).inVisualScreens }?.let { GridEntry.App(it) }
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

    val state: StateFlow<LauncherState> = combine(repo.settings, appRepo.apps) { settings, apps ->
        LauncherState(settings, apps)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherState())

    val iconRevision: StateFlow<Int> = appRepo.iconRevision

    val notes: StateFlow<List<Note>> = notesDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _gate = MutableStateFlow<GateRequest?>(null)
    val gate: StateFlow<GateRequest?> = _gate

    private val _usageWindow = MutableStateFlow(UsageWindow.LAST_24H)
    val usageWindow: StateFlow<UsageWindow> = _usageWindow

    private val _usageReport = MutableStateFlow<UsageReport?>(null)
    val usageReport: StateFlow<UsageReport?> = _usageReport

    private val _hasUsageAccess = MutableStateFlow(usageRepo.hasAccess())
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess

    init {
        viewModelScope.launch {
            repo.settings.map { it.iconPack }.distinctUntilChanged().collect { appRepo.setIconPack(it) }
        }
    }

    suspend fun icon(app: AppEntry) = appRepo.icon(app)

    fun installedIconPacks(): List<IconPackInfo> = appRepo.installedIconPacks()

    fun open(app: AppEntry) {
        if (state.value.level(app) != AccessLevel.GUARDED) {
            appRepo.launch(app)
            return
        }
        viewModelScope.launch {
            val stats = if (usageRepo.hasAccess()) usageRepo.statsFor(app.packageName) else null
            _gate.value = GateRequest(app, stats)
        }
    }

    fun continueGate() {
        _gate.value?.let { appRepo.launch(it.app) }
        _gate.value = null
    }

    fun dismissGate() {
        _gate.value = null
    }

    fun onResume() {
        refreshUsage()
        appRepo.dropIconPackIfUninstalled()
    }

    fun refreshUsage() {
        val allowed = usageRepo.hasAccess()
        _hasUsageAccess.value = allowed
        if (!allowed) {
            _usageReport.value = null
            return
        }
        viewModelScope.launch { _usageReport.value = usageRepo.report(_usageWindow.value) }
    }

    fun selectUsageWindow(window: UsageWindow) {
        _usageWindow.value = window
        _usageReport.value = null
        refreshUsage()
    }

    fun openUsageAccess() = appRepo.openUsageAccessSettings()
    fun openAppInfo(app: AppEntry) = appRepo.openAppInfo(app)
    fun uninstall(app: AppEntry) = appRepo.uninstall(app)
    fun refreshApps() = appRepo.refresh()

    fun updateSettings(change: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { repo.update(change) }
    }

    fun toggle(list: AppList, app: AppEntry) = updateSettings { s ->
        val keys = s.list(list)
        s.withList(list, if (app.key in keys) keys - app.key else keys + app.key)
    }

    /** Mueve una app del dock, que no se ordena arrastrando. */
    fun move(list: AppList, app: AppEntry, delta: Int) = updateSettings { s ->
        val keys = s.list(list).toMutableList()
        val from = keys.indexOf(app.key)
        val to = from + delta
        if (from < 0 || to !in keys.indices) return@updateSettings s
        keys.add(to, keys.removeAt(from))
        s.withList(list, keys)
    }

    fun setOrder(list: AppList, visibleOrder: List<String>) = updateSettings { s ->
        s.withList(list, applyVisibleOrder(s.list(list), visibleOrder))
    }

    fun createFolder(app: AppEntry, name: String) = updateSettings {
        it.createFolder(app.key, name, System.currentTimeMillis())
    }

    fun addToFolder(folderId: Long, app: AppEntry) = updateSettings { it.addToFolder(folderId, app.key) }

    fun removeFromFolder(folderId: Long, app: AppEntry) = updateSettings { it.removeFromFolder(folderId, app.key) }

    fun renameFolder(folderId: Long, name: String) = updateSettings { it.renameFolder(folderId, name) }

    fun deleteFolder(folderId: Long) = updateSettings { it.deleteFolder(folderId) }

    fun setLevel(app: AppEntry, level: AccessLevel) = updateSettings { s ->
        val levels = if (level == AccessLevel.VISIBLE) s.accessLevels - app.key
        else s.accessLevels + (app.key to level.level)
        s.copy(accessLevels = levels)
    }

    fun setCategory(app: AppEntry, category: String) = updateSettings { s ->
        s.copy(categoryOverrides = s.categoryOverrides + (app.key to category))
    }

    fun moveWidget(target: WidgetTarget, id: Int, delta: Int) = updateSettings { s ->
        val ids = (if (target == WidgetTarget.HOME) s.homeWidgetIds else s.widgetIds).toMutableList()
        val from = ids.indexOf(id)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return@updateSettings s
        ids.add(to, ids.removeAt(from))
        if (target == WidgetTarget.HOME) s.copy(homeWidgetIds = ids) else s.copy(widgetIds = ids)
    }

    fun setWidgetHeight(id: Int, heightDp: Int) = updateSettings { s ->
        s.copy(widgetHeights = s.widgetHeights + (id to heightDp))
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
}
