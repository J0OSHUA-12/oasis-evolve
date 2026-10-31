package com.oasisevolve.launcher.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.GridEntry
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppTileContent
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.ui.TextDialog
import com.oasisevolve.launcher.ui.reorderable
import com.oasisevolve.launcher.ui.rememberReorderState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderOverlay(
    folder: GridEntry.Folder,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onAddApps: () -> Unit,
    onDismiss: () -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    var draft by remember(folder.folder.id) { mutableStateOf<List<String>?>(null) }
    val saved = folder.apps.map { it.key }
    val current = draft
    if (current != null && (current == saved || current.toSet() != saved.toSet())) draft = null
    val byKey = folder.apps.associateBy { it.key }
    val shown = draft?.mapNotNull(byKey::get) ?: folder.apps

    val grid = rememberLazyGridState()
    val reorder = rememberReorderState(
        grid = grid,
        liveReorder = true,
        isTarget = { it is String && it in byKey },
        onMove = { from, to ->
            val order = (draft ?: saved).toMutableList()
            val fromIndex = order.indexOf(from as String)
            val toIndex = order.indexOf(to as String)
            if (fromIndex >= 0 && toIndex >= 0) {
                order.add(toIndex, order.removeAt(fromIndex))
                draft = order
            }
        },
    )
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visibleState = appear,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
        ) {
            GlassCard(
                Modifier
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Text(
                    folder.folder.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = grid,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    items(shown, key = { it.key }) { app ->
                        AppTileContent(
                            app = app,
                            viewModel = viewModel,
                            showIcon = state.settings.showIcons,
                            showLabel = true,
                            onSurface = true,
                            modifier = Modifier
                                .animateItem()
                                .reorderable(
                                    state = reorder,
                                    key = app.key,
                                    label = app.label,
                                    onTap = { viewModel.open(app) },
                                    onLongPress = { onLongPress(app) },
                                    onDrop = { draft?.let { viewModel.setFolderOrder(folder.folder.id, it) } },
                                ),
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onAddApps) { Text("Añadir apps") }
                    TextButton(onClick = { renaming = true }) { Text("Renombrar") }
                    TextButton(onClick = {
                        viewModel.deleteFolder(folder.folder.id)
                        onDismiss()
                    }) { Text("Deshacer carpeta") }
                }
            }
        }
    }

    if (renaming) {
        TextDialog(
            title = "Renombrar carpeta",
            initial = folder.folder.name,
            onConfirm = { name ->
                if (name.isBlank()) {
                    "Escribe un nombre"
                } else {
                    viewModel.renameFolder(folder.folder.id, name.trim())
                    null
                }
            },
            onDismiss = { renaming = false },
        )
    }
}
