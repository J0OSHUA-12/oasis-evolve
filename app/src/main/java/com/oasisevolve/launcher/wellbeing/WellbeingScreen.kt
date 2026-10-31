package com.oasisevolve.launcher.wellbeing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.Note
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.ui.onWallpaper
import com.oasisevolve.launcher.usage.UsageReport
import com.oasisevolve.launcher.usage.UsageWindow
import com.oasisevolve.launcher.usage.formatDuration
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun WellbeingScreen(viewModel: LauncherViewModel) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Bienestar",
                style = MaterialTheme.typography.headlineMedium.onWallpaper(),
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        item { AppUsageCard(viewModel) }
        item { ProgressCard() }
        item { NoteComposer(viewModel) }
        items(notes, key = { it.id }) { note -> NoteCard(note, viewModel) }
        item { Box(Modifier.height(28.dp)) }
    }
}

@Composable
private fun AppUsageCard(viewModel: LauncherViewModel) {
    val hasAccess by viewModel.hasUsageAccess.collectAsStateWithLifecycle()
    val window by viewModel.usageWindow.collectAsStateWithLifecycle()
    val report by viewModel.usageReport.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    GlassCard {
        Text("Uso de apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light)
        if (!hasAccess) {
            Text(
                "Android exige que permitas el «Acceso a datos de uso». Los datos se calculan en este teléfono y no salen de él: esta app no tiene permiso de Internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = viewModel::openUsageAccess) { Text("Abrir ajustes de Android") }
            return@GlassCard
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageWindow.entries.forEach { option ->
                FilterChip(
                    selected = option == window,
                    onClick = { viewModel.selectUsageWindow(option) },
                    label = { Text(option.label) },
                )
            }
        }

        val current = report
        if (current == null) {
            Text("Calculando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@GlassCard
        }
        UsageSummaryHeader(current)

        val labels = state.allApps.associate { it.packageName to it.label }
        val ranking = current.summary.perApp.entries
            .filter { it.value.foregroundMs >= 60_000 }
            .sortedByDescending { it.value.foregroundMs }
            .take(8)
        val longest = ranking.firstOrNull()?.value?.foregroundMs ?: 1L
        ranking.forEach { (pkg, usage) ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(labels[pkg] ?: pkg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${formatDuration(usage.foregroundMs)} · ${usage.opens} ${if (usage.opens == 1) "vez" else "veces"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(usage.foregroundMs.toFloat() / longest)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageSummaryHeader(report: UsageReport) {
    Text(
        formatDuration(report.summary.totalMs),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Light,
    )
    val detail = MaterialTheme.typography.bodySmall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    if (report.coverageDays < report.window.days) {
        Text(
            "Android solo tiene datos de ${report.coverageDays} de los ${report.window.days} días.",
            style = detail,
            color = muted,
        )
    }
    val change = report.changePercent
    val previous = report.previousTotalMs
    when {
        change != null && previous != null -> {
            val amount = formatDuration((report.summary.totalMs - previous).absoluteValue)
            val direction = if (change <= 0) "menos" else "más"
            val percent = String.format(Locale.getDefault(), "%.0f", change.absoluteValue)
            Text(
                "$percent % $direction que los ${report.window.label} anteriores ($amount).",
                style = detail,
                color = muted,
            )
        }
        else -> Text("Aún no hay un periodo anterior completo con el que comparar.", style = detail, color = muted)
    }
}

@Composable
private fun ProgressCard() {
    var period by rememberSaveable { mutableStateOf(ProgressPeriod.YEAR) }
    val fraction = periodProgress(period, LocalDateTime.now())

    GlassCard {
        Text("Progreso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressPeriod.entries.forEach { option ->
                FilterChip(selected = option == period, onClick = { period = option }, label = { Text(option.label) })
            }
        }
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        Text(
            String.format(Locale.getDefault(), "%.1f %% transcurrido", fraction * 100),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
