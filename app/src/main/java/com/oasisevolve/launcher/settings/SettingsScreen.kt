package com.oasisevolve.launcher.settings

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.Accent
import com.oasisevolve.launcher.storage.AppFont
import com.oasisevolve.launcher.storage.IconShape
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
    val iconPacks = remember { viewModel.installedIconPacks() }
    val blurAvailable = remember {
        Build.VERSION.SDK_INT >= 31 &&
            context.getSystemService(WindowManager::class.java).isCrossWindowBlurEnabled
    }
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
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
            TextButton(onClick = onClose) { Text("Cerrar") }
        }

        Section("Sistema", initiallyOpen = !isDefault || !hasUsageAccess) {
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
        }

        Section("Pantalla principal") {
            Toggle("Reloj y fecha", settings.showClock) { v -> update { it.copy(showClock = v) } }
            Toggle("Dock inferior", settings.showDock) { v -> update { it.copy(showDock = v) } }
            Toggle("Widgets en inicio", settings.showHomeWidgets) { v -> update { it.copy(showHomeWidgets = v) } }
            Options("Columnas", listOf(3, 4, 5), settings.gridColumns, { it.toString() }) { v ->
                update { it.copy(gridColumns = v) }
            }
            Options(
                "Espaciado",
                listOf(6, 12, 20),
                settings.gridSpacingDp,
                { mapOf(6 to "Compacto", 12 to "Normal", 20 to "Amplio").getValue(it) },
            ) { v -> update { it.copy(gridSpacingDp = v) } }
            Options(
                "Márgenes",
                listOf(12, 20, 32),
                settings.pageMarginDp,
                { mapOf(12 to "Estrechos", 20 to "Normales", 32 to "Anchos").getValue(it) },
            ) { v -> update { it.copy(pageMarginDp = v) } }
        }

        Section("Iconos") {
            Toggle("Mostrar iconos", settings.showIcons) { v -> update { it.copy(showIcons = v) } }
            Toggle("Mostrar nombres", settings.showLabels) { v -> update { it.copy(showLabels = v) } }
            Options(
                "Tamaño",
                listOf(48, 56, 64, 72),
                settings.iconSizeDp,
                { mapOf(48 to "S", 56 to "M", 64 to "L", 72 to "XL").getValue(it) },
            ) { v -> update { it.copy(iconSizeDp = v) } }
            Options("Forma", IconShape.entries, settings.iconShape, { it.label }) { v ->
                update { it.copy(iconShape = v) }
            }
            Options(
                "Paquete de iconos",
                listOf(null) + iconPacks.map { it.packageName },
                settings.iconPack?.takeIf { pack -> iconPacks.any { it.packageName == pack } },
                { pack -> iconPacks.firstOrNull { it.packageName == pack }?.label ?: "Ninguno" },
            ) { v -> update { it.copy(iconPack = v) } }
            Hint(
                if (iconPacks.isEmpty()) "No hay paquetes instalados. Admite los que usan el formato habitual de los launchers."
                else "Las apps que el paquete no incluya mantienen su icono original.",
            )
            Hint("El App Drawer siempre es solo texto, aunque los iconos estén activados.")
        }

        Section("Biblioteca") {
            Toggle("Pantalla de biblioteca de apps", settings.libraryEnabled) { v ->
                update { it.copy(libraryEnabled = v) }
            }
            Toggle("Vista expandida", settings.libraryExpanded) { v -> update { it.copy(libraryExpanded = v) } }
        }

        Section("Interrupciones") {
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
        }

        Section("Apariencia") {
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
            Options("Color de acento", Accent.entries, settings.accent, { it.label }) { v ->
                update { it.copy(accent = v) }
            }
            Options("Tipografía", AppFont.entries, settings.font, { it.label }) { v ->
                update { it.copy(font = v) }
            }
            Options(
                "Tamaño de texto",
                listOf(0.9f, 1f, 1.1f, 1.2f),
                settings.fontScale,
                { mapOf(0.9f to "Pequeño", 1f to "Normal", 1.1f to "Grande", 1.2f to "Muy grande").getValue(it) },
            ) { v -> update { it.copy(fontScale = v) } }
            Toggle("Reducir animaciones", settings.reduceMotion) { v -> update { it.copy(reduceMotion = v) } }
            Toggle("Cristal esmerilado (experimental)", settings.glassBlur) { v ->
                update { it.copy(glassBlur = v) }
            }
            Hint(
                if (blurAvailable) "Intenta desenfocar el fondo fuera de la pantalla principal. Según el teléfono puede no notarse, y consume algo más de batería."
                else "Este dispositivo no permite el desenfoque ahora mismo. Puede estar desactivado por el ahorro de batería o no estar disponible.",
            )
        }
    }
}

/** Sección plegable, para que los ajustes no sean una lista interminable. */
@Composable
private fun Section(
    title: String,
    initiallyOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    HorizontalDivider(Modifier.padding(top = 4.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(if (open) "−" else "+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
    if (open) {
        Column(
            Modifier.padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
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

@OptIn(ExperimentalLayoutApi::class)
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
