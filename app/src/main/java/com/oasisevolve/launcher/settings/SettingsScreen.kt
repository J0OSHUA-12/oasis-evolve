package com.oasisevolve.launcher.settings

import android.content.Intent
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.home.AppPickerSheet
import com.oasisevolve.launcher.quiet.QuietLog
import com.oasisevolve.launcher.quiet.QuietWindow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.time.Instant
import java.time.ZoneId
import com.oasisevolve.launcher.storage.Accent
import com.oasisevolve.launcher.storage.AppFont
import com.oasisevolve.launcher.storage.IconShape
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.ThemeMode
import com.oasisevolve.launcher.system.defaultLauncherIntent
import com.oasisevolve.launcher.system.isDefaultLauncher
import com.oasisevolve.launcher.ui.darkColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val SettingsBackground = Color(0xFF1C1E1F)
private val MutedText = Color(0xFFB4B6B8)
private const val PENDING = "Disponible en una próxima versión"
private const val DAY_MILLIS = 86_400_000L

/**
 * Lista sobria inspirada en la referencia: fondo oscuro, tipografía fina, mucho aire, secciones
 * desplegables e interruptores de contorno. Siempre oscura, sea cual sea el tema del launcher.
 */
@Composable
fun SettingsScreen(
    state: LauncherState,
    viewModel: LauncherViewModel,
    onEditHome: () -> Unit,
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
    var confirmReset by remember { mutableStateOf(false) }
    var pickQuietApps by remember { mutableStateOf(false) }
    val guardEnabled by viewModel.guardEnabled.collectAsStateWithLifecycle()
    val guardStalled by viewModel.guardStalled.collectAsStateWithLifecycle()
    val quietEnabled by viewModel.quietEnabled.collectAsStateWithLifecycle()
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importBackup)
    }
    fun update(change: (LauncherSettings) -> LauncherSettings) = viewModel.updateSettings(change)

    if (pickQuietApps) {
        AppPickerSheet(
            title = "Notificaciones a posponer",
            state = state,
            viewModel = viewModel,
            isSelected = { it.packageName in settings.quietApps },
            onToggle = viewModel::toggleQuiet,
            onDismiss = { pickQuietApps = false },
            includeHidden = true,
        )
    }

    MaterialTheme(colorScheme = darkColors(settings.accent), typography = MaterialTheme.typography) {
        Surface(color = SettingsBackground, contentColor = Color.White, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                    Text(
                        "Ajustes",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Ajustes de Android", tint = Color.White)
                    }
                }

                // Solo aparece si falta algo que el launcher necesita.
                if (!isDefault || !hasUsageAccess) {
                    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!isDefault) {
                            ActionRow("Establecer como launcher predeterminado", "Oasis Evolve aún no lo es") {
                                context.startActivity(defaultLauncherIntent(context))
                            }
                        }
                        if (!hasUsageAccess) {
                            ActionRow("Conceder acceso a datos de uso", "Lo necesitan Bienestar y las interrupciones") {
                                viewModel.openUsageAccess()
                            }
                        }
                    }
                }

                StaticRow("Focus Mode", "Una interfaz mínima para eliminar distracciones. $PENDING")
                Section("Notification Filter") {
                    Hint("Las notificaciones de las apps que elijas se posponen y vuelven a aparecer en tu franja, por ejemplo antes de dormir. Se usa la función de posponer de Android: Oasis Evolve no lee ni guarda su texto.")
                    if (!quietEnabled) {
                        ActionRow("Conceder acceso a notificaciones", "Lo necesita el filtro para posponerlas") {
                            viewModel.openQuietSettings()
                        }
                    }
                    val quietApps = state.allApps.filter { it.packageName in settings.quietApps }.distinctBy { it.packageName }
                    ActionRow(
                        "Elegir apps",
                        if (quietApps.isEmpty()) "Ninguna elegida" else quietApps.joinToString { it.label },
                    ) { pickQuietApps = true }
                    Options(
                        "Puedes verlas desde las",
                        listOf(19, 20, 21, 22, 23).map { it * 60 },
                        settings.quietStartMinute,
                        { "%02d:00".format(it / 60) },
                    ) { v -> update { it.copy(quietStartMinute = v) } }
                    Options("Durante", listOf(30, 60, 120), settings.quietLengthMinutes, { if (it < 60) "$it min" else "${it / 60} h" }) { v ->
                        update { it.copy(quietLengthMinutes = v) }
                    }
                    val window = QuietWindow(settings.quietStartMinute, settings.quietLengthMinutes)
                    val waiting = remember(quietEnabled, settings.quietApps) {
                        QuietLog.waiting(context, window.nextOpening(Instant.now(), ZoneId.systemDefault()))
                    }
                    if (waiting.isNotEmpty()) {
                        val labels = state.allApps.associate { it.packageName to it.label }
                        Hint("Esperando: " + waiting.entries.joinToString { (pkg, count) -> "${labels[pkg] ?: pkg} $count" })
                    }
                    Hint("Las que no se pueden descartar, como música, llamadas o descargas, no se tocan. Si reinicias el teléfono, Android puede soltar las pospuestas antes de tiempo.")
                }
                Toggle("Show App Icons", settings.showIcons) { v -> update { it.copy(showIcons = v) } }

                Section("Customization") {
                    Toggle("Immersive Mode", settings.immersiveMode, "Oculta la barra superior dentro del launcher") { v ->
                        update { it.copy(immersiveMode = v) }
                    }
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
                    Options(
                        "Tamaño de iconos",
                        listOf(48, 56, 64, 72),
                        settings.iconSizeDp,
                        { mapOf(48 to "S", 56 to "M", 64 to "L", 72 to "XL").getValue(it) },
                    ) { v -> update { it.copy(iconSizeDp = v) } }
                    Options("Forma de iconos", IconShape.entries, settings.iconShape, { it.label }) { v ->
                        update { it.copy(iconShape = v) }
                    }
                    Options(
                        "Paquete de iconos",
                        listOf(null) + iconPacks.map { it.packageName },
                        settings.iconPack?.takeIf { pack -> iconPacks.any { it.packageName == pack } },
                        { pack -> iconPacks.firstOrNull { it.packageName == pack }?.label ?: "Ninguno" },
                    ) { v -> update { it.copy(iconPack = v) } }
                    Toggle("Reducir animaciones", settings.reduceMotion) { v -> update { it.copy(reduceMotion = v) } }
                    Toggle(
                        "Cristal esmerilado (experimental)",
                        settings.glassBlur,
                        if (blurAvailable) "Intenta desenfocar el fondo fuera de Inicio. Puede no notarse y gasta más batería."
                        else "Este dispositivo no lo permite ahora mismo",
                    ) { v -> update { it.copy(glassBlur = v) } }
                }

                Section("Home Screen") {
                    ActionRow("Editar la pantalla", "Mover, añadir y quitar apps y widgets") { onEditHome() }
                    Toggle("Dock inferior", settings.showDock) { v -> update { it.copy(showDock = v) } }
                    Toggle("Nombres bajo los iconos", settings.showLabels) { v -> update { it.copy(showLabels = v) } }
                    Options("Columnas", listOf(3, 4, 5), settings.gridColumns, { it.toString() }) { v ->
                        update { it.copy(gridColumns = v) }
                    }
                    Options("Filas", listOf(6, 7, 8), settings.gridRows, { it.toString() }) { v ->
                        update { it.copy(gridRows = v) }
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
                    ActionRow("Restaurar distribución inicial", "Vuelve a la composición de referencia") {
                        confirmReset = true
                    }
                }

                Section("App Drawer") {
                    Toggle("Biblioteca de apps", settings.libraryEnabled, "Última pantalla a la derecha") { v ->
                        update { it.copy(libraryEnabled = v) }
                    }
                    Toggle("Biblioteca expandida", settings.libraryExpanded) { v ->
                        update { it.copy(libraryExpanded = v) }
                    }
                    val restricted = state.allApps.filter { state.level(it).level >= 2 }
                    Text("Apps ocultas y restringidas", fontSize = 18.sp, fontWeight = FontWeight.Light)
                    if (restricted.isEmpty()) {
                        Hint("Ninguna. Mantén pulsada una app para cambiar su nivel de acceso.")
                    } else {
                        restricted.forEach { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(app.label, fontWeight = FontWeight.Light)
                                Text("Nivel ${state.level(app).level}", color = MutedText)
                            }
                        }
                    }
                }

                Section("Interrupts") {
                    Options("Espera antes de abrir", listOf(5, 10, 15, 30, 60), settings.waitSeconds, { "$it s" }) { v ->
                        update { it.copy(waitSeconds = v) }
                    }
                    Hint("Se aplica a las apps de nivel 4 cuando las abres desde este launcher.")
                    Toggle(
                        "Bloqueo fuera del launcher",
                        guardEnabled,
                        if (guardEnabled) {
                            "Activo: también frena al volver desde Recientes o una notificación, y cierra la app al acabar el temporizador"
                        } else {
                            "Desactivado: Recientes y notificaciones se saltan la interrupción, y el temporizador no puede cerrar la app"
                        },
                    ) { viewModel.openGuardSettings() }
                    if (guardStalled) {
                        ActionRow(
                            "El bloqueo está desconectado",
                            "Android lo activó pero no lo está ejecutando. Apágalo y vuelve a encenderlo en Accesibilidad.",
                        ) { viewModel.openGuardSettings() }
                    }
                    Hint("Se activa en Ajustes de Android > Accesibilidad > Oasis Evolve. Solo recibe qué app se abre, y solo de las de nivel 4 o con temporizador. No lee la pantalla. Si sales de la app más de un minuto, al volver pasas otra vez por la interrupción.")
                    val timed = state.allApps.filter { it.key in settings.appTimers }
                    if (timed.isNotEmpty()) {
                        Hint("Temporizadores: " + timed.joinToString { "${it.label} ${settings.appTimers.getValue(it.key)} min" })
                    }
                    Hint("Para poner un temporizador, mantén pulsada una app y elige su duración.")
                }

                StaticRow("Focus Mode Settings", PENDING)

                Section("Font") {
                    Options("Tipografía", AppFont.entries, settings.font, { it.label }) { v ->
                        update { it.copy(font = v) }
                    }
                    Options(
                        "Tamaño de texto",
                        listOf(0.9f, 1f, 1.1f, 1.2f),
                        settings.fontScale,
                        { mapOf(0.9f to "Pequeño", 1f to "Normal", 1.1f to "Grande", 1.2f to "Muy grande").getValue(it) },
                    ) { v -> update { it.copy(fontScale = v) } }
                }

                StaticRow("Gestures", PENDING)

                Section("Copia de seguridad") {
                    Hint("Guarda en un archivo tus pantallas, carpetas, dock, ajustes y notas. No incluye las fotos del widget ni los widgets de otras apps.")
                    ActionRow("Exportar copia", "Elige dónde guardar el archivo") {
                        exportFile.launch("oasis-evolve-${LocalDate.now()}.json")
                    }
                    ActionRow("Importar copia", "Sustituye la configuración actual y las notas") {
                        importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }
                }

                Section("Bienestar") {
                    Options(
                        "La semana empieza el",
                        listOf(1, 7, 6),
                        settings.weekStartIso,
                        { mapOf(1 to "Lunes", 7 to "Domingo", 6 to "Sábado").getValue(it) },
                    ) { v -> update { it.copy(weekStartIso = v) } }
                    Options(
                        "Semestre",
                        listOf(false, true),
                        settings.academicSemester,
                        { if (it) "Académico" else "Calendario" },
                    ) { v -> update { it.copy(academicSemester = v) } }
                    if (settings.academicSemester) {
                        DateRow("Inicio del semestre", settings.academicStartDay) { day ->
                            update { it.copy(academicStartDay = day) }
                        }
                        DateRow("Fin del semestre", settings.academicEndDay) { day ->
                            update { it.copy(academicEndDay = day) }
                        }
                        val start = settings.academicStartDay
                        val end = settings.academicEndDay
                        if (start == null || end == null) {
                            Hint("Elige las dos fechas. Mientras tanto se usa el semestre calendario.")
                        } else if (end <= start) {
                            Hint("El final debe ser posterior al inicio. Mientras tanto se usa el semestre calendario.")
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Restaurar la distribución inicial?") },
            text = { Text("La pantalla principal vuelve a la composición de referencia. Tus apps y widgets actuales se conservan en los huecos libres. Lo que no quepa pasa a Apps útiles o a la pantalla Widgets.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreDefaultComposition()
                    confirmReset = false
                    onClose()
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

/** Sección desplegable con flecha, como en la referencia. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "arrow")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .padding(vertical = 20.dp)
            .semantics { role = Role.Button },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Light)
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = if (open) "Plegar" else "Desplegar",
            tint = Color.White,
            modifier = Modifier.rotate(rotation),
        )
    }
    AnimatedVisibility(
        visible = open,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(
            Modifier.padding(start = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/** Función aún sin implementar: se muestra sin fingir que funciona. */
@Composable
private fun StaticRow(title: String, description: String) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Light)
        Text(description, color = MutedText, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ActionRow(title: String, description: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.primary)
        Text(description, color = MutedText, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = MutedText, fontWeight = FontWeight.Light, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Toggle(label: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Light)
            description?.let { Text(it, color = MutedText, fontWeight = FontWeight.Light) }
        }
        // Interruptor de contorno: sin color de acento, como en la referencia.
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.Transparent,
                checkedBorderColor = Color.White,
                uncheckedThumbColor = MutedText,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = MutedText,
            ),
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Light)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val shape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .then(if (isSelected) Modifier.background(Color.White, shape) else Modifier)
                        .border(BorderStroke(1.dp, if (isSelected) Color.White else MutedText), shape)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .semantics { role = Role.RadioButton },
                ) {
                    Text(
                        text(option),
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Light,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(label: String, epochDay: Long?, onSelect: (Long) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val text = epochDay?.let {
        LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    } ?: "Sin elegir"
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { picking = true }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Light)
        Text(text, color = MaterialTheme.colorScheme.primary)
    }
    if (picking) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = epochDay?.times(DAY_MILLIS))
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onSelect(it / DAY_MILLIS) }
                    picking = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
