package com.oasisevolve.launcher.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.oasisevolve.launcher.storage.HomeItem
import com.oasisevolve.launcher.storage.HomeItemKind
import com.oasisevolve.launcher.storage.canPlace
import com.oasisevolve.launcher.storage.itemAt
import kotlin.math.floor
import kotlin.math.roundToInt

sealed interface DragPayload {
    data class GridElement(val item: HomeItem) : DragPayload
    data class DockApp(val key: String) : DragPayload
}

sealed interface DropTarget {
    data object None : DropTarget
    data class Cell(val col: Int, val row: Int, val valid: Boolean) : DropTarget
    data class Merge(val item: HomeItem) : DropTarget
    data class Dock(val index: Int) : DropTarget
}

/**
 * Qué pasaría al soltar aquí. Una app sobre otra app o carpeta las agrupa; cualquier otro solape es
 * inválido. Lógica pura: la cuadrícula solo le pasa las celdas bajo el dedo.
 */
fun resolveDrop(
    items: List<HomeItem>,
    payload: DragPayload,
    col: Int,
    row: Int,
    pointerCol: Int,
    pointerRow: Int,
    columns: Int,
    rows: Int,
    dockIndex: Int?,
): DropTarget {
    val element = (payload as? DragPayload.GridElement)?.item
    val isApp = element == null || element.kind == HomeItemKind.APP
    if (dockIndex != null) return if (isApp) DropTarget.Dock(dockIndex) else DropTarget.None

    if (isApp) {
        val under = itemAt(items, pointerCol, pointerRow, ignoreId = element?.id)
        if (under != null && under.isGroupable) return DropTarget.Merge(under)
    }
    val w = element?.w ?: 1
    val h = element?.h ?: 1
    return DropTarget.Cell(col, row, canPlace(items, col, row, w, h, columns, rows, ignoreId = element?.id))
}

/** Estado del arrastre en una cuadrícula. Las coordenadas son de la raíz de la ventana. */
@Stable
class GridDragController {
    var payload by mutableStateOf<DragPayload?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var drop by mutableStateOf<DropTarget>(DropTarget.None)
        private set
    var grabOffset = Offset.Zero
        private set
    var traveled = 0f
        private set

    /** Ajustados por la cuadrícula en cada composición. */
    var gridOrigin = Offset.Zero
    var cellWidth = 1f
    var cellHeight = 1f
    var columns = 4
    var rows = 7
    var items: List<HomeItem> = emptyList()
    var dockBounds: Rect? = null
    var dockSlots = 0

    val isDragging: Boolean get() = payload != null

    fun start(payload: DragPayload, pointer: Offset, grabOffset: Offset) {
        this.payload = payload
        this.pointer = pointer
        this.grabOffset = grabOffset
        traveled = 0f
        update()
    }

    fun move(delta: Offset) {
        pointer += delta
        traveled += delta.getDistance()
        update()
    }

    private fun update() {
        val current = payload ?: return
        val dock = dockBounds?.takeIf { it.contains(pointer) }
        val dockIndex = dock?.let {
            val slots = (dockSlots + 1).coerceAtLeast(1)
            floor((pointer.x - it.left) / it.width * slots).toInt().coerceIn(0, slots - 1)
        }
        val topLeft = pointer - grabOffset - gridOrigin
        val col = (topLeft.x / cellWidth).roundToInt()
        val row = (topLeft.y / cellHeight).roundToInt()
        val pointerCol = floor((pointer.x - gridOrigin.x) / cellWidth).toInt()
        val pointerRow = floor((pointer.y - gridOrigin.y) / cellHeight).toInt()
        drop = resolveDrop(items, current, col, row, pointerCol, pointerRow, columns, rows, dockIndex)
    }

    fun finish() {
        payload = null
        drop = DropTarget.None
    }
}
