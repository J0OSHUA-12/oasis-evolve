package com.oasisevolve.launcher.focus

import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.usage.EventType
import com.oasisevolve.launcher.usage.RawEvent

/** Tiempo fuera de una app vigilada a partir del cual volver a ella exige pasar otra vez por la interrupción. */
const val GUARD_GRACE_MS = 60_000L

/** Cómo se vigila una app: con interrupción (nivel 4), con temporizador, o con ambos. */
data class AppPolicy(val guarded: Boolean, val timerMinutes: Int?)

/** Permiso de uso concedido a una app. [endsAt] es `null` si no tiene límite. */
data class GuardSession(val startedAt: Long, val endsAt: Long?, val checkedAt: Long = startedAt)

sealed interface Verdict {
    /** Puede seguir: la sesión sigue vigente. */
    data object Allow : Verdict

    /** Empieza una sesión nueva con temporizador. */
    data class Started(val endsAt: Long) : Verdict

    /** Hay que volver al launcher y pasar por la interrupción. [afterTimeUp]: porque se acabó su tiempo. */
    data class Block(val afterTimeUp: Boolean) : Verdict
}

/**
 * ¿Estuvo la app fuera de primer plano más de [graceMs] desde [since]? Cuenta como fuera apagar la
 * pantalla o pasar a otra app. Los eventos son los de Android, ordenados por tiempo.
 */
fun wasAwayLongerThan(events: List<RawEvent>, pkg: String, since: Long, now: Long, graceMs: Long): Boolean {
    var awayFrom: Long? = null
    for (event in events) {
        if (event.timestamp < since || event.timestamp > now) continue
        when (event.type) {
            EventType.ACTIVITY_RESUMED -> if (event.packageName == pkg) {
                val from = awayFrom
                if (from != null && event.timestamp - from > graceMs) return true
                awayFrom = null
            }
            // Solo la pausa: la parada de una actividad llega después de que otra de la misma app se reanude.
            EventType.ACTIVITY_PAUSED -> if (event.packageName == pkg && awayFrom == null) awayFrom = event.timestamp
            EventType.SCREEN_NON_INTERACTIVE, EventType.KEYGUARD_SHOWN ->
                if (awayFrom == null) awayFrom = event.timestamp
        }
    }
    val from = awayFrom ?: return false
    return now - from > graceMs
}

/**
 * Libro de sesiones de las apps vigiladas. Lógica pura: el servicio de accesibilidad le dice qué app
 * se abrió y aplica lo que responde.
 */
class GuardBook {
    private val sessions = HashMap<String, GuardSession>()

    /** Apps cuyo tiempo se acabó: la próxima vez pasan por la interrupción aunque solo tengan temporizador. */
    private val gateNext = HashSet<String>()

    @Synchronized
    fun session(pkg: String): GuardSession? = sessions[pkg]

    /** El usuario pasó la interrupción: puede usar la app hasta que se acabe el tiempo elegido. */
    @Synchronized
    fun grant(pkg: String, now: Long, timerMinutes: Int?) {
        sessions[pkg] = GuardSession(now, timerMinutes?.let { now + it * 60_000L })
        gateNext -= pkg
    }

    /**
     * La app [pkg] acaba de pasar a primer plano. [awayTooLong] dice si estuvo fuera más del margen
     * desde la última comprobación; solo importa si tenía sesión.
     */
    @Synchronized
    fun onForeground(pkg: String, now: Long, policy: AppPolicy, awayTooLong: Boolean): Verdict {
        val session = sessions[pkg]
        if (session != null) {
            val expired = session.endsAt != null && now >= session.endsAt
            if (!expired && !awayTooLong) {
                sessions[pkg] = session.copy(checkedAt = now)
                return Verdict.Allow
            }
            sessions -= pkg
            if (expired) gateNext += pkg
        }
        if (policy.guarded || pkg in gateNext) return Verdict.Block(afterTimeUp = pkg in gateNext)
        val minutes = policy.timerMinutes ?: return Verdict.Allow
        grant(pkg, now, minutes)
        return Verdict.Started(now + minutes * 60_000L)
    }

    /** Se acabó el temporizador. Devuelve `false` si la sesión ya no era esa. */
    @Synchronized
    fun expire(pkg: String, endsAt: Long): Boolean {
        val session = sessions[pkg] ?: return false
        if (session.endsAt != endsAt) return false
        sessions -= pkg
        gateNext += pkg
        return true
    }

    /** Una app que deja de estar vigilada pierde su estado. */
    @Synchronized
    fun retain(packages: Set<String>) {
        sessions.keys.retainAll(packages)
        gateNext.retainAll(packages)
    }
}

/** El paquete de una clave de app: `paquete/actividad#usuario`. */
fun packageOfKey(key: String): String = key.substringBefore('/')

/** Qué apps vigilar y cómo, según los ajustes: las de nivel 4 y las que tienen temporizador. */
fun policiesOf(settings: LauncherSettings): Map<String, AppPolicy> {
    val guarded = settings.accessLevels.filterValues { it == AccessLevel.GUARDED.level }.keys.map(::packageOfKey).toSet()
    val timers = settings.appTimers.entries.associate { (key, minutes) -> packageOfKey(key) to minutes }
    return (guarded + timers.keys).associateWith { pkg -> AppPolicy(pkg in guarded, timers[pkg]) }
}

/** ¿Sigue [pkg] en primer plano según sus últimos eventos? */
fun isInForeground(events: List<RawEvent>, pkg: String): Boolean {
    var foreground = false
    for (event in events) {
        when (event.type) {
            EventType.ACTIVITY_RESUMED -> foreground = event.packageName == pkg
            EventType.ACTIVITY_PAUSED -> if (event.packageName == pkg) foreground = false
            EventType.SCREEN_NON_INTERACTIVE, EventType.KEYGUARD_SHOWN -> foreground = false
        }
    }
    return foreground
}
