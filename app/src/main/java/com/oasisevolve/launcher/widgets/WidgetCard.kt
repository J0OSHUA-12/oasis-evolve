package com.oasisevolve.launcher.widgets

import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.appwidget.AppWidgetProviderInfo
import com.oasisevolve.launcher.EvolveApp
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.WidgetTarget
import com.oasisevolve.launcher.ui.GlassCard

private val SIZE_OPTIONS = listOf("S" to 110, "M" to 180, "L" to 260, "XL" to 340)

/** Altura inicial: la que pide el propio widget, con un mínimo para que sea usable. */
private fun defaultHeightDp(minHeightPx: Int, density: Float): Int =
    (minHeightPx / density).toInt().coerceIn(110, 340)

@Composable
fun WidgetCard(
    id: Int,
    target: WidgetTarget,
    heightDp: Int?,
    editing: Boolean,
    viewModel: LauncherViewModel,
) {
    val context = LocalContext.current
    val controller = remember { (context.applicationContext as EvolveApp).widgets }
    val info = controller.info(id)
    val density = LocalDensity.current.density

    GlassCard {
        if (info == null) {
            Text("Este widget ya no está disponible.")
        } else {
            val height = heightDp ?: defaultHeightDp(info.minHeight, density)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val width = maxWidth.value.toInt()
                AndroidView(
                    factory = { viewContext -> controller.createView(viewContext, id, info) },
                    update = { view ->
                        // Sin esto, muchos widgets conservan la distribución de su tamaño mínimo.
                        @Suppress("DEPRECATION")
                        view.updateAppWidgetSize(Bundle(), width, height, width, height)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height.dp),
                )
            }
        }
        if (editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SIZE_OPTIONS.forEach { (label, size) ->
                    FilterChip(
                        selected = heightDp == size,
                        onClick = { viewModel.setWidgetHeight(id, size) },
                        label = { Text(label) },
                    )
                }
            }
            Row {
                TextButton(onClick = { viewModel.moveWidget(target, id, -1) }) { Text("Subir") }
                TextButton(onClick = { viewModel.moveWidget(target, id, 1) }) { Text("Bajar") }
                TextButton(onClick = { controller.remove(id, viewModel) }) { Text("Quitar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { (context.applicationContext as EvolveApp).widgets }
    val providers = remember { controller.providers() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .navigationBarsPadding(),
        ) {
            Text("Elige un widget", style = MaterialTheme.typography.titleLarge)
            if (providers.isEmpty()) {
                Text("Ninguna app instalada ofrece widgets.", Modifier.padding(vertical = 16.dp))
            }
            LazyColumn {
                items(providers, key = { "${it.provider.flattenToShortString()}/${it.profile.hashCode()}" }) { provider ->
                    Text(
                        controller.label(provider),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onPick(provider)
                            }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}
