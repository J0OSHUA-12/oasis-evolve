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

enum class WidgetTarget { HOME, USEFUL, CENTER }

data class AppFolder(val id: Long, val name: String, val apps: List<String>) {
    val entryKey: String get() = "$FOLDER_PREFIX$id"

    companion object {
        const val FOLDER_PREFIX = "folder:"
    }
}

/** Foto del widget de fotografías, copiada a almacenamiento privado de la app. */
data class WidgetPhoto(val file: String, val takenAt: Long?)

/** Toda la configuración como un único valor, lo que facilita la escritura atómica y exportarla después. */
data class LauncherSettings(
    val homeLayout: List<HomeItem> = emptyList(),
    val usefulLayout: List<HomeItem> = emptyList(),
    val layoutSeeded: Boolean = false,
    val dockApps: List<String> = emptyList(),
    val folders: List<AppFolder> = emptyList(),
    val photos: List<WidgetPhoto> = emptyList(),
    val accessLevels: Map<String, Int> = emptyMap(),
    val categories: List<String> = DEFAULT_CATEGORIES,
    val hiddenCategories: Set<String> = emptySet(),
    val categoryOverrides: Map<String, String> = emptyMap(),
    val categoryRenames: Map<String, String> = emptyMap(),
    val widgetIds: List<Int> = emptyList(),
    val widgetHeights: Map<Int, Int> = emptyMap(),
    val waitSeconds: Int = 10,
    /** Minutos de uso permitidos cada vez que se abre una app, por clave de app. */
    val appTimers: Map<String, Int> = emptyMap(),
    /** Paquetes cuyas notificaciones se posponen hasta la franja elegida. */
    val quietApps: Set<String> = emptySet(),
    val quietStartMinute: Int = 21 * 60,
    val quietLengthMinutes: Int = 60,
    val gridColumns: Int = 4,
    val gridRows: Int = 7,
    val iconSizeDp: Int = 56,
    val gridSpacingDp: Int = 12,
    val pageMarginDp: Int = 20,
    val showDock: Boolean = true,
    val showIcons: Boolean = true,
    val showLabels: Boolean = true,
    val libraryEnabled: Boolean = true,
    val libraryExpanded: Boolean = false,
    val reduceMotion: Boolean = false,
    val glassBlur: Boolean = false,
    val immersiveMode: Boolean = false,
    val weekStartIso: Int = 1,
    val academicSemester: Boolean = false,
    val academicStartDay: Long? = null,
    val academicEndDay: Long? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: Accent = Accent.JADE,
    val font: AppFont = AppFont.SYSTEM,
    val fontScale: Float = 1f,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconPack: String? = null,
) {
    fun layout(target: GridTarget): List<HomeItem> =
        if (target == GridTarget.HOME) homeLayout else usefulLayout

    fun withLayout(target: GridTarget, items: List<HomeItem>): LauncherSettings =
        if (target == GridTarget.HOME) copy(homeLayout = items) else copy(usefulLayout = items)

    fun toJson(): String = JSONObject().apply {
        put("version", SCHEMA_VERSION)
        put("homeLayout", layoutToJson(homeLayout))
        put("usefulLayout", layoutToJson(usefulLayout))
        put("layoutSeeded", layoutSeeded)
        put("dockApps", JSONArray(dockApps))
        put(
            "folders",
            JSONArray(
                folders.map { folder ->
                    JSONObject().put("id", folder.id).put("name", folder.name).put("apps", JSONArray(folder.apps))
                },
            ),
        )
        put(
            "photos",
            JSONArray(
                photos.map { photo ->
                    JSONObject().put("file", photo.file).put("takenAt", photo.takenAt ?: JSONObject.NULL)
                },
            ),
        )
        put("accessLevels", JSONObject(accessLevels as Map<*, *>))
        put("categories", JSONArray(categories))
        put("hiddenCategories", JSONArray(hiddenCategories.toList()))
        put("categoryOverrides", JSONObject(categoryOverrides as Map<*, *>))
        put("categoryRenames", JSONObject(categoryRenames as Map<*, *>))
        put("widgetIds", JSONArray(widgetIds))
        put("widgetHeights", JSONObject(widgetHeights.mapKeys { it.key.toString() } as Map<*, *>))
        put("waitSeconds", waitSeconds)
        put("appTimers", JSONObject(appTimers as Map<*, *>))
        put("quietApps", JSONArray(quietApps.toList()))
        put("quietStartMinute", quietStartMinute)
        put("quietLengthMinutes", quietLengthMinutes)
        put("gridColumns", gridColumns)
        put("gridRows", gridRows)
        put("iconSizeDp", iconSizeDp)
        put("gridSpacingDp", gridSpacingDp)
        put("pageMarginDp", pageMarginDp)
        put("showDock", showDock)
        put("showIcons", showIcons)
        put("showLabels", showLabels)
        put("libraryEnabled", libraryEnabled)
        put("libraryExpanded", libraryExpanded)
        put("reduceMotion", reduceMotion)
        put("glassBlur", glassBlur)
        put("immersiveMode", immersiveMode)
        put("weekStartIso", weekStartIso)
        put("academicSemester", academicSemester)
        put("academicStartDay", academicStartDay ?: JSONObject.NULL)
        put("academicEndDay", academicEndDay ?: JSONObject.NULL)
        put("themeMode", themeMode.name)
        put("accent", accent.name)
        put("font", font.name)
        put("fontScale", fontScale.toDouble())
        put("iconShape", iconShape.name)
        put("iconPack", iconPack ?: JSONObject.NULL)
    }.toString()

    companion object {
        private const val SCHEMA_VERSION = 3

        private fun layoutToJson(items: List<HomeItem>) = JSONArray(
            items.map { item ->
                JSONObject()
                    .put("id", item.id)
                    .put("kind", item.kind.name)
                    .put("ref", item.ref)
                    .put("col", item.col)
                    .put("row", item.row)
                    .put("w", item.w)
                    .put("h", item.h)
            },
        )

        private fun layoutFromJson(array: JSONArray): List<HomeItem> = List(array.length()) { index ->
            val o = array.getJSONObject(index)
            val kind = HomeItemKind.entries.firstOrNull { it.name == o.getString("kind") } ?: return@List null
            HomeItem(
                id = o.getString("id"),
                kind = kind,
                ref = o.optString("ref"),
                col = o.getInt("col"),
                row = o.getInt("row"),
                w = o.optInt("w", 1),
                h = o.optInt("h", 1),
            )
        }.filterNotNull()

        /** Tolera campos ausentes o desconocidos, para que actualizar la app no borre los ajustes. */
        fun fromJson(json: String?): LauncherSettings {
            if (json.isNullOrBlank()) return LauncherSettings()
            val defaults = LauncherSettings()
            val o = runCatching { JSONObject(json) }.getOrNull() ?: return defaults
            val folders = o.optJSONArray("folders")?.let { array ->
                List(array.length()) { index ->
                    val folder = array.getJSONObject(index)
                    AppFolder(folder.getLong("id"), folder.getString("name"), folder.stringList("apps"))
                }
            }.orEmpty()
            val parsed = LauncherSettings(
                homeLayout = o.optJSONArray("homeLayout")?.let(::layoutFromJson).orEmpty(),
                usefulLayout = o.optJSONArray("usefulLayout")?.let(::layoutFromJson).orEmpty(),
                layoutSeeded = o.optBoolean("layoutSeeded", false),
                dockApps = o.stringList("dockApps"),
                folders = folders,
                photos = o.optJSONArray("photos")?.let { array ->
                    List(array.length()) { index ->
                        val photo = array.getJSONObject(index)
                        WidgetPhoto(
                            photo.getString("file"),
                            if (photo.isNull("takenAt")) null else photo.getLong("takenAt"),
                        )
                    }
                }.orEmpty(),
                accessLevels = o.map("accessLevels") { obj, key -> obj.getInt(key) },
                categories = if (o.has("categories")) o.stringList("categories") else defaults.categories,
                hiddenCategories = o.stringList("hiddenCategories").toSet(),
                categoryOverrides = o.map("categoryOverrides") { obj, key -> obj.getString(key) },
                categoryRenames = o.map("categoryRenames") { obj, key -> obj.getString(key) },
                widgetIds = o.intList("widgetIds"),
                widgetHeights = o.map("widgetHeights") { obj, key -> obj.getInt(key) }
                    .mapKeys { it.key.toInt() },
                waitSeconds = o.optInt("waitSeconds", defaults.waitSeconds),
                appTimers = o.map("appTimers") { obj, key -> obj.getInt(key) },
                quietApps = o.stringList("quietApps").toSet(),
                quietStartMinute = o.optInt("quietStartMinute", defaults.quietStartMinute),
                quietLengthMinutes = o.optInt("quietLengthMinutes", defaults.quietLengthMinutes),
                gridColumns = o.optInt("gridColumns", defaults.gridColumns),
                gridRows = o.optInt("gridRows", defaults.gridRows),
                iconSizeDp = o.optInt("iconSizeDp", defaults.iconSizeDp),
                gridSpacingDp = o.optInt("gridSpacingDp", defaults.gridSpacingDp),
                pageMarginDp = o.optInt("pageMarginDp", defaults.pageMarginDp),
                showDock = o.optBoolean("showDock", defaults.showDock),
                showIcons = o.optBoolean("showIcons", defaults.showIcons),
                showLabels = o.optBoolean("showLabels", defaults.showLabels),
                libraryEnabled = o.optBoolean("libraryEnabled", defaults.libraryEnabled),
                libraryExpanded = o.optBoolean("libraryExpanded", defaults.libraryExpanded),
                reduceMotion = o.optBoolean("reduceMotion", defaults.reduceMotion),
                glassBlur = o.optBoolean("glassBlur", defaults.glassBlur),
                immersiveMode = o.optBoolean("immersiveMode", defaults.immersiveMode),
                weekStartIso = o.optInt("weekStartIso", defaults.weekStartIso),
                academicSemester = o.optBoolean("academicSemester", defaults.academicSemester),
                academicStartDay = if (o.isNull("academicStartDay")) null else o.getLong("academicStartDay"),
                academicEndDay = if (o.isNull("academicEndDay")) null else o.getLong("academicEndDay"),
                themeMode = o.enumValue("themeMode", defaults.themeMode),
                accent = o.enumValue("accent", defaults.accent),
                font = o.enumValue("font", defaults.font),
                fontScale = o.optDouble("fontScale", defaults.fontScale.toDouble()).toFloat(),
                iconShape = o.enumValue("iconShape", defaults.iconShape),
                iconPack = if (o.isNull("iconPack")) null else o.getString("iconPack"),
            )
            return if (o.has("homeLayout")) parsed else migrateFromV2(parsed, o)
        }

        /**
         * Convierte las listas de las versiones 0.1 y 0.2 en la cuadrícula. No se pierde nada: las apps
         * siguen colocadas, y «Quiero usar más» pasa a ser una carpeta, porque esa pantalla ya no tiene títulos.
         */
        private fun migrateFromV2(parsed: LauncherSettings, o: JSONObject): LauncherSettings {
            val columns = parsed.gridColumns
            val rows = parsed.gridRows
            fun entryMaker(key: String): (Int, Int) -> HomeItem {
                if (!key.startsWith(AppFolder.FOLDER_PREFIX)) return { col, row -> appItem(key, col, row) }
                val id = key.removePrefix(AppFolder.FOLDER_PREFIX).toLong()
                return { col, row -> folderItem(id, col, row) }
            }

            val widgetSpan = 2
            val home = addItems(
                emptyList(),
                columns,
                rows,
                o.intList("homeWidgetIds").map { id ->
                    { col: Int, row: Int -> widgetItem(id, col, row, columns.coerceAtMost(4), widgetSpan) }
                } + o.stringList("homeApps").map(::entryMaker),
            )

            val wantMore = o.stringList("wantMoreApps")
            val usefulKeys = o.stringList("usefulApps")
            // Los identificadores de carpeta son marcas de tiempo: 1 no puede coincidir con uno real.
            val wantMoreFolder = if (wantMore.isEmpty()) null else AppFolder(1, "Quiero usar más", wantMore.filterNot {
                it.startsWith(AppFolder.FOLDER_PREFIX)
            })
            val useful = addItems(
                emptyList(),
                columns,
                rows,
                listOfNotNull(wantMoreFolder?.let { folder -> { col: Int, row: Int -> folderItem(folder.id, col, row) } }) +
                    wantMore.filter { it.startsWith(AppFolder.FOLDER_PREFIX) }.map(::entryMaker) +
                    usefulKeys.map(::entryMaker),
            )
            return parsed.copy(
                homeLayout = home,
                usefulLayout = useful,
                folders = parsed.folders + listOfNotNull(wantMoreFolder),
                // Quien ya usaba la app conserva su tema hasta que lo cambie.
                themeMode = o.enumValue("themeMode", ThemeMode.SYSTEM),
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
