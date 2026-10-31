package com.oasisevolve.launcher.storage

/** Cuadrícula libre de Inicio y Apps útiles. Lógica pura, sin Android, para probarla en la JVM. */

enum class HomeItemKind { APP, FOLDER, WIDGET, CLOCK, PHOTO, SCREEN_TIME, SEARCH }

enum class GridTarget { HOME, USEFUL }

/** Un elemento ocupa un rectángulo de celdas. [ref] es la clave de la app o el id de carpeta o widget. */
data class HomeItem(
    val id: String,
    val kind: HomeItemKind,
    val ref: String = "",
    val col: Int,
    val row: Int,
    val w: Int = 1,
    val h: Int = 1,
) {
    /** Solo las apps y las carpetas pueden agruparse. */
    val isGroupable: Boolean get() = kind == HomeItemKind.APP || kind == HomeItemKind.FOLDER

    /** Las apps y las carpetas siempre ocupan una celda. */
    val isResizable: Boolean get() = !isGroupable

    fun occupies(c: Int, r: Int): Boolean = c in col until col + w && r in row until row + h
}

fun appItem(key: String, col: Int, row: Int) = HomeItem("app:$key", HomeItemKind.APP, key, col, row)

fun folderItem(folderId: Long, col: Int, row: Int) =
    HomeItem("${AppFolder.FOLDER_PREFIX}$folderId", HomeItemKind.FOLDER, folderId.toString(), col, row)

fun widgetItem(widgetId: Int, col: Int, row: Int, w: Int, h: Int) =
    HomeItem("widget:$widgetId", HomeItemKind.WIDGET, widgetId.toString(), col, row, w, h)

private fun overlaps(a: HomeItem, col: Int, row: Int, w: Int, h: Int): Boolean =
    a.col < col + w && col < a.col + a.w && a.row < row + h && row < a.row + a.h

/** ¿Cabe un rectángulo sin salirse de la cuadrícula ni pisar nada? [ignoreId] es el elemento que se mueve. */
fun canPlace(
    items: List<HomeItem>,
    col: Int,
    row: Int,
    w: Int,
    h: Int,
    columns: Int,
    rows: Int,
    ignoreId: String? = null,
): Boolean {
    if (col < 0 || row < 0 || col + w > columns || row + h > rows) return false
    return items.none { it.id != ignoreId && overlaps(it, col, row, w, h) }
}

/** Primer hueco libre. Si no hay, abre una fila nueva: un elemento nunca se pierde por falta de sitio. */
fun firstFree(items: List<HomeItem>, w: Int, h: Int, columns: Int, rows: Int): Pair<Int, Int> {
    val width = w.coerceAtMost(columns)
    val limit = maxOf(rows, (items.maxOfOrNull { it.row + it.h } ?: 0)) + h
    for (row in 0 until limit) {
        for (col in 0..columns - width) {
            if (canPlace(items, col, row, width, h, columns, limit)) return col to row
        }
    }
    return 0 to limit
}

/** Filas reales de la cuadrícula: al menos las configuradas, más las que hagan falta. */
fun effectiveRows(items: List<HomeItem>, rows: Int): Int =
    maxOf(rows, items.maxOfOrNull { it.row + it.h } ?: 0)

/** Elemento de la celda, o `null` si está libre. */
fun itemAt(items: List<HomeItem>, col: Int, row: Int, ignoreId: String? = null): HomeItem? =
    items.firstOrNull { it.id != ignoreId && it.occupies(col, row) }

fun moveItem(items: List<HomeItem>, id: String, col: Int, row: Int): List<HomeItem> =
    items.map { if (it.id == id) it.copy(col = col, row = row) else it }

/**
 * Tamaño más cercano al pedido que cabe sin pisar nada. Si no cabe ninguno mayor, se conserva el actual.
 * Solo crece o encoge desde su esquina superior izquierda.
 */
fun resizeItem(
    items: List<HomeItem>,
    id: String,
    requestedW: Int,
    requestedH: Int,
    columns: Int,
    rows: Int,
): List<HomeItem> {
    val item = items.firstOrNull { it.id == id } ?: return items
    val w = requestedW.coerceIn(1, columns - item.col)
    val h = requestedH.coerceAtLeast(1)
    val fits = canPlace(items, item.col, item.row, w, h, columns, maxOf(rows, item.row + h), ignoreId = id)
    return if (fits) items.map { if (it.id == id) it.copy(w = w, h = h) else it } else items
}

/** Coloca elementos nuevos conservando los existentes. */
fun addItems(
    items: List<HomeItem>,
    columns: Int,
    rows: Int,
    create: List<(col: Int, row: Int) -> HomeItem>,
): List<HomeItem> = create.fold(items) { acc, make ->
    val probe = make(0, 0)
    val (col, row) = firstFree(acc, probe.w, probe.h, columns, rows)
    acc + make(col, row)
}
