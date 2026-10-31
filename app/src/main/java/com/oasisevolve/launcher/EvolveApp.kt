package com.oasisevolve.launcher

import android.app.Application
import com.oasisevolve.launcher.storage.EvolveDatabase
import com.oasisevolve.launcher.storage.SettingsRepository
import com.oasisevolve.launcher.system.AppRepository
import com.oasisevolve.launcher.usage.UsageRepository
import com.oasisevolve.launcher.widgets.WidgetHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EvolveApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings by lazy { SettingsRepository(this) }
    val apps by lazy { AppRepository(this, appScope) }
    val usage by lazy { UsageRepository(this) }
    val database by lazy { EvolveDatabase.create(this) }
    val widgets by lazy { WidgetHostController(this) }
}
