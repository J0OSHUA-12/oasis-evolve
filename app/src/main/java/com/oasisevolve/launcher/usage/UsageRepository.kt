package com.oasisevolve.launcher.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Estadísticas de una app, o del total si es el resumen. */
data class AppStats(
    val packageName: String?,
    val todayMs: Long,
    val todayOpens: Int,
    val last24hMs: Long,
    val last7dMs: Long,
    val last30dMs: Long,
    val previous7dMs: Long?,
    val previous30dMs: Long?,
    val coverageDays: Int,
    /** Del más antiguo a hoy. */
    val dailyMs: List<Long>,
) {
    val averageDailyMs: Long get() = if (coverageDays > 0) last7dMs / coverageDays else 0
    val projection: TimeProjection get() = projectTime(averageDailyMs)
}

/** Lo que necesita el widget de Inicio: el tiempo de hoy, sin el historial completo. */
data class UsageToday(
    val generatedAt: Long,
    val totalMs: Long,
    /** De mayor a menor uso. */
    val apps: List<Pair<String, Long>>,
)

data class UsageOverview(
    val generatedAt: Long,
    val total: AppStats,
    /** Ordenadas de mayor a menor uso de hoy. */
    val apps: List<AppStats>,
    val coverage30d: Int,
) {
    fun today(): UsageToday = UsageToday(
        generatedAt,
        total.todayMs,
        apps.filter { it.todayMs > 0 }.sortedByDescending { it.todayMs }.map { it.packageName.orEmpty() to it.todayMs },
    )

    fun statsFor(packageName: String): AppStats =
        apps.firstOrNull { it.packageName == packageName } ?: total.copy(
            packageName = packageName,
            todayMs = 0,
            todayOpens = 0,
            last24hMs = 0,
            last7dMs = 0,
            last30dMs = 0,
            previous7dMs = if (total.previous7dMs != null) 0 else null,
            previous30dMs = if (total.previous30dMs != null) 0 else null,
            dailyMs = total.dailyMs.map { 0L },
        )
}

/** Un único motor de estadísticas: el panel de Bienestar y las interrupciones leen el mismo resumen. */
/** Horas antes de medianoche que se leen para saber qué estaba abierto al empezar el día. */
private const val TODAY_CONTEXT_MS = 6 * 3_600_000L

class UsageRepository(private val context: Context) {
    private val manager = context.getSystemService(UsageStatsManager::class.java)
    private val ownPackage = context.packageName

    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ownPackage)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ownPackage)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Solo el día de hoy: lee unas horas de eventos en lugar de 60 días. Se usa al volver a Inicio,
     * que es cuando el cálculo completo hacía que el launcher fuera lento.
     */
    suspend fun today(): UsageToday = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val midnight = midnight().timeInMillis
        val today = computeWindows(readEvents(midnight - TODAY_CONTEXT_MS, now), listOf(midnight to now), launcherPackages())
            .single()
        UsageToday(
            now,
            today.totalMs,
            today.perApp.filterValues { it.foregroundMs > 0 }.map { it.key to it.value.foregroundMs }.sortedByDescending { it.second },
        )
    }

    private fun midnight(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    suspend fun overview(): UsageOverview = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val midnight = midnight()

        val days = (6 downTo 0).map { back ->
            val day = (midnight.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -back) }.timeInMillis
            val next = (midnight.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -back + 1) }.timeInMillis
            day to minOf(next, now)
        }
        val week = 7 * DAY_MS
        val month = 30 * DAY_MS
        val windows = listOf(
            midnight.timeInMillis to now,
            now - DAY_MS to now,
            now - week to now,
            now - 2 * week to now - week,
            now - month to now,
            now - 2 * month to now - month,
        ) + days

        val summaries = computeWindows(readEvents(now - 2 * month, now), windows, launcherPackages())
        val (today, last24h, last7d, previous7d, last30d) = summaries
        val previous30d = summaries[5]
        val daily = summaries.drop(6)
        val coverage = coverageDays(last7d.firstEventAt, now - week, now)
        val comparable7 = isPeriodComparable(previous7d.firstEventAt, now - 2 * week, week)
        val comparable30 = isPeriodComparable(previous30d.firstEventAt, now - 2 * month, month)

        fun stats(pkg: String?): AppStats {
            fun ms(summary: UsageSummary) =
                if (pkg == null) summary.totalMs else summary.perApp[pkg]?.foregroundMs ?: 0L
            return AppStats(
                packageName = pkg,
                todayMs = ms(today),
                todayOpens = if (pkg == null) today.perApp.values.sumOf { it.opens } else today.perApp[pkg]?.opens ?: 0,
                last24hMs = ms(last24h),
                last7dMs = ms(last7d),
                last30dMs = ms(last30d),
                previous7dMs = if (comparable7) ms(previous7d) else null,
                previous30dMs = if (comparable30) ms(previous30d) else null,
                coverageDays = coverage,
                dailyMs = daily.map(::ms),
            )
        }

        val packages = last30d.perApp.keys + last7d.perApp.keys + today.perApp.keys
        UsageOverview(
            generatedAt = now,
            total = stats(null),
            apps = packages.map(::stats).sortedWith(compareByDescending<AppStats> { it.todayMs }.thenByDescending { it.last7dMs }),
            coverage30d = coverageDays(last30d.firstEventAt, now - month, now),
        )
    }

    /** El tiempo en la pantalla de inicio no es uso de apps, sea este launcher u otro instalado. */
    private fun launcherPackages(): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.queryIntentActivities(home, 0)
            .map { it.activityInfo.packageName }
            .toSet() + ownPackage
    }

    private fun readEvents(start: Long, end: Long): Sequence<RawEvent> = sequence {
        val events = manager.queryEvents(start, end) ?: return@sequence
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (isCountedEvent(event.eventType)) {
                yield(RawEvent(event.eventType, event.packageName, event.className, event.timeStamp))
            }
        }
    }
}
