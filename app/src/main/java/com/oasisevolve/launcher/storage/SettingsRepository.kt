package com.oasisevolve.launcher.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher")
private val STATE = stringPreferencesKey("state")

class SettingsRepository(private val context: Context) {
    val settings: Flow<LauncherSettings> =
        context.dataStore.data.map { LauncherSettings.fromJson(it[STATE]) }

    suspend fun update(change: (LauncherSettings) -> LauncherSettings) {
        context.dataStore.edit { prefs ->
            prefs[STATE] = change(LauncherSettings.fromJson(prefs[STATE])).toJson()
        }
    }
}
