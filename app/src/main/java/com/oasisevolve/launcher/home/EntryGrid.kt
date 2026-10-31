package com.oasisevolve.launcher.home

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.AppList
import com.oasisevolve.launcher.GridEntry
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppIcon
import com.oasisevolve.launcher.ui.AppTileContent
import com.oasisevolve.launcher.ui.LocalIconCorner
import com.oasisevolve.launcher.ui.LocalIconSize
import com.oasisevolve.launcher.ui.ReorderState
import com.oasisevolve.launcher.ui.TileLabel
import com.oasisevolve.launcher.ui.reorderable
import com.oasisevolve.launcher.ui.rememberReorderState

/**
 * Orden provisional de una lista mientras se arrastra. Solo se guarda al soltar, y se mantiene
 * hasta que el guardado llega al estado, para que los iconos no salten de sitio.
 */
class EntryDrag(
    val list: AppList,
    val prefix: String,
    val entries: List<GridEntry>,
    val reorder: ReorderState,
    private val commitOrder: () -> Unit,
) {
    fun commit() = commitOrder()
}

@Composable
fun rememberEntryDrag(
    list: AppList,
    prefix: String,
    state: LauncherState,
    viewModel: LauncherViewModel,
    grid: LazyGridState,
): EntryDrag {
    val saved = state.entries(list)
    val savedKeys = saved.map { it.key }
    var draft by remember(list) { mutableStateOf<List<String>?>(null) }

    // Terminó el guardado, o cambió el contenido de la lista: el borrador deja de valer.
    val current = draft
    if (current != null && (current == savedKeys || current.toSet() != savedKeys.toSet())) {
        draft = null
    }

    val byKey = saved.associateBy { it.key }
    val shown = draft?.mapNotNull(byKey::get) ?: saved

    val reorder = rememberReorderState(
        grid = grid,
        liveReorder = true,
        isTarget = { key -> key is String && key.startsWith(prefix) && key.removePrefix(prefix) in byKey },
        onMove = { from, to ->
            val order = (draft ?: savedKeys).toMutableList()
            val fromIndex = order.indexOf((from as String).removePrefix(prefix))
            val toIndex = order.indexOf((to as String).removePrefix(prefix))
            if (fromIndex >= 0 && toIndex >= 0) {
                order.add(toIndex, order.removeAt(fromIndex))
                draft = order
            }
        },
    )
    return EntryDrag(list, prefix, shown, reorder) {
        draft?.let { viewModel.setOrder(list, it) }
    }
}

fun LazyGridScope.entryItems(
    drag: EntryDrag,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onOpenFolder: (Long) -> Unit,
) {
    items(drag.entries, key = { drag.prefix + it.key }) { entry ->
        EntryTile(entry, drag, state, viewModel, onLongPress, onOpenFolder)
    }
}

@Composable
private fun LazyGridItemScope.EntryTile(
    entry: GridEntry,
    drag: EntryDrag,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onOpenFolder: (Long) -> Unit,
) {
    val settings = state.settings
    val key = drag.prefix + entry.key
    val isDragged = drag.reorder.draggingKey == key
    val placement = if (isDragged || settings.reduceMotion) null else spring<IntOffset>()
    val base = Modifier.animateItem(placementSpec = placement)

    when (entry) {
        is GridEntry.App -> AppTileContent(
            app = entry.app,
            viewModel = viewModel,
            showIcon = settings.showIcons,
            showLabel = settings.showLabels,
            modifier = base.reorderable(
                state = drag.reorder,
                key = key,
                label = entry.app.label,
                onTap = { viewModel.open(entry.app) },
                onLongPress = { onLongPress(entry.app) },
                onDrop = drag::commit,
            ),
        )

        is GridEntry.Folder -> FolderTile(
            folder = entry,
            viewModel = viewModel,
            showLabel = settings.showLabels,
            modifier = base.reorderable(
                state = drag.reorder,
                key = key,
                label = "carpeta ${entry.folder.name}",
                onTap = { onOpenFolder(entry.folder.id) },
                onLongPress = { onOpenFolder(entry.folder.id) },
                onDrop = drag::commit,
            ),
        )
    }
}

/** Carpeta como icono del mismo tamaño que una app, con hasta cuatro apps en miniatura. */
@Composable
fun FolderTile(
    folder: GridEntry.Folder,
    viewModel: LauncherViewModel,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val size = LocalIconSize.current
    val mini = (size - 12.dp) / 2
    val shape = RoundedCornerShape(size * LocalIconCorner.current)
    Column(
        modifier = modifier.padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                folder.apps.take(4).chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { AppIcon(it, viewModel, size = mini) }
                    }
                }
            }
        }
        if (showLabel) TileLabel(folder.folder.name)
    }
}
