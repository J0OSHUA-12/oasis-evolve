package com.oasisevolve.launcher.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.GateRequest
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.usage.AppStats
import com.oasisevolve.launcher.usage.formatDuration
import com.oasisevolve.launcher.usage.projectTime
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun InterruptionScreen(
    request: GateRequest,
    waitSeconds: Int,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var remaining by remember(request.app.key) { mutableIntStateOf(waitSeconds) }
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
                "Estás a punto de abrir ${request.app.label}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
            )
            request.stats?.let { StatsCard(it) } ?: Text(
                "Sin acceso a datos de uso no hay estadísticas que mostrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onContinue, enabled = remaining == 0, modifier = Modifier.fillMaxWidth()) {
                Text(if (remaining > 0) "Continuar en $remaining s" else "Continuar")
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al inicio")
            }
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
