package com.oasisevolve.launcher.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.AppList
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.drawer.filterBySearch
import com.oasisevolve.launcher.focus.AccessLevel
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppIcon
import com.oasisevolve.launcher.ui.SearchField
import com.oasisevolve.launcher.ui.TextDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: AppEntry,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    var chooseCategory by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    val currentFolder = state.folderOf(app)
    val otherFolders = state.settings.folders.filter { it.id != currentFolder?.id }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(app, viewModel, size = 40.dp)
                Text(app.label, style = MaterialTheme.typography.titleLarge)
            }

            SectionTitle("Colocar en")
            AppList.entries.forEach { list ->
                CheckRow(list.label, state.isIn(list, app)) { viewModel.toggle(list, app) }
            }
            Text(
                "Mantén pulsada una app y arrástrala para moverla, o suéltala sobre otra para crear una carpeta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Carpeta")
            currentFolder?.let { folder ->
                ActionRow("Sacar de «${folder.name}»") {
                    viewModel.removeFromFolder(folder.id, app)
                    onDismiss()
                }
            }
            otherFolders.forEach { folder ->
                ActionRow("Añadir a «${folder.name}»") {
                    viewModel.addToFolder(folder.id, app)
                    onDismiss()
                }
            }
            ActionRow("Crear carpeta nueva") { newFolder = true }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Dificultad de acceso")
            AccessLevel.entries.forEach { level ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setLevel(app, level) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = state.level(app) == level, onClick = { viewModel.setLevel(app, level) })
                    Column {
                        Text(level.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            level.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                "Solo afecta a este launcher. La app sigue visible en Ajustes de Android y en otros launchers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ActionRow("Categoría: ${state.category(app)}") { chooseCategory = !chooseCategory }
            if (chooseCategory) {
                state.settings.categories.forEach { category ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setCategory(app, category)
                                chooseCategory = false
                            }
                            .padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.category(app) == category, onClick = null)
                        Text(category, Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
                    }
                }
            }
            ActionRow("Información de la app") {
                viewModel.openAppInfo(app)
                onDismiss()
            }
            ActionRow("Desinstalar") {
                viewModel.uninstall(app)
                onDismiss()
            }
        }
    }

    if (newFolder) {
        TextDialog(
            title = "Nombre de la carpeta",
            onConfirm = { name ->
                if (name.isBlank()) {
                    "Escribe un nombre"
                } else {
                    viewModel.createFolder(app, name.trim())
                    onDismiss()
                    null
                }
            },
            onDismiss = { newFolder = false },
        )
    }
}

/** Opciones de un icono del dock: reemplazarlo, quitarlo o moverlo sin perder su contenedor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockIconSheet(
    app: AppEntry,
    viewModel: LauncherViewModel,
    onReplace: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(app, viewModel, size = 40.dp)
                Text(app.label, style = MaterialTheme.typography.titleLarge)
            }
            ActionRow("Reemplazar") {
                onDismiss()
                onReplace()
            }
            ActionRow("Mover a la izquierda") { viewModel.moveInDock(app, -1) }
            ActionRow("Mover a la derecha") { viewModel.moveInDock(app, 1) }
            ActionRow("Quitar del dock") {
                viewModel.removeFromDock(app)
                onDismiss()
            }
            ActionRow("Información de la app") {
                viewModel.openAppInfo(app)
                onDismiss()
            }
        }
    }
}

/** Selector de apps. Sirve para colocar varias, para una carpeta o para reemplazar un icono del dock. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    title: String,
    state: LauncherState,
    viewModel: LauncherViewModel,
    isSelected: (AppEntry) -> Boolean,
    onToggle: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = filterBySearch(state.allApps.filter { state.level(it).inVisualScreens }, query) { it.label }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            SearchField(query, onQueryChange = { query = it }, placeholder = "Buscar apps")
            LazyColumn(Modifier.fillMaxWidth()) {
                items(results, key = { it.key }) { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(app) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppIcon(app, viewModel, size = 36.dp)
                        Text(app.label, Modifier.weight(1f))
                        Checkbox(checked = isSelected(app), onCheckedChange = { onToggle(app) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
