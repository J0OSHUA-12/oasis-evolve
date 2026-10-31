package com.oasisevolve.launcher.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.addCategory
import com.oasisevolve.launcher.storage.deleteCategory
import com.oasisevolve.launcher.storage.isBuiltInCategory
import com.oasisevolve.launcher.storage.renameCategory
import com.oasisevolve.launcher.storage.toggleCategoryHidden
import com.oasisevolve.launcher.ui.TextDialog

private const val NAME_ERROR = "Escribe un nombre que no use otra categoría"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorSheet(
    settings: LauncherSettings,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Categorías", style = MaterialTheme.typography.titleLarge)
            Text(
                "El interruptor muestra u oculta la categoría en la biblioteca. Sus apps siguen en el App Drawer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { adding = true }) { Text("Nueva categoría") }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(settings.categories, key = { it }) { name ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { renaming = name }) { Text("Renombrar") }
                        if (!settings.isBuiltInCategory(name)) {
                            TextButton(onClick = { viewModel.updateSettings { it.deleteCategory(name) } }) {
                                Text("Borrar")
                            }
                        }
                        Switch(
                            checked = name !in settings.hiddenCategories,
                            onCheckedChange = { viewModel.updateSettings { it.toggleCategoryHidden(name) } },
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        TextDialog(
            title = "Nueva categoría",
            onConfirm = { name ->
                if (settings.addCategory(name) == null) {
                    NAME_ERROR
                } else {
                    viewModel.updateSettings { it.addCategory(name) ?: it }
                    null
                }
            },
            onDismiss = { adding = false },
        )
    }
    renaming?.let { old ->
        TextDialog(
            title = "Renombrar categoría",
            initial = old,
            onConfirm = { name ->
                if (settings.renameCategory(old, name) == null) {
                    NAME_ERROR
                } else {
                    viewModel.updateSettings { it.renameCategory(old, name) ?: it }
                    null
                }
            },
            onDismiss = { renaming = null },
        )
    }
}
