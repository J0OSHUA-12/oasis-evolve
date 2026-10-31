package com.oasisevolve.launcher.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.oasisevolve.launcher.EvolveApp
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.home.builtinName
import com.oasisevolve.launcher.storage.HomeItemKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/** Tamaño de referencia de una celda para convertir las medidas que declara un widget. */
private const val REFERENCE_CELL_DP = 80f

private data class CellSize(val w: Int, val h: Int) {
    val label: String get() = "$w × $h"
}

private sealed interface GalleryEntry {
    val name: String
    val appName: String
    val sizes: List<CellSize>

    data class Builtin(
        val kind: HomeItemKind,
        override val sizes: List<CellSize>,
    ) : GalleryEntry {
        override val name: String get() = builtinName(kind)
        override val appName: String get() = "Oasis Evolve"
    }

    data class Provider(
        val info: AppWidgetProviderInfo,
        override val name: String,
        override val appName: String,
        override val sizes: List<CellSize>,
        val fromOtherLauncher: Boolean,
    ) : GalleryEntry
}

private const val OTHER_LAUNCHER_NOTE = "Es de otro launcher: puede quedar vacío aquí."

private val BUILTINS = listOf(
    GalleryEntry.Builtin(HomeItemKind.CLOCK, listOf(CellSize(2, 2), CellSize(4, 2))),
    GalleryEntry.Builtin(HomeItemKind.PHOTO, listOf(CellSize(2, 2), CellSize(4, 2), CellSize(4, 3))),
    GalleryEntry.Builtin(HomeItemKind.SCREEN_TIME, listOf(CellSize(2, 2), CellSize(4, 2))),
    GalleryEntry.Builtin(HomeItemKind.SEARCH, listOf(CellSize(4, 1))),
)

private fun cellsFor(dp: Float): Int = ceil(dp / REFERENCE_CELL_DP).toInt().coerceAtLeast(1)

/** Tamaños reales: el que el widget pide y, si admite cambiarlo, los habituales dentro de sus límites. */
private fun sizesFor(context: Context, info: AppWidgetProviderInfo, columns: Int): List<CellSize> {
    val density = context.resources.displayMetrics.density
    val declaredW = if (Build.VERSION.SDK_INT >= 31 && info.targetCellWidth > 0) info.targetCellWidth else cellsFor(info.minWidth / density)
    val declaredH = if (Build.VERSION.SDK_INT >= 31 && info.targetCellHeight > 0) info.targetCellHeight else cellsFor(info.minHeight / density)
    val base = CellSize(declaredW.coerceAtMost(columns), declaredH)

    val canResizeW = info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
    val canResizeH = info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0
    val minW = if (canResizeW && info.minResizeWidth > 0) cellsFor(info.minResizeWidth / density) else base.w
    val minH = if (canResizeH && info.minResizeHeight > 0) cellsFor(info.minResizeHeight / density) else base.h

    val options = listOf(CellSize(2, 1), CellSize(2, 2), CellSize(4, 1), CellSize(4, 2), CellSize(4, 3), CellSize(4, 4))
        .filter { option ->
            option.w <= columns &&
                (if (canResizeW) option.w >= minW else option.w == base.w) &&
                (if (canResizeH) option.h >= minH else option.h == base.h)
        }
    return (listOf(base) + options).distinct().sortedWith(compareBy({ it.w * it.h }, { it.w }))
}

/** Galería de widgets: propios y de las apps instaladas, cada uno con su vista previa real. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetGallerySheet(
    state: LauncherState,
    viewModel: LauncherViewModel,
    allowBuiltins: Boolean,
    onAddProvider: (AppWidgetProviderInfo, Int, Int) -> Unit,
    onAddBuiltin: (HomeItemKind, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { (context.applicationContext as EvolveApp).widgets }
    val entries by produceState<List<GalleryEntry>?>(null) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val columns = state.settings.gridColumns
            val launchers = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .map { it.activityInfo.packageName }
                .toSet()
            controller.providers().map { info ->
                val appName = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                }.getOrDefault(info.provider.packageName)
                GalleryEntry.Provider(
                    info,
                    controller.label(info),
                    appName,
                    sizesFor(context, info, columns),
                    fromOtherLauncher = info.provider.packageName in launchers,
                )
            }.sortedWith(compareBy({ it.appName.lowercase() }, { it.name.lowercase() }))
        }
    }
    var selected by remember { mutableStateOf<GalleryEntry?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val detail = selected
        if (detail != null) {
            WidgetDetail(
                entry = detail,
                state = state,
                viewModel = viewModel,
                onBack = { selected = null },
                onAdd = { size ->
                    onDismiss()
                    when (detail) {
                        is GalleryEntry.Builtin -> onAddBuiltin(detail.kind, size.w, size.h)
                        is GalleryEntry.Provider -> onAddProvider(detail.info, size.w, size.h)
                    }
                },
            )
            return@ModalBottomSheet
        }

        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Widgets", style = MaterialTheme.typography.headlineSmall)
            val list = entries
            if (list == null) {
                Text("Cargando widgets…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            val all = (if (allowBuiltins) BUILTINS else emptyList()) + list
            if (all.isEmpty()) {
                Text("Ninguna app instalada ofrece widgets.")
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(all, key = { entry ->
                    when (entry) {
                        is GalleryEntry.Builtin -> "builtin/${entry.kind}"
                        is GalleryEntry.Provider -> "${entry.info.provider.flattenToShortString()}/${entry.info.profile.hashCode()}"
                    }
                }) { entry ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .clickable { selected = entry }
                            .padding(12.dp)
                            .semantics { contentDescription = "Widget ${entry.name} de ${entry.appName}" },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WidgetPreview(entry, state, viewModel, height = 120.dp, onTap = { selected = entry })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    entry.appName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if ((entry as? GalleryEntry.Provider)?.fromOtherLauncher == true) {
                                    Text(
                                        OTHER_LAUNCHER_NOTE,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                "${entry.sizes.first().label} celdas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetDetail(
    entry: GalleryEntry,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    onAdd: (CellSize) -> Unit,
) {
    var size by remember { mutableStateOf(entry.sizes.first()) }
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(entry.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            entry.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WidgetPreview(entry, state, viewModel, height = 240.dp, onTap = {})
        (entry as? GalleryEntry.Provider)?.info?.let { info ->
            val description = remember(info) {
                if (Build.VERSION.SDK_INT >= 31) info.loadDescription(viewModel.getApplication())?.toString() else null
            }
            description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if ((entry as? GalleryEntry.Provider)?.fromOtherLauncher == true) {
            Text(OTHER_LAUNCHER_NOTE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Text("Tamaño aproximado: ${size.label} celdas", style = MaterialTheme.typography.bodyLarge)
        if (entry.sizes.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                entry.sizes.forEach { option ->
                    FilterChip(
                        selected = option == size,
                        onClick = { size = option },
                        label = { Text(option.label) },
                    )
                }
            }
        }
        Text(
            "Después de añadirlo podrás moverlo y cambiar su tamaño en la pantalla.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("Volver") }
            Button(onClick = { onAdd(size) }) { Text("Añadir") }
        }
    }
}

@Composable
private fun WidgetPreview(
    entry: GalleryEntry,
    state: LauncherState,
    viewModel: LauncherViewModel,
    height: Dp,
    onTap: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        when (entry) {
            is GalleryEntry.Builtin -> {
                // Los widgets propios se dibujan de verdad: es su aspecto real, con datos reales.
                val first = entry.sizes.first()
                val cell = (height - 16.dp) / maxOf(first.h, 1)
                Box(Modifier.size(minOf(cell * first.w, 320.dp), cell * first.h)) {
                    BuiltinWidget(
                        kind = entry.kind,
                        state = state,
                        viewModel = viewModel,
                        onOpenWellbeing = {},
                        editing = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Es una muestra: tocarla selecciona el widget, no lo usa.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTap,
                            ),
                    )
                }
            }
            is GalleryEntry.Provider -> ProviderPreview(entry.info)
        }
    }
}

private sealed interface PreviewContent {
    data class Picture(val image: ImageBitmap) : PreviewContent
    data class Layout(val views: RemoteViews) : PreviewContent
    data class None(val icon: ImageBitmap?) : PreviewContent
}

/**
 * Lo que la propia app publica: imagen, diseño o vista generada. Si no publica nada se indica
 * así y se muestra su icono; nunca una vista previa inventada.
 */
@Composable
private fun ProviderPreview(info: AppWidgetProviderInfo) {
    val context = LocalContext.current
    val preview by produceState<PreviewContent?>(null, info) {
        value = withContext(Dispatchers.IO) { loadPreview(context, info) }
    }
    when (val content = preview) {
        null -> Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        is PreviewContent.Picture -> Image(
            bitmap = content.image,
            contentDescription = "Vista previa",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
        )
        is PreviewContent.Layout -> AndroidView(
            factory = { viewContext ->
                FrameLayout(viewContext).apply {
                    runCatching { addView(content.views.apply(viewContext, this)) }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
        )
        is PreviewContent.None -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(10.dp),
        ) {
            content.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(44.dp)) }
            Text(
                "Esta app no publica una vista previa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun loadPreview(context: Context, info: AppWidgetProviderInfo): PreviewContent {
    val density = context.resources.displayMetrics.densityDpi
    if (Build.VERSION.SDK_INT >= 35) {
        runCatching {
            AppWidgetManager.getInstance(context).getWidgetPreview(
                info.provider,
                info.profile,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            )
        }.getOrNull()?.let { return PreviewContent.Layout(it) }
    }
    runCatching { info.loadPreviewImage(context, density) }.getOrNull()?.let { drawable ->
        drawablePicture(drawable)?.let { return PreviewContent.Picture(it) }
    }
    if (Build.VERSION.SDK_INT >= 31 && info.previewLayout != 0) {
        return PreviewContent.Layout(RemoteViews(info.provider.packageName, info.previewLayout))
    }
    val icon = runCatching { info.loadIcon(context, density) }.getOrNull()?.let(::drawablePicture)
    return PreviewContent.None(icon)
}

private fun drawablePicture(drawable: Drawable): ImageBitmap? = runCatching {
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 256
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 256
    // Limita la memoria: una vista previa no necesita más resolución que la de la tarjeta.
    val scale = minOf(1f, 900f / maxOf(width, height))
    drawable.toBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1)).asImageBitmap()
}.getOrNull()
