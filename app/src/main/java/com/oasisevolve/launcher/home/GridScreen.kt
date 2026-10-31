package com.oasisevolve.launcher.home

import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.LaunchSource
import com.oasisevolve.launcher.storage.GridTarget
import com.oasisevolve.launcher.storage.HomeItem
import com.oasisevolve.launcher.storage.HomeItemKind
import com.oasisevolve.launcher.storage.MAX_DOCK_APPS
import com.oasisevolve.launcher.storage.canPlace
import com.oasisevolve.launcher.storage.effectiveRows
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppIcon
import com.oasisevolve.launcher.ui.LocalIconCorner
import com.oasisevolve.launcher.ui.LocalIconSize
import com.oasisevolve.launcher.ui.LocalWallpaperContent
import com.oasisevolve.launcher.ui.TileLabel
import com.oasisevolve.launcher.ui.onWallpaper
import com.oasisevolve.launcher.widgets.BuiltinWidget
import com.oasisevolve.launcher.widgets.GridAppWidget
import kotlin.math.roundToInt

/** Altura que ocupa el dock, para colocar el indicador de páginas justo encima. */
val DOCK_TOTAL_HEIGHT: Dp = 102.dp
private val DOCK_ICON_SIZE = 62.dp

/**
 * Cuadrícula libre de Inicio y Apps útiles. Cada elemento ocupa un rectángulo de celdas y se
 * mueve arrastrándolo. Solo Inicio lleva dock.
 */
@Composable
fun GridScreen(
    target: GridTarget,
    state: LauncherState,
    viewModel: LauncherViewModel,
    drag: GridDragController,
    editing: Boolean,
    onEnterEdit: () -> Unit,
    onExitEdit: () -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onChooseApps: () -> Unit,
    onOpenGallery: () -> Unit,
    onSettings: () -> Unit,
    onOpenWellbeing: () -> Unit,
    onDockIconMenu: (AppEntry) -> Unit,
) {
    val settings = state.settings
    val items = state.visibleItems(target)
    val rows = effectiveRows(items, settings.gridRows)
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    val showDock = target == GridTarget.HOME && settings.showDock

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenOrigin = it.positionInRoot() },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            if (editing) {
                EditBar(onChooseApps, onOpenGallery, onSettings, onExitEdit)
            }
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = settings.pageMarginDp.dp)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { onEnterEdit() }) },
            ) {
                val cellWidth = maxWidth / settings.gridColumns
                val cellHeight = maxHeight / settings.gridRows
                val density = LocalDensity.current
                val scroll = rememberScrollState()

                Box(
                    Modifier
                        .fillMaxWidth()
                        .then(if (rows > settings.gridRows) Modifier.verticalScroll(scroll) else Modifier)
                        .requiredHeight(cellHeight * rows)
                        .onGloballyPositioned { drag.gridOrigin = it.positionInRoot() },
                ) {
                    with(density) {
                        drag.cellWidth = cellWidth.toPx()
                        drag.cellHeight = cellHeight.toPx()
                    }
                    drag.columns = settings.gridColumns
                    drag.rows = rows
                    drag.items = items

                    if (editing || drag.isDragging) {
                        GridGuides(settings.gridColumns, rows, cellWidth, cellHeight)
                    }
                    DropHighlight(drag, cellWidth, cellHeight)

                    if (items.isEmpty() && !editing) {
                        Text(
                            "Mantén pulsado para añadir apps y widgets",
                            style = MaterialTheme.typography.bodyMedium.onWallpaper(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    items.forEach { item ->
                        key(item.id) {
                            GridTile(
                                item = item,
                                target = target,
                                state = state,
                                viewModel = viewModel,
                                drag = drag,
                                editing = editing,
                                cellWidth = cellWidth,
                                cellHeight = cellHeight,
                                onEnterEdit = onEnterEdit,
                                onLongPressApp = onLongPressApp,
                                onOpenFolder = onOpenFolder,
                                onOpenWellbeing = onOpenWellbeing,
                            )
                        }
                    }
                }
            }
            if (showDock) {
                Spacer(Modifier.height(22.dp))
                Dock(state, viewModel, drag, target, editing, onDockIconMenu)
            } else {
                Spacer(Modifier.height(28.dp))
            }
        }

        DragGhost(drag, state, viewModel, screenOrigin)
    }
}

@Composable
private fun EditBar(
    onChooseApps: () -> Unit,
    onOpenGallery: () -> Unit,
    onSettings: () -> Unit,
    onExitEdit: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE01A1D1F),
        contentColor = Color.White,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(
                "Apps" to onChooseApps,
                "Widgets" to onOpenGallery,
                "Ajustes" to onSettings,
                "Listo" to onExitEdit,
            ).forEach { (label, action) ->
                TextButton(onClick = action) {
                    Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Cuadrícula invisible hecha visible durante la edición: un punto por cada cruce. */
@Composable
private fun GridGuides(columns: Int, rows: Int, cellWidth: Dp, cellHeight: Dp) {
    val color = LocalWallpaperContent.current.copy(alpha = 0.22f)
    for (row in 0 until rows) {
        for (col in 0 until columns) {
            Box(
                Modifier
                    .offset(cellWidth * col + cellWidth / 2 - 2.dp, cellHeight * row + cellHeight / 2 - 2.dp)
                    .size(4.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
private fun DropHighlight(drag: GridDragController, cellWidth: Dp, cellHeight: Dp) {
    val cell = drag.drop as? DropTarget.Cell ?: return
    val element = (drag.payload as? DragPayload.GridElement)?.item
    val w = element?.w ?: 1
    val h = element?.h ?: 1
    val color = if (cell.valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Box(
        Modifier
            .offset(cellWidth * cell.col, cellHeight * cell.row)
            .size(cellWidth * w, cellHeight * h)
            .padding(4.dp)
            .background(color.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .border(1.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(18.dp)),
    )
}

@Composable
private fun GridTile(
    item: HomeItem,
    target: GridTarget,
    state: LauncherState,
    viewModel: LauncherViewModel,
    drag: GridDragController,
    editing: Boolean,
    cellWidth: Dp,
    cellHeight: Dp,
    onEnterEdit: () -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onOpenWellbeing: () -> Unit,
) {
    val settings = state.settings
    val density = LocalDensity.current
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val slop = LocalViewConfiguration.current.touchSlop
    val currentOnLongPress by rememberUpdatedState(onLongPressApp)
    val currentOnOpenFolder by rememberUpdatedState(onOpenFolder)
    val currentEnterEdit by rememberUpdatedState(onEnterEdit)

    val app = if (item.kind == HomeItemKind.APP) state.app(item.ref) else null
    val folder = if (item.kind == HomeItemKind.FOLDER) item.ref.toLongOrNull()?.let(state::folder) else null

    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var sizePx by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var resizing by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val isDragged = (drag.payload as? DragPayload.GridElement)?.item?.id == item.id
    val isMergeTarget = (drag.drop as? DropTarget.Merge)?.item?.id == item.id
    val mergeScale by animateFloatAsState(if (isMergeTarget) 1.16f else 1f, label = "merge")

    val position = with(density) {
        IntOffset((cellWidth * item.col).roundToPx(), (cellHeight * item.row).roundToPx())
    }
    val animated by animateIntOffsetAsState(
        position,
        animationSpec = if (settings.reduceMotion) snap() else spring(stiffness = 500f),
        label = "cell",
    )
    val w = resizing?.first ?: item.w
    val h = resizing?.second ?: item.h

    fun launch(entry: AppEntry) {
        val origin = IntArray(2).also(view::getLocationOnScreen)
        val left = origin[0] + rootPosition.x.roundToInt()
        val top = origin[1] + rootPosition.y.roundToInt()
        viewModel.open(
            entry,
            LaunchSource(view, Rect(left, top, left + sizePx.width.roundToInt(), top + sizePx.height.roundToInt())),
        )
    }

    val startDrag: (Offset) -> Unit = { down ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        drag.start(DragPayload.GridElement(item), rootPosition + down, down)
    }
    val endDrag: (onStill: () -> Unit) -> Unit = { onStill ->
        val moved = drag.traveled > slop
        val payload = drag.payload
        val drop = drag.drop
        drag.finish()
        if (moved && payload != null) viewModel.applyDrop(target, payload, drop) else onStill()
    }
    val longPressAction: () -> Unit = {
        when {
            app != null -> currentOnLongPress(app)
            folder != null -> currentOnOpenFolder(folder.folder.id)
            else -> currentEnterEdit()
        }
    }

    val label = app?.label ?: folder?.folder?.name ?: builtinName(item.kind)

    Box(
        Modifier
            .offset { animated }
            .size(cellWidth * w, cellHeight * h)
            .zIndex(if (resizing != null) 1f else 0f)
            .scale(mergeScale)
            .alpha(if (isDragged) 0.25f else 1f)
            .onGloballyPositioned {
                rootPosition = it.positionInRoot()
                sizePx = androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat())
            }
            .then(
                // Los elementos con contenido propio solo escuchan el arrastre: sus botones siguen activos.
                if (editing || item.kind == HomeItemKind.WIDGET) {
                    Modifier
                } else {
                    Modifier
                        .semantics {
                            contentDescription = label
                            if (app != null || folder != null) {
                                onClick(label = "Abrir $label") {
                                    if (app != null) launch(app) else folder?.let { currentOnOpenFolder(it.folder.id) }
                                    true
                                }
                            }
                            onLongClick(label = "Opciones de $label") {
                                longPressAction()
                                true
                            }
                        }
                        .pointerInput(item.id, app != null, folder != null) {
                            if (app != null || folder != null) {
                                detectTapGestures(
                                    onTap = {
                                        if (app != null) launch(app) else folder?.let { currentOnOpenFolder(it.folder.id) }
                                    },
                                )
                            }
                        }
                        .pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = startDrag,
                                onDrag = { change, delta ->
                                    change.consume()
                                    drag.move(delta)
                                },
                                onDragEnd = { endDrag(longPressAction) },
                                onDragCancel = { drag.finish() },
                            )
                        }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val inset = (settings.gridSpacingDp / 2).dp
        when (item.kind) {
            HomeItemKind.APP -> app?.let { GridAppIcon(it, viewModel, settings.showIcons, settings.showLabels) }
            HomeItemKind.FOLDER -> folder?.let { FolderTile(it, viewModel, settings.showLabels) }
            HomeItemKind.WIDGET -> GridAppWidget(
                id = item.ref.toIntOrNull(),
                onLongPress = onEnterEdit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inset),
            )
            else -> BuiltinWidget(
                kind = item.kind,
                state = state,
                viewModel = viewModel,
                onOpenWellbeing = onOpenWellbeing,
                editing = editing,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inset),
            )
        }

        if (editing) {
            // Capa de edición: sobre un widget, los toques mueven el elemento en vez de llegar a él.
            Box(
                Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Mover $label" }
                    .pointerInput(item.id, item.col, item.row) {
                        detectDragGestures(
                            onDragStart = startDrag,
                            onDrag = { change, delta ->
                                change.consume()
                                drag.move(delta)
                            },
                            onDragEnd = { endDrag {} },
                            onDragCancel = { drag.finish() },
                        )
                    },
            )
            RemoveBadge(label, Modifier.align(Alignment.TopStart)) { viewModel.removeFromGrid(target, item) }
            if (item.isResizable) {
                ResizeHandle(
                    label = label,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onResize = { delta ->
                        val newW = (item.w + (delta.x / drag.cellWidth).roundToInt())
                            .coerceIn(1, settings.gridColumns - item.col)
                        val newH = (item.h + (delta.y / drag.cellHeight).roundToInt()).coerceAtLeast(1)
                        val fits = canPlace(
                            drag.items,
                            item.col,
                            item.row,
                            newW,
                            newH,
                            settings.gridColumns,
                            maxOf(drag.rows, item.row + newH),
                            ignoreId = item.id,
                        )
                        if (fits) resizing = newW to newH
                    },
                    onEnd = {
                        resizing?.let { (rw, rh) -> viewModel.resizeGridItem(target, item.id, rw, rh) }
                        resizing = null
                    },
                )
            }
        }
    }
}

@Composable
private fun RemoveBadge(label: String, modifier: Modifier, onRemove: () -> Unit) {
    Box(
        modifier
            .size(26.dp)
            .background(Color(0xE6202325), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .semantics { contentDescription = "Quitar $label" }
            .clickable(onClick = onRemove),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ResizeHandle(label: String, modifier: Modifier, onResize: (Offset) -> Unit, onEnd: () -> Unit) {
    var total by remember { mutableStateOf(Offset.Zero) }
    val currentOnResize by rememberUpdatedState(onResize)
    val currentOnEnd by rememberUpdatedState(onEnd)
    Box(
        modifier
            .size(30.dp)
            .semantics { contentDescription = "Cambiar tamaño de $label" }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDrag = { change, delta ->
                        change.consume()
                        total += delta
                        currentOnResize(total)
                    },
                    onDragEnd = { currentOnEnd() },
                    onDragCancel = { currentOnEnd() },
                )
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(1.5.dp, Color.White, CircleShape),
        )
    }
}

@Composable
fun GridAppIcon(app: AppEntry, viewModel: LauncherViewModel, showIcon: Boolean, showLabel: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (showIcon) AppIcon(app, viewModel)
        if (showLabel || !showIcon) TileLabel(app.label)
    }
}

/** Carpeta como icono del mismo tamaño que una app, con hasta cuatro apps en miniatura. */
@Composable
fun FolderTile(
    folder: com.oasisevolve.launcher.GridEntry.Folder,
    viewModel: LauncherViewModel,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val size = LocalIconSize.current
    val mini = (size - 12.dp) / 2
    val shape = RoundedCornerShape(size * LocalIconCorner.current)
    // La carpeta recién creada aparece con un pequeño rebote.
    var appeared by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (appeared) 1f else 0.7f, spring(dampingRatio = 0.55f), label = "folder")
    androidx.compose.runtime.LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier.scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier
                .size(size)
                .background(Color(0xB81A1D1F), shape)
                .border(1.dp, Color.White.copy(alpha = 0.22f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                folder.apps.take(4).chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { AppIcon(it, viewModel, size = mini) }
                    }
                }
            }
        }
        if (showLabel) TileLabel(folder.folder.name)
    }
}

/** Dock de la referencia: contenedor oscuro, translúcido y muy redondeado, separado de la cuadrícula. */
@Composable
private fun Dock(
    state: LauncherState,
    viewModel: LauncherViewModel,
    drag: GridDragController,
    target: GridTarget,
    editing: Boolean,
    onDockIconMenu: (AppEntry) -> Unit,
) {
    val apps = state.visibleDock()
    drag.dockSlots = apps.size
    val insertion = (drag.drop as? DropTarget.Dock)?.index

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
            .onGloballyPositioned { drag.dockBounds = it.boundsInRoot() },
        shape = RoundedCornerShape(34.dp),
        color = Color(0xC4141618),
        contentColor = Color.White,
        border = BorderStroke(
            1.dp,
            if (insertion != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f),
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(86.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            if (apps.isEmpty()) {
                Text(
                    if (editing) "Arrastra aquí hasta $MAX_DOCK_APPS apps" else "Mantén pulsado Inicio y arrastra apps aquí",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            apps.forEach { app ->
                key(app.key) { DockIcon(app, viewModel, drag, target, onDockIconMenu) }
            }
        }
    }
}

@Composable
private fun DockIcon(
    app: AppEntry,
    viewModel: LauncherViewModel,
    drag: GridDragController,
    target: GridTarget,
    onDockIconMenu: (AppEntry) -> Unit,
) {
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val slop = LocalViewConfiguration.current.touchSlop
    val currentMenu by rememberUpdatedState(onDockIconMenu)
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var sizePx by remember { mutableStateOf(0f) }
    val isDragged = (drag.payload as? DragPayload.DockApp)?.key == app.key

    fun launch() {
        val origin = IntArray(2).also(view::getLocationOnScreen)
        val left = origin[0] + rootPosition.x.roundToInt()
        val top = origin[1] + rootPosition.y.roundToInt()
        viewModel.open(app, LaunchSource(view, Rect(left, top, left + sizePx.roundToInt(), top + sizePx.roundToInt())))
    }

    Box(
        Modifier
            .alpha(if (isDragged) 0.25f else 1f)
            .onGloballyPositioned {
                rootPosition = it.positionInRoot()
                sizePx = it.size.width.toFloat()
            }
            .semantics {
                contentDescription = app.label
                onClick(label = "Abrir ${app.label}") {
                    launch()
                    true
                }
                onLongClick(label = "Opciones de ${app.label}") {
                    currentMenu(app)
                    true
                }
            }
            .pointerInput(app.key) { detectTapGestures(onTap = { launch() }) }
            .pointerInput(app.key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { down ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        drag.start(DragPayload.DockApp(app.key), rootPosition + down, down)
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        drag.move(delta)
                    },
                    onDragEnd = {
                        val moved = drag.traveled > slop
                        val payload = drag.payload
                        val drop = drag.drop
                        drag.finish()
                        if (moved && payload != null) viewModel.applyDrop(target, payload, drop) else currentMenu(app)
                    },
                    onDragCancel = { drag.finish() },
                )
            },
    ) {
        AppIcon(app, viewModel, size = DOCK_ICON_SIZE)
    }
}

/** El elemento bajo el dedo. Un widget se representa con un marco: no se duplica su vista real. */
@Composable
private fun DragGhost(
    drag: GridDragController,
    state: LauncherState,
    viewModel: LauncherViewModel,
    screenOrigin: Offset,
) {
    val payload = drag.payload ?: return
    val density = LocalDensity.current
    val topLeft = drag.pointer - drag.grabOffset - screenOrigin
    val element = (payload as? DragPayload.GridElement)?.item
    val app = when (payload) {
        is DragPayload.DockApp -> state.app(payload.key)
        is DragPayload.GridElement -> if (element?.kind == HomeItemKind.APP) state.app(element.ref) else null
    }
    val folder = if (element?.kind == HomeItemKind.FOLDER) element.ref.toLongOrNull()?.let(state::folder) else null
    val width = with(density) { drag.cellWidth.toDp() * (element?.w ?: 1) }
    val height = with(density) { drag.cellHeight.toDp() * (element?.h ?: 1) }

    Box(
        Modifier
            .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            .size(width, height)
            .zIndex(10f)
            .scale(1.08f),
        contentAlignment = Alignment.Center,
    ) {
        when {
            app != null -> AppIcon(app, viewModel)
            folder != null -> FolderTile(folder, viewModel, showLabel = false)
            else -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .background(Color(0xB81A1D1F), RoundedCornerShape(24.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
            )
        }
    }
}

fun builtinName(kind: HomeItemKind): String = when (kind) {
    HomeItemKind.CLOCK -> "Reloj y fecha"
    HomeItemKind.PHOTO -> "Fotografías"
    HomeItemKind.SCREEN_TIME -> "Tiempo de pantalla"
    HomeItemKind.SEARCH -> "Búsqueda de Google"
    HomeItemKind.WIDGET -> "Widget"
    HomeItemKind.APP -> "App"
    HomeItemKind.FOLDER -> "Carpeta"
}
