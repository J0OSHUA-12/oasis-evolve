package com.oasisevolve.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.oasisevolve.launcher.ui.AppTile
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.ui.TextDialog

@Composable
fun FolderOverlay(
    folder: GridEntry.Folder,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                items(folder.apps, key = { it.key }) { app ->
                    AppTile(
                        app = app,
                        viewModel = viewModel,
                        showIcon = state.settings.showIcons,
                        showLabel = true,
                        onLongPress = onLongPress,
                        onSurface = true,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { renaming = true }) { Text("Renombrar") }
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.folder.id)
                    onDismiss()
                }) { Text("Deshacer carpeta") }
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
