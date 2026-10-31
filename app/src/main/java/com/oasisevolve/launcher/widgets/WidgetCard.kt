package com.oasisevolve.launcher.widgets

import android.appwidget.AppWidgetHostView
import android.os.Bundle
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.oasisevolve.launcher.EvolveApp
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.ui.GlassCard

private val SIZE_OPTIONS = listOf("S" to 110, "M" to 180, "L" to 260, "XL" to 340)

/** Altura inicial: la que pide el propio widget, con un mínimo para que sea usable. */
private fun defaultHeightDp(minHeightPx: Int, density: Float): Int =
    (minHeightPx / density).toInt().coerceIn(110, 340)

/** Widget externo dentro de la cuadrícula. Un widget que falle no debe tumbar la pantalla. */
@Composable
fun GridAppWidget(id: Int?, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val controller = remember { (context.applicationContext as EvolveApp).widgets }
    val info = id?.let(controller::info)

    if (id == null || info == null) {
        Box(
            modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xC4141618))
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Este widget ya no está disponible", color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    BoxWithConstraints(modifier.clip(RoundedCornerShape(24.dp))) {
        val width = maxWidth.value.toInt()
        val height = maxHeight.value.toInt()
        AndroidView(
            factory = { viewContext ->
                runCatching { controller.createView(viewContext, id, info) }.getOrElse {
                    TextView(viewContext).apply { text = "No se pudo mostrar este widget" }
                }.also { view ->
                    (view as? EvolveWidgetHostView)?.onLongPress = { currentOnLongPress() }
                }
            },
            update = { view ->
                // Sin esto, muchos widgets conservan la distribución de su tamaño mínimo.
                @Suppress("DEPRECATION")
                runCatching { (view as? AppWidgetHostView)?.updateAppWidgetSize(Bundle(), width, height, width, height) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Widget en el Widget Center, una lista vertical con alturas predefinidas. */
@Composable
fun WidgetCard(
    id: Int,
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
                    factory = { viewContext ->
                        runCatching { controller.createView(viewContext, id, info) }.getOrElse {
                            TextView(viewContext).apply { text = "No se pudo mostrar este widget" }
                        }
                    },
                    update = { view ->
                        @Suppress("DEPRECATION")
                        runCatching {
                            (view as? AppWidgetHostView)?.updateAppWidgetSize(Bundle(), width, height, width, height)
                        }
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
                TextButton(onClick = { viewModel.moveWidget(id, -1) }) { Text("Subir") }
                TextButton(onClick = { viewModel.moveWidget(id, 1) }) { Text("Bajar") }
                TextButton(onClick = { controller.remove(id, viewModel) }) { Text("Quitar") }
            }
        }
    }
}
