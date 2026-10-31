package com.oasisevolve.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import com.oasisevolve.launcher.LauncherViewModel

const val REQUEST_CONFIGURE_WIDGET = 4101
private const val HOST_ID = 2048

/** Flujo oficial de Android para alojar widgets: asignar id, pedir vínculo, configurar y guardar. */
class WidgetHostController(private val context: Context) {
    private val manager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context, HOST_ID)
    private var pendingId: Int? = null

    fun providers(): List<AppWidgetProviderInfo> =
        manager.installedProviders.sortedBy { it.loadLabel(context.packageManager).lowercase() }

    fun label(info: AppWidgetProviderInfo): String = info.loadLabel(context.packageManager)

    fun info(id: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(id)

    fun startListening() = runCatching { host.startListening() }.let { }

    fun stopListening() = runCatching { host.stopListening() }.let { }

    fun createView(activityContext: Context, id: Int, info: AppWidgetProviderInfo): AppWidgetHostView =
        host.createView(activityContext, id, info)

    fun startAdding(
        provider: AppWidgetProviderInfo,
        activity: Activity,
        viewModel: LauncherViewModel,
        requestBind: (Intent) -> Unit,
    ) {
        val id = host.allocateAppWidgetId()
        pendingId = id
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

    fun remove(id: Int, viewModel: LauncherViewModel) {
        host.deleteAppWidgetId(id)
        viewModel.updateSettings { it.copy(widgetIds = it.widgetIds - id) }
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
        pendingId = null
        viewModel.updateSettings { it.copy(widgetIds = it.widgetIds + id) }
    }

    private fun discardPending() {
        pendingId?.let(host::deleteAppWidgetId)
        pendingId = null
    }
}
