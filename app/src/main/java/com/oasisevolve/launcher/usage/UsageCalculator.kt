package com.oasisevolve.launcher.usage

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Evento de uso independiente de Android, para poder probar el cálculo en la JVM. */
data class RawEvent(val type: Int, val packageName: String?, val className: String?, val timestamp: Long)

/** Valores públicos de `UsageEvents.Event`. */
object EventType {
    const val ACTIVITY_RESUMED = 1
    const val ACTIVITY_PAUSED = 2
    const val SCREEN_NON_INTERACTIVE = 16
    const val KEYGUARD_SHOWN = 17
    const val ACTIVITY_STOPPED = 23
    const val DEVICE_SHUTDOWN = 26
}

data class AppUsage(val foregroundMs: Long, val opens: Int)

data class UsageSummary(
    val perApp: Map<String, AppUsage>,
    val firstEventAt: Long?,
) {
    val totalMs: Long get() = perApp.values.sumOf { it.foregroundMs }
}

/** Dos reanudaciones de la misma app separadas por menos que esto cuentan como una sola apertura. */
private const val SAME_OPEN_GAP_MS = 1_000L

private class OpenPackage(var since: Long, val activities: MutableSet<String> = mutableSetOf())

/**
 * Suma el tiempo en primer plano por paquete dentro de `[windowStart, windowEnd]`.
 *
 * Un paquete cuenta desde que tiene alguna actividad reanudada hasta que ya no tiene ninguna.
 * Apagar la pantalla, bloquearla o apagar el teléfono cierra todas las sesiones.
 */
fun computeUsage(
    events: Sequence<RawEvent>,
    windowStart: Long,
    windowEnd: Long,
    excludedPackages: Set<String> = emptySet(),
): UsageSummary {
    val open = HashMap<String, OpenPackage>()
    val seen = HashSet<String>()
    val totals = HashMap<String, Long>()
    val opens = HashMap<String, Int>()
    val lastClosedAt = HashMap<String, Long>()
    var firstEventAt: Long? = null
    var deviceWentInactive = false

    fun add(pkg: String, from: Long, to: Long) {
        val start = max(from, windowStart)
        val end = min(to, windowEnd)
        if (end > start) totals[pkg] = (totals[pkg] ?: 0L) + (end - start)
    }

    fun closeAll(at: Long) {
        for ((pkg, session) in open) {
            add(pkg, session.since, at)
            lastClosedAt[pkg] = at
        }
        open.clear()
    }

    for (event in events) {
        if (firstEventAt == null) firstEventAt = event.timestamp
        when (event.type) {
            EventType.ACTIVITY_RESUMED -> {
                val pkg = event.packageName ?: continue
                if (pkg in excludedPackages) continue
                seen += pkg
                val session = open[pkg]
                if (session == null) {
                    val closedAt = lastClosedAt[pkg]
                    if (closedAt == null || event.timestamp - closedAt > SAME_OPEN_GAP_MS) {
                        opens[pkg] = (opens[pkg] ?: 0) + 1
                    }
                    open[pkg] = OpenPackage(event.timestamp).also { it.activities += event.className.orEmpty() }
                } else {
                    session.activities += event.className.orEmpty()
                }
            }

            EventType.ACTIVITY_PAUSED, EventType.ACTIVITY_STOPPED -> {
                val pkg = event.packageName ?: continue
                if (pkg in excludedPackages) continue
                val session = open[pkg]
                if (session != null) {
                    session.activities -= event.className.orEmpty()
                    if (session.activities.isEmpty()) {
                        add(pkg, session.since, event.timestamp)
                        lastClosedAt[pkg] = event.timestamp
                        open.remove(pkg)
                    }
                } else if (
                    event.type == EventType.ACTIVITY_PAUSED && pkg !in seen && !deviceWentInactive
                ) {
                    // La app ya estaba abierta cuando empezó la ventana.
                    add(pkg, windowStart, event.timestamp)
                    lastClosedAt[pkg] = event.timestamp
                }
                seen += pkg
            }

            EventType.SCREEN_NON_INTERACTIVE, EventType.KEYGUARD_SHOWN, EventType.DEVICE_SHUTDOWN -> {
                deviceWentInactive = true
                closeAll(event.timestamp)
            }
        }
    }
    closeAll(windowEnd)

    val perApp = (totals.keys + opens.keys).associateWith { pkg ->
        AppUsage(foregroundMs = totals[pkg] ?: 0L, opens = opens[pkg] ?: 0)
    }
    return UsageSummary(perApp, firstEventAt)
}

const val DAY_MS = 86_400_000L

/** Días de la ventana con datos reales. `0` si el sistema no devolvió ningún evento. */
fun coverageDays(firstEventAt: Long?, windowStart: Long, windowEnd: Long): Int {
    if (firstEventAt == null) return 0
    val covered = windowEnd - max(firstEventAt, windowStart)
    return max(1, ceil(covered.toDouble() / DAY_MS).toInt())
}

/**
 * Un periodo anterior solo es comparable si los datos llegan hasta casi su inicio. Se tolera
 * un margen porque es normal no usar el teléfono al principio de la ventana (por ejemplo, de noche).
 */
fun isPeriodComparable(firstEventAt: Long?, periodStart: Long, periodLengthMs: Long): Boolean {
    if (firstEventAt == null) return false
    val tolerance = max(12 * 3_600_000L, periodLengthMs / 10)
    return firstEventAt <= periodStart + tolerance
}

/** Cambio porcentual respecto al periodo anterior; `null` si no hay base con la que comparar. */
fun percentChange(currentMs: Long, previousMs: Long): Double? =
    if (previousMs <= 0) null else (currentMs - previousMs).toDouble() / previousMs * 100.0

data class TimeProjection(val weekMs: Long, val monthMs: Long, val yearMs: Long) {
    val yearInFullDays: Double get() = yearMs.toDouble() / DAY_MS
}

/** Estimación lineal a partir de un promedio diario real. No es una predicción. */
fun projectTime(averageDailyMs: Long) = TimeProjection(
    weekMs = averageDailyMs * 7,
    monthMs = averageDailyMs * 30,
    yearMs = averageDailyMs * 365,
)

fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 1 -> if (ms > 0) "menos de 1 min" else "0 min"
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0L -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}
