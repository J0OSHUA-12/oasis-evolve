package com.oasisevolve.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
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
                onPickWidget = { provider ->
                    widgets.startAdding(provider, this, viewModel) { intent -> bindWidget.launch(intent) }
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch { homeRequests.emit(Unit) }
        }
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
        viewModel.refreshUsage()
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
