package com.oasisevolve.launcher.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class UsageWindow(val label: String, val days: Int) {
    LAST_24H("24 horas", 1),
    LAST_7D("7 días", 7),
    LAST_30D("30 días", 30),
}

data class UsageReport(
    val window: UsageWindow,
    val summary: UsageSummary,
    val coverageDays: Int,
    val previousTotalMs: Long?,
    val changePercent: Double?,
)

data class AppStats(
    val todayMs: Long,
    val last24hMs: Long,
    val last7dMs: Long,
    val coverageDays: Int,
) {
    val averageDailyMs: Long get() = if (coverageDays > 0) last7dMs / coverageDays else 0
}

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

    suspend fun report(window: UsageWindow): UsageReport = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val length = window.days * DAY_MS
        val start = now - length
        val previousStart = start - length

        val current = summarize(start, now)
        val previous = summarize(previousStart, start)
        val comparable = isPeriodComparable(previous.firstEventAt, previousStart, length)
        val previousTotal = if (comparable) previous.totalMs else null

        UsageReport(
            window = window,
            summary = current,
            coverageDays = coverageDays(current.firstEventAt, start, now),
            previousTotalMs = previousTotal,
            changePercent = previousTotal?.let { percentChange(current.totalMs, it) },
        )
    }

    suspend fun statsFor(packageName: String): AppStats = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val weekStart = now - 7 * DAY_MS
        val week = summarize(weekStart, now)
        AppStats(
            todayMs = summarize(midnight, now).perApp[packageName]?.foregroundMs ?: 0,
            last24hMs = summarize(now - DAY_MS, now).perApp[packageName]?.foregroundMs ?: 0,
            last7dMs = week.perApp[packageName]?.foregroundMs ?: 0,
            coverageDays = coverageDays(week.firstEventAt, weekStart, now),
        )
    }

    private fun summarize(start: Long, end: Long): UsageSummary =
        computeUsage(readEvents(start, end), start, end, setOf(ownPackage))

    private fun readEvents(start: Long, end: Long): Sequence<RawEvent> = sequence {
        val events = manager.queryEvents(start, end) ?: return@sequence
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            yield(RawEvent(event.eventType, event.packageName, event.className, event.timeStamp))
        }
    }
}
