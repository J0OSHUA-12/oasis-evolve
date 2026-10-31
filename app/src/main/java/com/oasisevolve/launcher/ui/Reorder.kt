package com.oasisevolve.launcher.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex

/**
 * Arrastre sobre una cuadrícula perezosa. Con [liveReorder], los elementos se reordenan mientras se
 * arrastra; sin él, solo se informa de qué elemento hay debajo, para soltar sobre un destino.
 */
@Stable
class ReorderState(
    private val grid: LazyGridState,
    private val liveReorder: Boolean,
    private val isTarget: (Any) -> Boolean,
    private val onMove: (Any, Any) -> Unit,
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var hoverKey by mutableStateOf<Any?>(null)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var traveled = 0f
        private set

    fun start(key: Any) {
        draggingKey = key
        hoverKey = null
        offset = Offset.Zero
        traveled = 0f
    }

    fun drag(delta: Offset) {
        val key = draggingKey ?: return
        offset += delta
        traveled += delta.getDistance()
        val items = grid.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.key == key } ?: return
        val center = Offset(
            current.offset.x + current.size.width / 2f + offset.x,
            current.offset.y + current.size.height / 2f + offset.y,
        )
        val target = items.firstOrNull { item ->
            item.key != key && isTarget(item.key) &&
                center.x >= item.offset.x && center.x < item.offset.x + item.size.width &&
                center.y >= item.offset.y && center.y < item.offset.y + item.size.height
        }
        hoverKey = target?.key
        if (liveReorder && target != null) {
            onMove(key, target.key)
            // El elemento salta al hueco del destino: se compensa para que siga bajo el dedo.
            offset += Offset(
                (current.offset.x - target.offset.x).toFloat(),
                (current.offset.y - target.offset.y).toFloat(),
            )
        }
    }

    fun finish() {
        draggingKey = null
        hoverKey = null
        offset = Offset.Zero
    }
}

@Composable
fun rememberReorderState(
    grid: LazyGridState,
    liveReorder: Boolean,
    isTarget: (Any) -> Boolean,
    onMove: (Any, Any) -> Unit = { _, _ -> },
): ReorderState {
    val currentIsTarget by rememberUpdatedState(isTarget)
    val currentOnMove by rememberUpdatedState(onMove)
    return remember(grid, liveReorder) {
        ReorderState(grid, liveReorder, { currentIsTarget(it) }, { from, to -> currentOnMove(from, to) })
    }
}

/**
 * Tocar abre, mantener pulsado sin mover muestra opciones y mantener pulsado y mover arrastra.
 * Sustituye a `combinedClickable`, que no admite arrastre tras la pulsación larga.
 */
@Composable
fun Modifier.reorderable(
    state: ReorderState,
    key: Any,
    label: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDrop: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val slop = LocalViewConfiguration.current.touchSlop
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(onLongPress)
    val drop by rememberUpdatedState(onDrop)
    val longPressed = remember { booleanArrayOf(false) }
    val dragging = state.draggingKey == key

    return this
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            if (dragging) {
                translationX = state.offset.x
                translationY = state.offset.y
                alpha = 0.92f
            }
        }
        .scale(if (dragging) 1.1f else 1f)
        .semantics {
            onClick(label = "Abrir $label") {
                tap()
                true
            }
            onLongClick(label = "Opciones de $label") {
                longPress()
                true
            }
        }
        .pointerInput(key) {
            detectTapGestures(
                onPress = { longPressed[0] = false },
                onTap = { if (!longPressed[0]) tap() },
            )
        }
        .pointerInput(key) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    longPressed[0] = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.start(key)
                },
                onDrag = { change, delta ->
                    change.consume()
                    state.drag(delta)
                },
                onDragEnd = {
                    val moved = state.traveled > slop
                    if (moved) drop() else longPress()
                    state.finish()
                },
                onDragCancel = { state.finish() },
            )
        }
}
