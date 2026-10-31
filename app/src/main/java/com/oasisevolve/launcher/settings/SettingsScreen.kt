package com.oasisevolve.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.ThemeMode
import com.oasisevolve.launcher.system.defaultLauncherIntent
import com.oasisevolve.launcher.system.isDefaultLauncher

@Composable
fun SettingsScreen(
    state: LauncherState,
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val settings = state.settings
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsStateWithLifecycle()
    val isDefault = isDefaultLauncher(context)
    fun update(change: (LauncherSettings) -> LauncherSettings) = viewModel.updateSettings(change)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
            TextButton(onClick = onClose) { Text("Cerrar") }
        }

        Section("Sistema")
        Text(
            if (isDefault) "Oasis Evolve es tu launcher predeterminado." else "Oasis Evolve aún no es tu launcher predeterminado.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!isDefault) {
            Button(onClick = { context.startActivity(defaultLauncherIntent(context)) }) {
                Text("Establecer como predeterminado")
            }
        }
        Text(
            if (hasUsageAccess) "Acceso a datos de uso concedido." else "Acceso a datos de uso sin conceder. Es necesario para Bienestar.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (!hasUsageAccess) {
            Button(onClick = viewModel::openUsageAccess) { Text("Conceder acceso") }
        }

        Section("Pantalla principal")
        Toggle("Reloj y fecha", settings.showClock) { v -> update { it.copy(showClock = v) } }
        Toggle("Dock inferior", settings.showDock) { v -> update { it.copy(showDock = v) } }
        Toggle("Mostrar iconos", settings.showIcons) { v -> update { it.copy(showIcons = v) } }
        Toggle("Mostrar nombres bajo los iconos", settings.showLabels) { v -> update { it.copy(showLabels = v) } }
        Options("Columnas", listOf(3, 4, 5), settings.gridColumns, { it.toString() }) { v ->
            update { it.copy(gridColumns = v) }
        }
        Hint("El App Drawer siempre es solo texto, aunque los iconos estén activados.")

        Section("Biblioteca")
        Toggle("Pantalla de biblioteca de apps", settings.libraryEnabled) { v ->
            update { it.copy(libraryEnabled = v) }
        }

        Section("Interrupciones")
        Options("Espera antes de abrir", listOf(5, 10, 15, 30, 60), settings.waitSeconds, { "$it s" }) { v ->
            update { it.copy(waitSeconds = v) }
        }
        Hint("Se aplica a las apps de nivel 4 cuando las abres desde este launcher. Aperturas desde notificaciones u otras apps no pasan por aquí.")
        val guarded = state.allApps.filter { state.level(it).level >= 2 }
        if (guarded.isEmpty()) {
            Hint("No hay apps restringidas. Mantén pulsada una app para cambiar su nivel.")
        } else {
            guarded.forEach { app ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(app.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Nivel ${state.level(app).level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Section("Apariencia")
        Options(
            "Tema",
            ThemeMode.entries,
            settings.themeMode,
            {
                when (it) {
                    ThemeMode.SYSTEM -> "Sistema"
                    ThemeMode.LIGHT -> "Claro"
                    ThemeMode.DARK -> "Oscuro"
                }
            },
        ) { v -> update { it.copy(themeMode = v) } }
        Toggle("Reducir animaciones", settings.reduceMotion) { v -> update { it.copy(reduceMotion = v) } }
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(Modifier.padding(top = 14.dp, bottom = 8.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> Options(
    label: String,
    options: List<T>,
    selected: T,
    text: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(text(option)) },
                )
            }
        }
    }
}
