package com.oasisevolve.launcher.wellbeing

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class ProgressPeriod(val label: String) {
    YEAR("Año"),
    SEMESTER("Semestre"),
    MONTH("Mes"),
}

/** Fracción transcurrida del periodo que contiene a [now], entre 0 y 1. */
fun periodProgress(period: ProgressPeriod, now: LocalDateTime): Float {
    val (start, end) = when (period) {
        ProgressPeriod.YEAR -> LocalDate.of(now.year, 1, 1) to LocalDate.of(now.year + 1, 1, 1)
        ProgressPeriod.SEMESTER ->
            if (now.monthValue <= 6) LocalDate.of(now.year, 1, 1) to LocalDate.of(now.year, 7, 1)
            else LocalDate.of(now.year, 7, 1) to LocalDate.of(now.year + 1, 1, 1)
        ProgressPeriod.MONTH -> now.toLocalDate().withDayOfMonth(1).let { it to it.plusMonths(1) }
    }
    val total = Duration.between(start.atStartOfDay(), end.atStartOfDay()).toMinutes().toFloat()
    val elapsed = Duration.between(start.atStartOfDay(), now).toMinutes().toFloat()
    return (elapsed / total).coerceIn(0f, 1f)
}
