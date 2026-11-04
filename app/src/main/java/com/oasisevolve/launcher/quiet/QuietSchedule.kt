package com.oasisevolve.launcher.quiet

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Franja del día en la que se pueden ver las notificaciones filtradas, por ejemplo antes de dormir.
 * [startMinute] es el minuto del día en que empieza, y puede pasar de medianoche.
 */
data class QuietWindow(val startMinute: Int, val lengthMinutes: Int) {
    fun isOpen(now: Instant, zone: ZoneId): Boolean {
        val time = now.atZone(zone).toLocalTime()
        val minute = time.hour * 60 + time.minute
        val sinceStart = Math.floorMod(minute - startMinute, MINUTES_PER_DAY)
        return sinceStart < lengthMinutes
    }

    /** Próxima apertura de la franja, con la duración real del día aunque cambie la hora oficial. */
    fun nextOpening(now: Instant, zone: ZoneId): Instant {
        val local = now.atZone(zone)
        val today = ZonedDateTime.of(local.toLocalDate(), LocalTime.of(startMinute / 60, startMinute % 60), zone)
        val opening = if (today.toInstant().isAfter(now)) today else today.plusDays(1)
        return opening.toInstant()
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
    }
}

/** Qué hacer con una notificación nueva. Devuelve hasta cuándo posponerla, o `null` para dejarla pasar. */
fun postponeUntil(
    packageName: String,
    clearable: Boolean,
    filtered: Set<String>,
    window: QuietWindow,
    now: Instant,
    zone: ZoneId,
): Instant? {
    // Las que no se pueden descartar (música, llamadas, descargas) nunca se tocan.
    if (!clearable || packageName !in filtered) return null
    if (window.isOpen(now, zone)) return null
    return window.nextOpening(now, zone)
}
