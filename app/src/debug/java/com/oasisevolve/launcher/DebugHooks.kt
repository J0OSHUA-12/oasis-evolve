package com.oasisevolve.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Añade o quita un paquete del filtro de notificaciones. Sirve para probar con notificaciones del
 * shell de adb, que no es una app que se pueda elegir en la interfaz.
 */
class DebugHooks : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra("package") ?: return
        val add = intent.getBooleanExtra("add", true)
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            (context.applicationContext as EvolveApp).settings.update { s ->
                s.copy(quietApps = if (add) s.quietApps + pkg else s.quietApps - pkg)
            }
            result.finish()
        }
    }
}
