package com.oasisevolve.launcher.storage

import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Toda la configuración como un único valor, lo que facilita la escritura atómica y exportarla después. */
data class LauncherSettings(
    val homeApps: List<String> = emptyList(),
    val dockApps: List<String> = emptyList(),
    val usefulApps: List<String> = emptyList(),
    val wantMoreApps: List<String> = emptyList(),
    val accessLevels: Map<String, Int> = emptyMap(),
    val categoryOverrides: Map<String, String> = emptyMap(),
    val widgetIds: List<Int> = emptyList(),
    val waitSeconds: Int = 10,
    val gridColumns: Int = 4,
    val showClock: Boolean = true,
    val showDock: Boolean = true,
    val showIcons: Boolean = true,
    val showLabels: Boolean = true,
    val libraryEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    fun toJson(): String = JSONObject().apply {
        put("homeApps", JSONArray(homeApps))
        put("dockApps", JSONArray(dockApps))
        put("usefulApps", JSONArray(usefulApps))
        put("wantMoreApps", JSONArray(wantMoreApps))
        put("accessLevels", JSONObject(accessLevels as Map<*, *>))
        put("categoryOverrides", JSONObject(categoryOverrides as Map<*, *>))
        put("widgetIds", JSONArray(widgetIds))
        put("waitSeconds", waitSeconds)
        put("gridColumns", gridColumns)
        put("showClock", showClock)
        put("showDock", showDock)
        put("showIcons", showIcons)
        put("showLabels", showLabels)
        put("libraryEnabled", libraryEnabled)
        put("reduceMotion", reduceMotion)
        put("themeMode", themeMode.name)
    }.toString()

    companion object {
        /** Tolera campos ausentes o desconocidos, para que actualizar la app no borre los ajustes. */
        fun fromJson(json: String?): LauncherSettings {
            if (json.isNullOrBlank()) return LauncherSettings()
            val defaults = LauncherSettings()
            val o = runCatching { JSONObject(json) }.getOrNull() ?: return defaults
            return LauncherSettings(
                homeApps = o.stringList("homeApps"),
                dockApps = o.stringList("dockApps"),
                usefulApps = o.stringList("usefulApps"),
                wantMoreApps = o.stringList("wantMoreApps"),
                accessLevels = o.map("accessLevels") { obj, key -> obj.getInt(key) },
                categoryOverrides = o.map("categoryOverrides") { obj, key -> obj.getString(key) },
                widgetIds = o.optJSONArray("widgetIds")?.let { a -> List(a.length()) { a.getInt(it) } }.orEmpty(),
                waitSeconds = o.optInt("waitSeconds", defaults.waitSeconds),
                gridColumns = o.optInt("gridColumns", defaults.gridColumns),
                showClock = o.optBoolean("showClock", defaults.showClock),
                showDock = o.optBoolean("showDock", defaults.showDock),
                showIcons = o.optBoolean("showIcons", defaults.showIcons),
                showLabels = o.optBoolean("showLabels", defaults.showLabels),
                libraryEnabled = o.optBoolean("libraryEnabled", defaults.libraryEnabled),
                reduceMotion = o.optBoolean("reduceMotion", defaults.reduceMotion),
                themeMode = ThemeMode.entries.firstOrNull { it.name == o.optString("themeMode") } ?: defaults.themeMode,
            )
        }

        private fun JSONObject.stringList(name: String): List<String> =
            optJSONArray(name)?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()

        private fun <T> JSONObject.map(name: String, read: (JSONObject, String) -> T): Map<String, T> {
            val obj = optJSONObject(name) ?: return emptyMap()
            return obj.keys().asSequence().associateWith { read(obj, it) }
        }
    }
}
