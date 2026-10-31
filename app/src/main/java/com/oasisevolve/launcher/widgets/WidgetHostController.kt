package com.oasisevolve.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.GridTarget
import com.oasisevolve.launcher.storage.HomeItemKind
import com.oasisevolve.launcher.storage.WidgetTarget
import com.oasisevolve.launcher.storage.addGridItem
import com.oasisevolve.launcher.storage.widgetItem

const val REQUEST_CONFIGURE_WIDGET = 4101
private const val HOST_ID = 2048

/** Flujo oficial de Android para alojar widgets: asignar id, pedir vínculo, configurar y guardar. */
class WidgetHostController(private val context: Context) {
    private val manager = AppWidgetManager.getInstance(context)
    private val host = object : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = EvolveWidgetHostView(context)
    }
    private var pendingId: Int? = null
    private var pendingTarget = WidgetTarget.CENTER
    private var pendingW = 2
    private var pendingH = 2

    fun providers(): List<AppWidgetProviderInfo> = manager.installedProviders

    fun label(info: AppWidgetProviderInfo): String = info.loadLabel(context.packageManager)

    fun info(id: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(id)

    fun manager(): AppWidgetManager = manager

    fun startListening() = runCatching { host.startListening() }.let { }

    fun stopListening() = runCatching { host.stopListening() }.let { }

    fun createView(activityContext: Context, id: Int, info: AppWidgetProviderInfo): AppWidgetHostView =
        host.createView(activityContext, id, info)

    fun startAdding(
        provider: AppWidgetProviderInfo,
        target: WidgetTarget,
        wCells: Int,
        hCells: Int,
        activity: Activity,
        viewModel: LauncherViewModel,
        requestBind: (Intent) -> Unit,
    ) {
        val id = host.allocateAppWidgetId()
        pendingId = id
        pendingTarget = target
        pendingW = wCells
        pendingH = hCells
        if (manager.bindAppWidgetIdIfAllowed(id, provider.profile, provider.provider, null)) {
            configureOrSave(activity, viewModel)
        } else {
            requestBind(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile),
            )
        }
    }

    fun onBindResult(resultCode: Int, activity: Activity, viewModel: LauncherViewModel) {
        if (resultCode == Activity.RESULT_OK) configureOrSave(activity, viewModel) else discardPending()
    }

    fun onConfigureResult(resultCode: Int, viewModel: LauncherViewModel) {
        if (resultCode == Activity.RESULT_OK) savePending(viewModel) else discardPending()
    }

    /** Libera el identificador del widget y borra todo rastro suyo en la configuración. */
    fun remove(id: Int, viewModel: LauncherViewModel) {
        runCatching { host.deleteAppWidgetId(id) }
        viewModel.updateSettings { s ->
            fun isThisWidget(item: com.oasisevolve.launcher.storage.HomeItem) =
                item.kind == HomeItemKind.WIDGET && item.ref == id.toString()
            s.copy(
                widgetIds = s.widgetIds - id,
                widgetHeights = s.widgetHeights - id,
                homeLayout = s.homeLayout.filterNot(::isThisWidget),
                usefulLayout = s.usefulLayout.filterNot(::isThisWidget),
            )
        }
    }

    private fun configureOrSave(activity: Activity, viewModel: LauncherViewModel) {
        val id = pendingId ?: return
        if (manager.getAppWidgetInfo(id)?.configure == null) {
            savePending(viewModel)
            return
        }
        try {
            host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE_WIDGET, null)
        } catch (_: RuntimeException) {
            // Algunos proveedores no permiten configurarse desde fuera: se añade sin configurar.
            savePending(viewModel)
        }
    }

    private fun savePending(viewModel: LauncherViewModel) {
        val id = pendingId ?: return
        val target = pendingTarget
        val w = pendingW
        val h = pendingH
        pendingId = null
        viewModel.updateSettings { s ->
            when (target) {
                WidgetTarget.CENTER -> s.copy(widgetIds = s.widgetIds + id)
                WidgetTarget.HOME -> s.addGridItem(GridTarget.HOME) { col, row -> widgetItem(id, col, row, w, h) }
                WidgetTarget.USEFUL -> s.addGridItem(GridTarget.USEFUL) { col, row -> widgetItem(id, col, row, w, h) }
            }
        }
    }

    private fun discardPending() {
        pendingId?.let { runCatching { host.deleteAppWidgetId(it) } }
        pendingId = null
    }
}
