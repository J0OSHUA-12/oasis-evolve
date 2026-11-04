package com.oasisevolve.launcher.quiet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.oasisevolve.launcher.EvolveApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Pospone las notificaciones de las apps elegidas hasta la franja en que el usuario quiere verlas.
 * Usa la función de Android de posponer: no se guarda ni se lee su contenido, solo de qué app son.
 */
class QuietService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching: Job? = null

    @Volatile
    private var filtered: Set<String> = emptySet()

    @Volatile
    private var window = QuietWindow(21 * 60, 60)

    override fun onListenerConnected() {
        watching?.cancel()
        watching = scope.launch {
            (application as EvolveApp).settings.settings
                .map { Triple(it.quietApps, it.quietStartMinute, it.quietLengthMinutes) }
                .distinctUntilChanged()
                .collect { (apps, start, length) ->
                    filtered = apps
                    window = QuietWindow(start, length)
                    // Las que ya estaban en la barra también se posponen al elegir una app nueva.
                    runCatching { activeNotifications }.getOrNull()?.forEach(::consider)
                }
        }
    }

    override fun onListenerDisconnected() {
        watching?.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = consider(sbn)

    private fun consider(sbn: StatusBarNotification) {
        val now = Instant.now()
        val until = postponeUntil(sbn.packageName, sbn.isClearable, filtered, window, now, ZoneId.systemDefault()) ?: return
        // Un segundo de margen para que vuelva ya dentro de la franja.
        val duration = until.toEpochMilli() - now.toEpochMilli() + 1_000
        runCatching { snoozeNotification(sbn.key, duration) }
            .onSuccess { QuietLog.record(this, sbn.packageName, sbn.key, until) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun isEnabled(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

        fun settingsIntent(context: Context): Intent {
            val intent = if (Build.VERSION.SDK_INT >= 30) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, QuietService::class.java).flattenToString(),
                )
            } else {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }
            return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

/**
 * Cuántas notificaciones esperan, por app. Solo guarda el identificador que Android les da y la app,
 * nunca su texto. Se vacía sola cuando llega la franja.
 */
object QuietLog {
    private const val PREFS = "quiet_log"
    private const val OPENING = "opening"
    private const val KEYS = "keys"

    @Synchronized
    fun record(context: Context, packageName: String, key: String, opening: Instant) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sameOpening = prefs.getLong(OPENING, 0) == opening.toEpochMilli()
        val keys = if (sameOpening) prefs.getStringSet(KEYS, emptySet()).orEmpty() else emptySet()
        prefs.edit()
            .putLong(OPENING, opening.toEpochMilli())
            .putStringSet(KEYS, keys + "$packageName|$key")
            .apply()
    }

    /** Notificaciones que esperan a [opening], agrupadas por paquete. */
    fun waiting(context: Context, opening: Instant): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(OPENING, 0) != opening.toEpochMilli()) return emptyMap()
        return prefs.getStringSet(KEYS, emptySet()).orEmpty()
            .groupingBy { it.substringBefore('|') }
            .eachCount()
    }
}
