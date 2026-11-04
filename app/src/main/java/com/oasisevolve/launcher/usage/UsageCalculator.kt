package com.oasisevolve.launcher.usage

import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

/** Solo estos tipos influyen en el cálculo. Descartar el resto pronto ahorra la mayor parte del trabajo. */
fun isCountedEvent(type: Int): Boolean = when (type) {
    EventType.ACTIVITY_RESUMED,
    EventType.ACTIVITY_PAUSED,
    EventType.ACTIVITY_STOPPED,
    EventType.SCREEN_NON_INTERACTIVE,
    EventType.KEYGUARD_SHOWN,
    EventType.DEVICE_SHUTDOWN,
    -> true
    else -> false
}

data class AppUsage(val foregroundMs: Long, val opens: Int)

data class UsageSummary(
    val perApp: Map<String, AppUsage>,
    val firstEventAt: Long?,
) {
    val totalMs: Long get() = perApp.values.sumOf { it.foregroundMs }
}

private class OpenPackage(var since: Long, val activities: MutableSet<String> = mutableSetOf())

/**
 * Suma el tiempo en primer plano por paquete dentro de `[windowStart, windowEnd]`.
 *
 * Un paquete cuenta desde que tiene alguna actividad reanudada hasta que ya no tiene ninguna.
 * Apagar la pantalla, bloquearla o apagar el teléfono cierra todas las sesiones.
 *
 * Una apertura es volver a una app desde otra, como en el contador del propio Android: desbloquear
 * el teléfono o saltar entre pantallas de la misma app no cuenta.
 *
 * Con [assumeOpenAtStart], una pausa sin reanudación previa se cuenta desde el inicio de la ventana.
 * Solo es válido si los eventos empiezan justo en la ventana. Si también se le dan los anteriores,
 * el estado ya se conoce y esa suposición inventaría tiempo.
 */
class UsageAccumulator(
    val windowStart: Long,
    val windowEnd: Long,
    private val excludedPackages: Set<String> = emptySet(),
    private val assumeOpenAtStart: Boolean = true,
) {
    private val open = HashMap<String, OpenPackage>()
    private val seen = HashSet<String>()
    private val totals = HashMap<String, Long>()
    private val opens = HashMap<String, Int>()
    private var lastBackgrounded: String? = null
    private var firstEventAt: Long? = null
    private var deviceWentInactive = false

    private fun add(pkg: String, from: Long, to: Long) {
        val start = max(from, windowStart)
        val end = min(to, windowEnd)
        if (end > start) totals[pkg] = (totals[pkg] ?: 0L) + (end - start)
    }

    private fun closeAll(at: Long) {
        for ((pkg, session) in open) add(pkg, session.since, at)
        open.clear()
    }

    fun accept(event: RawEvent) {
        val insideWindow = event.timestamp >= windowStart
        if (firstEventAt == null && insideWindow) firstEventAt = event.timestamp
        when (event.type) {
            EventType.ACTIVITY_RESUMED -> {
                val pkg = event.packageName ?: return
                if (pkg in excludedPackages) return
                seen += pkg
                val session = open[pkg]
                if (session == null) {
                    if (insideWindow && pkg != lastBackgrounded) opens[pkg] = (opens[pkg] ?: 0) + 1
                    open[pkg] = OpenPackage(event.timestamp).also { it.activities += event.className.orEmpty() }
                } else {
                    session.activities += event.className.orEmpty()
                }
            }

            EventType.ACTIVITY_PAUSED, EventType.ACTIVITY_STOPPED -> {
                val pkg = event.packageName ?: return
                if (event.type == EventType.ACTIVITY_PAUSED) lastBackgrounded = pkg
                if (pkg in excludedPackages) return
                val session = open[pkg]
                if (session != null) {
                    session.activities -= event.className.orEmpty()
                    if (session.activities.isEmpty()) {
                        add(pkg, session.since, event.timestamp)
                        open.remove(pkg)
                    }
                } else if (
                    assumeOpenAtStart && event.type == EventType.ACTIVITY_PAUSED && pkg !in seen && !deviceWentInactive
                ) {
                    // La app ya estaba abierta cuando empezó la ventana.
                    add(pkg, windowStart, event.timestamp)
                }
                seen += pkg
            }

            EventType.SCREEN_NON_INTERACTIVE, EventType.KEYGUARD_SHOWN, EventType.DEVICE_SHUTDOWN -> {
                deviceWentInactive = true
                closeAll(event.timestamp)
            }
        }
    }

    fun finish(): UsageSummary {
        closeAll(windowEnd)
        val perApp = (totals.keys + opens.keys).associateWith { pkg ->
            AppUsage(foregroundMs = totals[pkg] ?: 0L, opens = opens[pkg] ?: 0)
        }
        return UsageSummary(perApp, firstEventAt)
    }
}

fun computeUsage(
    events: Sequence<RawEvent>,
    windowStart: Long,
    windowEnd: Long,
    excludedPackages: Set<String> = emptySet(),
): UsageSummary {
    val accumulator = UsageAccumulator(windowStart, windowEnd, excludedPackages)
    events.forEach(accumulator::accept)
    return accumulator.finish()
}

/**
 * Una sola pasada por los eventos para todas las ventanas. Cada ventana recibe también los eventos
 * anteriores a ella, para saber qué estaba abierto al empezar sin tener que suponerlo. Lo que ocurrió
 * antes de los datos que conserva Android queda sin contar: no se puede saber.
 */
fun computeWindows(
    events: Sequence<RawEvent>,
    windows: List<Pair<Long, Long>>,
    excludedPackages: Set<String> = emptySet(),
): List<UsageSummary> {
    val accumulators = windows.map { (start, end) ->
        UsageAccumulator(start, end, excludedPackages, assumeOpenAtStart = false)
    }
    events.forEach { event ->
        accumulators.forEach { window ->
            if (event.timestamp <= window.windowEnd) window.accept(event)
        }
    }
    return accumulators.map { it.finish() }
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

/** «26,5» con la coma decimal del idioma del teléfono. */
fun formatDays(days: Double, locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.1f", days)

/** Variación menor que esto no se presenta como un cambio. */
private const val STABLE_PERCENT = 1.0

/**
 * Mensaje de comparación entre periodos equivalentes. Refleja solo tiempo de pantalla:
 * no afirma que el tiempo reducido se haya usado en otra cosa, y si sube se limita a informar.
 */
fun comparisonMessage(currentMs: Long, previousMs: Long, periodName: String, subject: String = "tu uso"): String? {
    val change = percentChange(currentMs, previousMs) ?: return null
    val percent = change.absoluteValue.roundToInt()
    val difference = formatDuration((currentMs - previousMs).absoluteValue)
    return when {
        change.absoluteValue < STABLE_PERCENT -> "Se mantuvo igual que $periodName."
        change < 0 -> "Redujiste un $percent % $subject respecto a $periodName. Eso representa $difference menos de pantalla."
        else -> "Subió un $percent % $subject respecto a $periodName: $difference más de pantalla."
    }
}
