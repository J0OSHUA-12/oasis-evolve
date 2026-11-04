package com.oasisevolve.launcher.focus

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.os.HandlerCompat
import com.oasisevolve.launcher.EvolveApp
import com.oasisevolve.launcher.MainActivity
import com.oasisevolve.launcher.usage.RawEvent
import com.oasisevolve.launcher.usage.isCountedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Cuánto atrás mirar para saber si una app sigue abierta al acabarse su tiempo. */
private const val FOREGROUND_LOOKBACK_MS = 6 * 3_600_000L

/**
 * Servicio de accesibilidad limitado: solo recibe el aviso de que una ventana cambió, y solo de las
 * apps vigiladas. No lee el contenido de la pantalla ni actúa sobre otras apps. Si una app vigilada
 * se abre sin pasar por el launcher, o se acaba su tiempo, devuelve al usuario al launcher.
 */
class GuardService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val book get() = (application as EvolveApp).guard
    private val usage by lazy { getSystemService(UsageStatsManager::class.java) }
    private val scheduled = HashMap<String, Long>()

    @Volatile
    private var policies: Map<String, AppPolicy> = emptyMap()

    override fun onServiceConnected() {
        connected = true
        scope.launch {
            (application as EvolveApp).settings.settings.map(::policiesOf).distinctUntilChanged().collect { current ->
                policies = current
                book.retain(current.keys)
                listenOnlyTo(current.keys)
            }
        }
    }

    /** Sin paquetes, Android enviaría los avisos de todas las apps: entonces se limita al propio launcher. */
    private fun listenOnlyTo(packages: Set<String>) {
        val info = serviceInfo ?: return
        info.packageNames = if (packages.isEmpty()) arrayOf(packageName) else packages.toTypedArray()
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val policy = policies[pkg] ?: return
        val now = System.currentTimeMillis()
        val session = book.session(pkg)
        val away = session != null &&
            wasAwayLongerThan(events(session.checkedAt, now), pkg, session.checkedAt, now, GUARD_GRACE_MS)
        val verdict = book.onForeground(pkg, now, policy, away)
        if (verdict is Verdict.Block) {
            openLauncher(EXTRA_GATE_PACKAGE, pkg, verdict.afterTimeUp)
            return
        }
        book.session(pkg)?.endsAt?.let { endsAt -> scheduleEnd(pkg, endsAt) }
    }

    private fun scheduleEnd(pkg: String, endsAt: Long) {
        if (scheduled[pkg] == endsAt) return
        handler.removeCallbacksAndMessages(pkg)
        scheduled[pkg] = endsAt
        val delay = (endsAt - System.currentTimeMillis()).coerceAtLeast(0)
        HandlerCompat.postDelayed(handler, { onTimeUp(pkg, endsAt) }, pkg, delay)
    }

    private fun onTimeUp(pkg: String, endsAt: Long) {
        scheduled -= pkg
        if (!book.expire(pkg, endsAt)) return
        val now = System.currentTimeMillis()
        if (isInForeground(events(now - FOREGROUND_LOOKBACK_MS, now), pkg)) openLauncher(EXTRA_TIME_UP_PACKAGE, pkg)
    }

    private fun events(from: Long, to: Long): List<RawEvent> {
        val result = ArrayList<RawEvent>()
        val iterator = runCatching { usage.queryEvents(from, to) }.getOrNull() ?: return result
        val event = UsageEvents.Event()
        while (iterator.hasNextEvent()) {
            iterator.getNextEvent(event)
            if (isCountedEvent(event.eventType)) {
                result += RawEvent(event.eventType, event.packageName, event.className, event.timeStamp)
            }
        }
        return result
    }

    private fun openLauncher(extra: String, pkg: String, afterTimeUp: Boolean = false) {
        // Android permite abrir una actividad desde un servicio de accesibilidad activo.
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(extra, pkg)
                .putExtra(EXTRA_AFTER_TIME_UP, afterTimeUp),
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        connected = false
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_GATE_PACKAGE = "com.oasisevolve.launcher.GATE_PACKAGE"
        const val EXTRA_TIME_UP_PACKAGE = "com.oasisevolve.launcher.TIME_UP_PACKAGE"
        const val EXTRA_AFTER_TIME_UP = "com.oasisevolve.launcher.AFTER_TIME_UP"

        /**
         * ¿Está funcionando ahora? Puede estar activado en Ajustes y aun así desconectado: Android no lo
         * vuelve a conectar si se fuerza la detención del launcher.
         */
        @Volatile
        var connected = false
            private set

        /** ¿Lo activó el usuario en los ajustes de accesibilidad? */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val self = ComponentName(context, GuardService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == self }
        }

        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
