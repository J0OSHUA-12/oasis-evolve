package com.oasisevolve.launcher.wellbeing

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

enum class TimePeriod(val title: String) {
    DAY("Progreso del día"),
    WEEK("Progreso de la semana"),
    MONTH("Progreso del mes"),
    SEMESTER("Progreso del semestre"),
    YEAR("Progreso del año"),
}

/** Semestre académico propio. Si no es válido, se usa el semestre calendario. */
data class AcademicSemester(val start: LocalDate, val end: LocalDate) {
    val isValid: Boolean get() = end.isAfter(start)
}

data class PeriodProgress(
    val period: TimePeriod,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val now: LocalDateTime,
) {
    private val total: Duration get() = Duration.between(start, end)
    val elapsed: Duration
        get() = Duration.between(start, now).coerceIn(Duration.ZERO, total)
    val remaining: Duration get() = total.minus(elapsed)

    /** Entre 0 y 1. */
    val fraction: Float get() = (elapsed.toMinutes().toDouble() / total.toMinutes()).toFloat()
    val percent: Int get() = (fraction * 100).toInt()
    val hasNotStarted: Boolean get() = now.isBefore(start)
    val hasEnded: Boolean get() = !now.isBefore(end)
}

private fun Duration.coerceIn(min: Duration, max: Duration): Duration = when {
    this < min -> min
    this > max -> max
    else -> this
}

/** Límites reales del periodo que contiene a [now]: meses y años bisiestos con su duración verdadera. */
fun periodBounds(
    period: TimePeriod,
    now: LocalDateTime,
    weekStart: DayOfWeek = DayOfWeek.MONDAY,
    academic: AcademicSemester? = null,
): Pair<LocalDate, LocalDate> {
    val today = now.toLocalDate()
    return when (period) {
        TimePeriod.DAY -> today to today.plusDays(1)
        TimePeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(weekStart)).let { it to it.plusWeeks(1) }
        TimePeriod.MONTH -> today.withDayOfMonth(1).let { it to it.plusMonths(1) }
        TimePeriod.YEAR -> today.withDayOfYear(1).let { it to it.plusYears(1) }
        TimePeriod.SEMESTER ->
            if (academic != null && academic.isValid) {
                // El último día elegido cuenta entero.
                academic.start to academic.end.plusDays(1)
            } else if (today.monthValue <= 6) {
                LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 7, 1)
            } else {
                LocalDate.of(today.year, 7, 1) to LocalDate.of(today.year + 1, 1, 1)
            }
    }
}

fun periodProgress(
    period: TimePeriod,
    now: LocalDateTime,
    weekStart: DayOfWeek = DayOfWeek.MONDAY,
    academic: AcademicSemester? = null,
): PeriodProgress {
    val (start, end) = periodBounds(period, now, weekStart, academic)
    return PeriodProgress(period, start.atStartOfDay(), end.atStartOfDay(), now)
}

/** Días si el periodo los permite; horas y minutos en el día o cuando queda menos de uno. */
fun describeSpan(duration: Duration, period: TimePeriod): String {
    if (period == TimePeriod.DAY || duration.toDays() == 0L) {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return if (hours > 0) "$hours h $minutes min" else "$minutes min"
    }
    val days = duration.toDays()
    return if (days == 1L) "1 día" else "$days días"
}
