package com.oasisevolve.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oasisevolve.launcher.focus.AccessLevel
import com.oasisevolve.launcher.library.autoCategory
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.Note
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.usage.AppStats
import com.oasisevolve.launcher.usage.UsageReport
import com.oasisevolve.launcher.usage.UsageWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Colecciones de apps que el usuario coloca a mano. */
enum class AppList(val label: String) {
    HOME("Pantalla principal"),
    DOCK("Dock"),
    USEFUL("Apps útiles"),
    WANT_MORE("Quiero usar más"),
}

data class LauncherState(
    val settings: LauncherSettings = LauncherSettings(),
    val allApps: List<AppEntry> = emptyList(),
) {
    private val byKey = allApps.associateBy { it.key }

    fun level(app: AppEntry): AccessLevel = AccessLevel.of(settings.accessLevels[app.key])

    fun category(app: AppEntry): String =
        settings.categoryOverrides[app.key] ?: autoCategory(app.packageName, app.appCategory)

    fun keys(list: AppList): List<String> = when (list) {
        AppList.HOME -> settings.homeApps
        AppList.DOCK -> settings.dockApps
        AppList.USEFUL -> settings.usefulApps
        AppList.WANT_MORE -> settings.wantMoreApps
    }

    /** Apps de una lista que siguen instaladas y cuyo nivel permite mostrarlas. */
    fun visible(list: AppList): List<AppEntry> =
        keys(list).mapNotNull(byKey::get).filter { level(it).inVisualScreens }
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

    suspend fun icon(app: AppEntry) = appRepo.icon(app)

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

    private fun LauncherSettings.withList(list: AppList, keys: List<String>) = when (list) {
        AppList.HOME -> copy(homeApps = keys)
        AppList.DOCK -> copy(dockApps = keys)
        AppList.USEFUL -> copy(usefulApps = keys)
        AppList.WANT_MORE -> copy(wantMoreApps = keys)
    }

    private fun LauncherSettings.list(list: AppList) = when (list) {
        AppList.HOME -> homeApps
        AppList.DOCK -> dockApps
        AppList.USEFUL -> usefulApps
        AppList.WANT_MORE -> wantMoreApps
    }

    fun toggle(list: AppList, app: AppEntry) = updateSettings { s ->
        val keys = s.list(list)
        s.withList(list, if (app.key in keys) keys - app.key else keys + app.key)
    }

    /** Mueve una app dentro de su lista. El orden se aplica en la lista completa guardada. */
    fun move(list: AppList, app: AppEntry, delta: Int) = updateSettings { s ->
        val keys = s.list(list).toMutableList()
        val from = keys.indexOf(app.key)
        val to = from + delta
        if (from < 0 || to !in keys.indices) return@updateSettings s
        keys.add(to, keys.removeAt(from))
        s.withList(list, keys)
    }

    fun setLevel(app: AppEntry, level: AccessLevel) = updateSettings { s ->
        val levels = if (level == AccessLevel.VISIBLE) s.accessLevels - app.key
        else s.accessLevels + (app.key to level.level)
        s.copy(accessLevels = levels)
    }

    fun setCategory(app: AppEntry, category: String) = updateSettings { s ->
        s.copy(categoryOverrides = s.categoryOverrides + (app.key to category))
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
