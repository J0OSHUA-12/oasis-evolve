package com.oasisevolve.launcher.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.GateRequest
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.usage.AppStats
import com.oasisevolve.launcher.usage.formatDuration
import com.oasisevolve.launcher.usage.projectTime
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Opciones de temporizador, en minutos. `null` es sin límite. */
val TIMER_OPTIONS = listOf(null, 5, 10, 15, 30, 60)

fun timerLabel(minutes: Int?): String = if (minutes == null) "Sin límite" else "$minutes min"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterruptionScreen(
    request: GateRequest,
    waitSeconds: Int,
    guardEnabled: Boolean,
    onContinue: (timerMinutes: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var remaining by remember(request.app.key) { mutableIntStateOf(waitSeconds) }
    var timer by remember(request.app.key) { mutableStateOf(request.timerMinutes) }
    LaunchedEffect(request.app.key) {
        // Sin esto, el teclado de la búsqueda absorbe la primera pulsación de Atrás.
        focusManager.clearFocus()
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                when {
                    request.afterTimeUp -> "Ya usaste tu tiempo en ${request.app.label}"
                    request.returned -> "Volviste a ${request.app.label}"
                    else -> "Estás a punto de abrir ${request.app.label}"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
            )
            if (request.afterTimeUp) {
                Text(
                    "Se acabó el temporizador de la última vez. Si quieres seguir, elige otro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (request.returned) {
                Text(
                    "Se abrió sin pasar por el launcher, desde la lista de apps recientes o una notificación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            request.stats?.let { StatsCard(it) } ?: Text(
                "Sin acceso a datos de uso no hay estadísticas que mostrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (guardEnabled) {
                Text("Temporizador", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TIMER_OPTIONS.forEach { option ->
                        FilterChip(selected = timer == option, onClick = { timer = option }, label = { Text(timerLabel(option)) })
                    }
                }
            } else {
                Text(
                    "Para elegir un temporizador que cierre la app, activa el bloqueo en Ajustes > Interrupts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { onContinue(if (guardEnabled) timer else null) }, enabled = remaining == 0, modifier = Modifier.fillMaxWidth()) {
                Text(if (remaining > 0) "Continuar en $remaining s" else "Continuar")
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al inicio")
            }
        }
    }
}

/** Aviso de que se acabó el tiempo elegido para una app. El launcher ya está delante. */
@Composable
fun TimeUpScreen(app: AppEntry, todayMs: Long?, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Se acabó tu tiempo en ${app.label}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
            )
            todayMs?.let {
                Text("Hoy llevas ${formatDuration(it)} en esta app.", style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                "Si vuelves a abrirla, pasará antes por la interrupción.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Entendido") }
        }
    }
}

@Composable
private fun StatsCard(stats: AppStats) {
    GlassCard {
        StatLine("Hoy", formatDuration(stats.todayMs))
        StatLine("Últimas 24 horas", formatDuration(stats.last24hMs))
        StatLine("Últimos 7 días", formatDuration(stats.last7dMs))
        if (stats.coverageDays > 0) {
            val days = if (stats.coverageDays == 1) "1 día" else "${stats.coverageDays} días"
            StatLine("Promedio diario ($days con datos)", formatDuration(stats.averageDailyMs))
        }
        val yearDays = projectTime(stats.averageDailyMs).yearInFullDays.roundToInt()
        if (yearDays >= 1) {
            Text(
                "Si mantienes este promedio, podrías dedicar aproximadamente $yearDays ${if (yearDays == 1) "día completo" else "días completos"} al año a esta app. Es una estimación.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
