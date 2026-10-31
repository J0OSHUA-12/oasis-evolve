package com.oasisevolve.launcher.storage

import com.oasisevolve.launcher.library.DEFAULT_CATEGORIES
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppFont(val label: String) {
    SYSTEM("Sistema"),
    MANROPE("Manrope"),
    OUTFIT("Outfit"),
    JETBRAINS_MONO("JetBrains Mono"),
}

enum class Accent(val label: String, val dark: Long, val light: Long) {
    JADE("Jade", 0xFF6EE7B7, 0xFF047857),
    SKY("Cielo", 0xFF7DD3FC, 0xFF0369A1),
    AMBER("Ámbar", 0xFFFCD34D, 0xFFB45309),
    ROSE("Rosa", 0xFFFDA4AF, 0xFFBE123C),
    VIOLET("Violeta", 0xFFC4B5FD, 0xFF6D28D9),
}

enum class IconShape(val label: String, val cornerFraction: Float) {
    SQUIRCLE("Redondeado", 0.28f),
    CIRCLE("Círculo", 0.5f),
    SQUARE("Cuadrado", 0.14f),
}

enum class WidgetTarget { HOME, CENTER }

data class AppFolder(val id: Long, val name: String, val apps: List<String>) {
    val entryKey: String get() = "$FOLDER_PREFIX$id"

    companion object {
        const val FOLDER_PREFIX = "folder:"
    }
}

/** Toda la configuración como un único valor, lo que facilita la escritura atómica y exportarla después. */
data class LauncherSettings(
    val homeApps: List<String> = emptyList(),
    val dockApps: List<String> = emptyList(),
    val usefulApps: List<String> = emptyList(),
    val wantMoreApps: List<String> = emptyList(),
    val folders: List<AppFolder> = emptyList(),
    val accessLevels: Map<String, Int> = emptyMap(),
    val categories: List<String> = DEFAULT_CATEGORIES,
    val hiddenCategories: Set<String> = emptySet(),
    val categoryOverrides: Map<String, String> = emptyMap(),
    val categoryRenames: Map<String, String> = emptyMap(),
    val widgetIds: List<Int> = emptyList(),
    val homeWidgetIds: List<Int> = emptyList(),
    val widgetHeights: Map<Int, Int> = emptyMap(),
    val waitSeconds: Int = 10,
    val gridColumns: Int = 4,
    val iconSizeDp: Int = 56,
    val gridSpacingDp: Int = 12,
    val pageMarginDp: Int = 20,
    val showClock: Boolean = true,
    val showDock: Boolean = true,
    val showHomeWidgets: Boolean = true,
    val showIcons: Boolean = true,
    val showLabels: Boolean = true,
    val libraryEnabled: Boolean = true,
    val libraryExpanded: Boolean = false,
    val reduceMotion: Boolean = false,
    val glassBlur: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.JADE,
    val font: AppFont = AppFont.SYSTEM,
    val fontScale: Float = 1f,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconPack: String? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("homeApps", JSONArray(homeApps))
        put("dockApps", JSONArray(dockApps))
        put("usefulApps", JSONArray(usefulApps))
        put("wantMoreApps", JSONArray(wantMoreApps))
        put(
            "folders",
            JSONArray(
                folders.map { folder ->
                    JSONObject().put("id", folder.id).put("name", folder.name).put("apps", JSONArray(folder.apps))
                },
            ),
        )
        put("accessLevels", JSONObject(accessLevels as Map<*, *>))
        put("categories", JSONArray(categories))
        put("hiddenCategories", JSONArray(hiddenCategories.toList()))
        put("categoryOverrides", JSONObject(categoryOverrides as Map<*, *>))
        put("categoryRenames", JSONObject(categoryRenames as Map<*, *>))
        put("widgetIds", JSONArray(widgetIds))
        put("homeWidgetIds", JSONArray(homeWidgetIds))
        put("widgetHeights", JSONObject(widgetHeights.mapKeys { it.key.toString() } as Map<*, *>))
        put("waitSeconds", waitSeconds)
        put("gridColumns", gridColumns)
        put("iconSizeDp", iconSizeDp)
        put("gridSpacingDp", gridSpacingDp)
        put("pageMarginDp", pageMarginDp)
        put("showClock", showClock)
        put("showDock", showDock)
        put("showHomeWidgets", showHomeWidgets)
        put("showIcons", showIcons)
        put("showLabels", showLabels)
        put("libraryEnabled", libraryEnabled)
        put("libraryExpanded", libraryExpanded)
        put("reduceMotion", reduceMotion)
        put("glassBlur", glassBlur)
        put("themeMode", themeMode.name)
        put("accent", accent.name)
        put("font", font.name)
        put("fontScale", fontScale.toDouble())
        put("iconShape", iconShape.name)
        put("iconPack", iconPack ?: JSONObject.NULL)
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
                folders = o.optJSONArray("folders")?.let { array ->
                    List(array.length()) { index ->
                        val folder = array.getJSONObject(index)
                        AppFolder(folder.getLong("id"), folder.getString("name"), folder.stringList("apps"))
                    }
                }.orEmpty(),
                accessLevels = o.map("accessLevels") { obj, key -> obj.getInt(key) },
                categories = if (o.has("categories")) o.stringList("categories") else defaults.categories,
                hiddenCategories = o.stringList("hiddenCategories").toSet(),
                categoryOverrides = o.map("categoryOverrides") { obj, key -> obj.getString(key) },
                categoryRenames = o.map("categoryRenames") { obj, key -> obj.getString(key) },
                widgetIds = o.intList("widgetIds"),
                homeWidgetIds = o.intList("homeWidgetIds"),
                widgetHeights = o.map("widgetHeights") { obj, key -> obj.getInt(key) }
                    .mapKeys { it.key.toInt() },
                waitSeconds = o.optInt("waitSeconds", defaults.waitSeconds),
                gridColumns = o.optInt("gridColumns", defaults.gridColumns),
                iconSizeDp = o.optInt("iconSizeDp", defaults.iconSizeDp),
                gridSpacingDp = o.optInt("gridSpacingDp", defaults.gridSpacingDp),
                pageMarginDp = o.optInt("pageMarginDp", defaults.pageMarginDp),
                showClock = o.optBoolean("showClock", defaults.showClock),
                showDock = o.optBoolean("showDock", defaults.showDock),
                showHomeWidgets = o.optBoolean("showHomeWidgets", defaults.showHomeWidgets),
                showIcons = o.optBoolean("showIcons", defaults.showIcons),
                showLabels = o.optBoolean("showLabels", defaults.showLabels),
                libraryEnabled = o.optBoolean("libraryEnabled", defaults.libraryEnabled),
                libraryExpanded = o.optBoolean("libraryExpanded", defaults.libraryExpanded),
                reduceMotion = o.optBoolean("reduceMotion", defaults.reduceMotion),
                glassBlur = o.optBoolean("glassBlur", defaults.glassBlur),
                themeMode = o.enumValue("themeMode", defaults.themeMode),
                accent = o.enumValue("accent", defaults.accent),
                font = o.enumValue("font", defaults.font),
                fontScale = o.optDouble("fontScale", defaults.fontScale.toDouble()).toFloat(),
                iconShape = o.enumValue("iconShape", defaults.iconShape),
                iconPack = if (o.isNull("iconPack")) null else o.getString("iconPack"),
            )
        }

        private fun JSONObject.stringList(name: String): List<String> =
            optJSONArray(name)?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()

        private fun JSONObject.intList(name: String): List<Int> =
            optJSONArray(name)?.let { a -> List(a.length()) { a.getInt(it) } }.orEmpty()

        private fun <T> JSONObject.map(name: String, read: (JSONObject, String) -> T): Map<String, T> {
            val obj = optJSONObject(name) ?: return emptyMap()
            return obj.keys().asSequence().associateWith { read(obj, it) }
        }

        private inline fun <reified E : Enum<E>> JSONObject.enumValue(name: String, fallback: E): E =
            enumValues<E>().firstOrNull { it.name == optString(name) } ?: fallback
    }
}
