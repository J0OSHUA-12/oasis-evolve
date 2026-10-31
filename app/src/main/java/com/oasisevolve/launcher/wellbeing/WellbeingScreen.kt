package com.oasisevolve.launcher.wellbeing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.storage.Note
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppIcon
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.ui.onWallpaper
import com.oasisevolve.launcher.usage.AppStats
import com.oasisevolve.launcher.usage.UsageOverview
import com.oasisevolve.launcher.usage.comparisonMessage
import com.oasisevolve.launcher.usage.formatDays
import com.oasisevolve.launcher.usage.formatDuration
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

private const val COLLAPSED_APPS = 6
private const val MINIMUM_RELEVANT_MS = 60_000L

@Composable
fun WellbeingScreen(state: LauncherState, viewModel: LauncherViewModel) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val hasAccess by viewModel.hasUsageAccess.collectAsStateWithLifecycle()
    val overview by viewModel.usage.collectAsStateWithLifecycle()
    var detailFor by remember { mutableStateOf<String?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }

    val labels = remember(state.allApps) { state.allApps.associateBy { it.packageName } }
    val usage = overview
    // Los servicios del sistema cuentan en el total, pero no son apps que se puedan abrir.
    val relevant = usage?.apps.orEmpty().filter {
        it.packageName in labels && (it.todayMs >= MINIMUM_RELEVANT_MS || it.last7dMs >= MINIMUM_RELEVANT_MS)
    }
    val shown = if (showAll) relevant else relevant.take(COLLAPSED_APPS)
    val longestToday = relevant.maxOfOrNull { it.todayMs }?.coerceAtLeast(1) ?: 1

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!hasAccess) {
                item(key = "access") { UsageAccessCard(viewModel) }
            } else if (usage == null) {
                item(key = "loading") {
                    GlassCard { Text("Calculando tu uso…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            } else {
                item(key = "total") { TotalCard(usage) }
                items(shown, key = { "app/${it.packageName}" }) { stats ->
                    AppUsageCard(
                        stats = stats,
                        app = labels[stats.packageName],
                        viewModel = viewModel,
                        longestToday = longestToday,
                        onOpen = { detailFor = stats.packageName },
                    )
                }
                if (relevant.size > COLLAPSED_APPS) {
                    item(key = "more") {
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(
                                if (showAll) "Ver menos" else "Ver las ${relevant.size} apps",
                                style = MaterialTheme.typography.labelLarge.onWallpaper(),
                            )
                        }
                    }
                }
            }
            item(key = "progress") { TimeProgressCard(state.settings) }
            item(key = "composer") { NoteComposer(viewModel) }
            items(notes, key = { "note/${it.id}" }) { note -> NoteCard(note, viewModel) }
        }

        val detailStats = detailFor?.let { pkg -> usage?.apps?.firstOrNull { it.packageName == pkg } }
        if (detailStats != null) {
            BackHandler { detailFor = null }
            AppUsageDetail(detailStats, labels[detailStats.packageName], viewModel, onClose = { detailFor = null })
        }
    }
}

@Composable
private fun UsageAccessCard(viewModel: LauncherViewModel) {
    GlassCard {
        Text("Uso de apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light)
        Text(
            "Android exige que permitas el «Acceso a datos de uso». Los datos se calculan en este teléfono y no salen de él: esta app no tiene permiso de Internet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = viewModel::openUsageAccess) { Text("Abrir ajustes de Android") }
    }
}

@Composable
private fun TotalCard(usage: UsageOverview) {
    val total = usage.total
    GlassCard {
        Text("Tiempo de pantalla hoy", style = MaterialTheme.typography.bodyLarge)
        Text(
            formatDuration(total.todayMs),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Light,
        )
        MetricRow("Últimas 24 horas", formatDuration(total.last24hMs))
        MetricRow("Últimos 7 días", formatDuration(total.last7dMs))
        MetricRow("Últimos 30 días", formatDuration(total.last30dMs))
        if (total.coverageDays in 1..6) {
            Muted("Android solo tiene datos de ${total.coverageDays} de los últimos 7 días.")
        }
        if (usage.coverage30d in 1..29) {
            Muted("Y de ${usage.coverage30d} de los últimos 30 días.")
        }
        ComparisonText(total)
    }
}

@Composable
private fun ComparisonText(stats: AppStats) {
    val previous = stats.previous7dMs
    val message = previous?.let { comparisonMessage(stats.last7dMs, it, "la semana anterior") }
    if (message != null) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    } else {
        Muted("Aún no hay una semana anterior completa con la que comparar.")
    }
}

@Composable
private fun AppUsageCard(
    stats: AppStats,
    app: AppEntry?,
    viewModel: LauncherViewModel,
    longestToday: Long,
    onOpen: () -> Unit,
) {
    val name = app?.label ?: stats.packageName.orEmpty()
    GlassCard(
        Modifier
            .clickable(onClick = onOpen)
            .semantics { contentDescription = "Estadísticas de $name" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            app?.let {
                AppIcon(it, viewModel, size = 40.dp)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatDuration(stats.todayMs),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
        )
        ProportionBar(stats.todayMs.toFloat() / longestToday)
        MetricRow("Aperturas hoy", timesText(stats.todayOpens))
        MetricRow(averageLabel(stats), formatDuration(stats.averageDailyMs))
        ProjectionLines(stats)
    }
}

private fun timesText(count: Int) = if (count == 1) "1 vez" else "$count veces"

private fun averageLabel(stats: AppStats): String {
    val days = if (stats.coverageDays == 1) "1 día" else "${stats.coverageDays} días"
    return "Promedio diario ($days)"
}

@Composable
private fun ProjectionLines(stats: AppStats) {
    val projection = stats.projection
    if (stats.averageDailyMs <= 0) return
    Muted("Si mantienes este promedio, como estimación:")
    MetricRow("En una semana", formatDuration(projection.weekMs))
    MetricRow("En 30 días", formatDuration(projection.monthMs))
    MetricRow("En un año", "unos ${formatDays(projection.yearInFullDays)} días completos")
}

@Composable
private fun AppUsageDetail(stats: AppStats, app: AppEntry?, viewModel: LauncherViewModel, onClose: () -> Unit) {
    val name = app?.label ?: stats.packageName.orEmpty()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            app?.let {
                AppIcon(it, viewModel, size = 44.dp)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text("Cerrar") }
        }
        GlassCard {
            MetricRow("Hoy", formatDuration(stats.todayMs))
            MetricRow("Aperturas hoy", timesText(stats.todayOpens))
            MetricRow("Últimas 24 horas", formatDuration(stats.last24hMs))
            MetricRow("Últimos 7 días", formatDuration(stats.last7dMs))
            MetricRow("Últimos 30 días", formatDuration(stats.last30dMs))
            MetricRow(averageLabel(stats), formatDuration(stats.averageDailyMs))
        }
        GlassCard {
            Text("Últimos 7 días", style = MaterialTheme.typography.titleMedium)
            DailyBars(stats.dailyMs)
        }
        GlassCard {
            Text("Comparación", style = MaterialTheme.typography.titleMedium)
            ComparisonText(stats)
            val previous30 = stats.previous30dMs
            val message30 = previous30?.let { comparisonMessage(stats.last30dMs, it, "los 30 días anteriores") }
            message30?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Muted("Mide solo tiempo de pantalla. No indica en qué se usó el tiempo que dejaste libre.")
        }
        GlassCard {
            Text("Proyección", style = MaterialTheme.typography.titleMedium)
            ProjectionLines(stats)
            if (stats.averageDailyMs <= 0) Muted("Sin uso reciente con el que estimar.")
        }
    }
}

/** Del día más antiguo a hoy. Barras discretas: solo muestran la forma de la semana. */
@Composable
private fun DailyBars(dailyMs: List<Long>) {
    val longest = dailyMs.maxOrNull()?.coerceAtLeast(1) ?: 1
    val today = LocalDate.now()
    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        dailyMs.forEachIndexed { index, ms ->
            val day = today.minusDays((dailyMs.lastIndex - index).toLong())
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { contentDescription = "$day: ${formatDuration(ms)}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((ms.toFloat() / longest * 0.8f).coerceAtLeast(0.02f))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), RoundedCornerShape(5.dp)),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProportionBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f), RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0.01f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Los cinco indicadores de tiempo. Se recalculan cada minuto con la hora real del teléfono. */
@Composable
private fun TimeProgressCard(settings: LauncherSettings) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }
    val weekStart = DayOfWeek.of(settings.weekStartIso.coerceIn(1, 7))
    val academic = settings.academicConfig()

    GlassCard {
        TimePeriod.entries.forEach { period ->
            PeriodRow(periodProgress(period, now, weekStart, academic))
        }
    }
}

private fun LauncherSettings.academicConfig(): AcademicSemester? {
    if (!academicSemester) return null
    val start = academicStartDay ?: return null
    val end = academicEndDay ?: return null
    return AcademicSemester(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(end)).takeIf { it.isValid }
}

@Composable
private fun PeriodRow(progress: PeriodProgress) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.semantics { contentDescription = "${progress.period.title}: ${progress.percent} %" },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(progress.period.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light)
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(colors.onSurface.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.fraction)
                    .fillMaxHeight()
                    .background(colors.onSurface, RoundedCornerShape(8.dp)),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${progress.percent} %", style = MaterialTheme.typography.bodyLarge)
            Muted(
                when {
                    progress.hasNotStarted -> "Aún no empieza"
                    progress.hasEnded -> "Terminado"
                    else -> "Pasaron ${describeSpan(progress.elapsed, progress.period)} · " +
                        "faltan ${describeSpan(progress.remaining, progress.period)}"
                },
            )
        }
    }
}

@Composable
private fun NoteComposer(viewModel: LauncherViewModel) {
    var text by rememberSaveable { mutableStateOf("") }
    GlassCard {
        Text("Notas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Escribe una nota rápida") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = text.isNotBlank(),
            onClick = {
                viewModel.addNote(text.trim())
                text = ""
            },
        ) { Text("Guardar") }
    }
}

@Composable
private fun NoteCard(note: Note, viewModel: LauncherViewModel) {
    val context = LocalContext.current
    var editing by remember(note.id) { mutableStateOf(false) }
    var draft by remember(note.id, note.text) { mutableStateOf(note.text) }

    GlassCard {
        if (editing) {
            OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth())
        } else {
            Text(note.text, style = MaterialTheme.typography.bodyLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (editing) {
                TextButton(enabled = draft.isNotBlank(), onClick = {
                    viewModel.editNote(note, draft.trim())
                    editing = false
                }) { Text("Guardar") }
                TextButton(onClick = {
                    draft = note.text
                    editing = false
                }) { Text("Cancelar") }
            } else {
                TextButton(onClick = { editing = true }) { Text("Editar") }
                TextButton(onClick = { copyToClipboard(context, note.text) }) { Text("Copiar") }
                TextButton(onClick = { shareText(context, note.text) }) { Text("Compartir") }
                TextButton(onClick = { viewModel.deleteNote(note) }) { Text("Borrar") }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("Nota", text))
    Toast.makeText(context, "Nota copiada", Toast.LENGTH_SHORT).show()
}

/** Abre el selector de Android. Obsidian aparece ahí si está instalado. */
private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "Compartir nota"))
}
