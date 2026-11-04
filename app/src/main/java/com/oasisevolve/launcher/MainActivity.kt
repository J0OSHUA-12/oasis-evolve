package com.oasisevolve.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.oasisevolve.launcher.focus.GuardService
import com.oasisevolve.launcher.navigation.LauncherRoot
import com.oasisevolve.launcher.widgets.REQUEST_CONFIGURE_WIDGET
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private val widgets by lazy { (application as EvolveApp).widgets }

    /** Se emite cada vez que el sistema pide ir a Inicio. */
    private val homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        widgets.onBindResult(result.resultCode, this, viewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LauncherRoot(
                viewModel = viewModel,
                homeRequests = homeRequests,
                onPickWidget = { provider, target, w, h ->
                    widgets.startAdding(provider, target, w, h, this, viewModel) { intent -> bindWidget.launch(intent) }
                },
            )
        }
        handleGuard(intent)
    }

    /** El servicio de bloqueo abre el launcher para frenar una app o avisar de que se acabó su tiempo. */
    private fun handleGuard(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(GuardService.EXTRA_GATE_PACKAGE)?.let { pkg ->
            viewModel.gateFromGuard(pkg, intent.getBooleanExtra(GuardService.EXTRA_AFTER_TIME_UP, false))
        }
        intent.getStringExtra(GuardService.EXTRA_TIME_UP_PACKAGE)?.let(viewModel::timeUpFromGuard)
        intent.removeExtra(GuardService.EXTRA_GATE_PACKAGE)
        intent.removeExtra(GuardService.EXTRA_TIME_UP_PACKAGE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch { homeRequests.emit(Unit) }
        }
        handleGuard(intent)
    }

    override fun onStart() {
        super.onStart()
        widgets.startListening()
    }

    override fun onStop() {
        widgets.stopListening()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    @Deprecated("AppWidgetHost entrega aquí el resultado de la configuración del widget.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            widgets.onConfigureResult(resultCode, viewModel)
        }
    }
}
