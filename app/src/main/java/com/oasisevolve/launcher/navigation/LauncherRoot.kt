package com.oasisevolve.launcher.navigation

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oasisevolve.launcher.AppList
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.drawer.DrawerScreen
import com.oasisevolve.launcher.focus.InterruptionScreen
import com.oasisevolve.launcher.focus.TimeUpScreen
import com.oasisevolve.launcher.home.AppActionsSheet
import com.oasisevolve.launcher.home.AppPickerSheet
import com.oasisevolve.launcher.home.DOCK_TOTAL_HEIGHT
import com.oasisevolve.launcher.home.DockIconSheet
import com.oasisevolve.launcher.home.FolderOverlay
import com.oasisevolve.launcher.home.GridDragController
import com.oasisevolve.launcher.home.GridScreen
import com.oasisevolve.launcher.library.LibraryScreen
import com.oasisevolve.launcher.settings.SettingsScreen
import com.oasisevolve.launcher.storage.GridTarget
import com.oasisevolve.launcher.storage.WidgetTarget
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.EvolveTheme
import com.oasisevolve.launcher.ui.LocalWallpaperContent
import com.oasisevolve.launcher.wellbeing.WellbeingScreen
import com.oasisevolve.launcher.widgets.WidgetCenterScreen
import com.oasisevolve.launcher.widgets.WidgetGallerySheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val WIDGETS = 0
private const val WELLBEING = 1
private const val HOME = 2
private const val USEFUL = 3
private const val DRAWER = 4
private const val LIBRARY = 5

/** Radio del desenfoque del fondo en las pantallas secundarias, en píxeles. */
private const val GLASS_BLUR_RADIUS = 60

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    homeRequests: Flow<Unit>,
    onPickWidget: (AppWidgetProviderInfo, WidgetTarget, Int, Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    val iconRevision by viewModel.iconRevision.collectAsStateWithLifecycle()
    val guardEnabled by viewModel.guardEnabled.collectAsStateWithLifecycle()
    val timeUp by viewModel.timeUp.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val settings = state.settings
    val pageCount = if (settings.libraryEnabled) LIBRARY + 1 else DRAWER + 1
    val pager = rememberPagerState(initialPage = HOME) { pageCount }
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GridTarget?>(null) }
    var actionsFor by remember { mutableStateOf<AppEntry?>(null) }
    var dockMenuFor by remember { mutableStateOf<AppEntry?>(null) }
    var dockReplaceFor by remember { mutableStateOf<AppEntry?>(null) }
    var pickerFor by remember { mutableStateOf<GridTarget?>(null) }
    var folderPickerFor by remember { mutableStateOf<Long?>(null) }
    var galleryFor by remember { mutableStateOf<WidgetTarget?>(null) }
    var openFolderId by remember { mutableStateOf<Long?>(null) }
    val openFolder = openFolderId?.let(state::folder)?.takeIf { it.apps.isNotEmpty() }
    val homeDrag = remember { GridDragController() }
    val usefulDrag = remember { GridDragController() }

    fun goTo(page: Int) {
        // Ya está ahí: animar hacia la misma página redibujaba todo durante la vuelta a Inicio.
        if (pager.currentPage == page && pager.currentPageOffsetFraction == 0f && !pager.isScrollInProgress) return
        scope.launch {
            if (settings.reduceMotion) pager.scrollToPage(page) else pager.animateScrollToPage(page)
        }
    }

    LaunchedEffect(homeRequests) {
        homeRequests.collect {
            showSettings = false
            editing = null
            actionsFor = null
            dockMenuFor = null
            dockReplaceFor = null
            pickerFor = null
            folderPickerFor = null
            galleryFor = null
            openFolderId = null
            viewModel.dismissGate()
            goTo(HOME)
        }
    }

    // El historial completo solo se calcula al ir hacia Bienestar, nunca al volver a Inicio.
    LaunchedEffect(pager.targetPage) {
        if (pager.targetPage == WELLBEING) viewModel.refreshUsage()
    }

    LaunchedEffect(pager.settledPage) {
        // Al cambiar de pantalla se termina la edición de la anterior.
        val current = when (pager.settledPage) {
            HOME -> GridTarget.HOME
            USEFUL -> GridTarget.USEFUL
            else -> null
        }
        if (editing != current) editing = null
    }

    // Android solo desenfoca la ventana entera: se aplica fuera de inicio, donde el fondo es protagonista.
    val view = LocalView.current
    val blurRadius = if (settings.glassBlur && pager.settledPage != HOME) GLASS_BLUR_RADIUS else 0
    LaunchedEffect(blurRadius) {
        if (Build.VERSION.SDK_INT >= 31) {
            (view.context as? Activity)?.window?.let { window ->
                if (blurRadius > 0) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
                window.attributes = window.attributes.apply { blurBehindRadius = blurRadius }
            }
        }
    }

    BackHandler(
        enabled = gate != null || timeUp != null || openFolder != null || showSettings || editing != null ||
            pager.currentPage != HOME,
    ) {
        when {
            gate != null -> viewModel.dismissGate()
            timeUp != null -> viewModel.dismissTimeUp()
            openFolder != null -> openFolderId = null
            showSettings -> showSettings = false
            editing != null -> editing = null
            else -> goTo(HOME)
        }
    }

    val isBusyOnGrid = editing != null || homeDrag.isDragging || usefulDrag.isDragging
    val indicatorPadding by animateDpAsState(
        if (pager.currentPage == HOME && settings.showDock) DOCK_TOTAL_HEIGHT else 6.dp,
        label = "indicator",
    )

    EvolveTheme(settings, iconRevision) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxSize()) {
                // Atenúa un poco el fondo para que el texto claro se lea también sobre zonas claras.
                if (LocalWallpaperContent.current == Color.White) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.22f)),
                    )
                }
                HorizontalPager(
                    state = pager,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isBusyOnGrid,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            // Android puede devolver el foco a cualquier elemento compuesto, y el
                            // carrusel saltaría hasta su página: las páginas no visibles no lo aceptan.
                            .focusProperties {
                                onEnter = { if (page != pager.currentPage) cancelFocusChange() }
                            }
                            .focusGroup()
                            .graphicsLayer {
                                // Sin capa intermedia: la transparencia se aplica a cada dibujo, mucho más barato
                                // que pintar la página entera en otra textura en cada fotograma del deslizamiento.
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                                if (!settings.reduceMotion) {
                                    val offset = pager.pageOffset(page).absoluteValue.coerceAtMost(1f)
                                    alpha = 1f - offset * 0.35f
                                    val scale = 1f - offset * 0.04f
                                    scaleX = scale
                                    scaleY = scale
                                }
                            },
                    ) {
                        when (page) {
                            WIDGETS -> WidgetCenterScreen(
                                viewModel = viewModel,
                                settings = settings,
                                onAddWidget = { galleryFor = WidgetTarget.CENTER },
                            )
                            WELLBEING -> WellbeingScreen(state, viewModel)
                            HOME, USEFUL -> {
                                val target = if (page == HOME) GridTarget.HOME else GridTarget.USEFUL
                                GridScreen(
                                    target = target,
                                    state = state,
                                    viewModel = viewModel,
                                    drag = if (page == HOME) homeDrag else usefulDrag,
                                    editing = editing == target,
                                    onEnterEdit = { editing = target },
                                    onExitEdit = { editing = null },
                                    onLongPressApp = { actionsFor = it },
                                    onOpenFolder = { openFolderId = it },
                                    onChooseApps = { pickerFor = target },
                                    onOpenGallery = {
                                        galleryFor = if (page == HOME) WidgetTarget.HOME else WidgetTarget.USEFUL
                                    },
                                    onSettings = { showSettings = true },
                                    onOpenWellbeing = { goTo(WELLBEING) },
                                    onDockIconMenu = { dockMenuFor = it },
                                )
                            }
                            DRAWER -> DrawerScreen(
                                state = state,
                                viewModel = viewModel,
                                isCurrentPage = pager.currentPage == DRAWER,
                                onLongPress = { actionsFor = it },
                            )
                            LIBRARY -> LibraryScreen(
                                state = state,
                                viewModel = viewModel,
                                isCurrentPage = pager.currentPage == LIBRARY,
                                onLongPress = { actionsFor = it },
                                onSettings = { showSettings = true },
                            )
                        }
                    }
                }

                PageIndicator(
                    current = pager.currentPage,
                    count = pageCount,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = indicatorPadding),
                )

                openFolder?.let { folder ->
                    FolderOverlay(
                        folder = folder,
                        state = state,
                        viewModel = viewModel,
                        onLongPress = { actionsFor = it },
                        onAddApps = { folderPickerFor = folder.folder.id },
                        onDismiss = { openFolderId = null },
                    )
                }
                if (showSettings) {
                    SettingsScreen(
                        state = state,
                        viewModel = viewModel,
                        onEditHome = {
                            showSettings = false
                            goTo(HOME)
                            editing = GridTarget.HOME
                        },
                        onClose = { showSettings = false },
                    )
                }
                gate?.let { request ->
                    InterruptionScreen(
                        request = request,
                        waitSeconds = settings.waitSeconds,
                        guardEnabled = guardEnabled,
                        onContinue = viewModel::continueGate,
                        onDismiss = {
                            viewModel.dismissGate()
                            goTo(HOME)
                        },
                    )
                }
                timeUp?.let { app ->
                    TimeUpScreen(
                        app = app,
                        todayMs = today?.apps?.firstOrNull { it.first == app.packageName }?.second,
                        onDismiss = {
                            viewModel.dismissTimeUp()
                            goTo(HOME)
                        },
                    )
                }
            }

            actionsFor?.let { app ->
                AppActionsSheet(
                    app = app,
                    state = state,
                    viewModel = viewModel,
                    onDismiss = { actionsFor = null },
                )
            }
            dockMenuFor?.let { app ->
                DockIconSheet(
                    app = app,
                    viewModel = viewModel,
                    onReplace = { dockReplaceFor = app },
                    onDismiss = { dockMenuFor = null },
                )
            }
            dockReplaceFor?.let { old ->
                AppPickerSheet(
                    title = "Reemplazar ${old.label}",
                    state = state,
                    viewModel = viewModel,
                    isSelected = { it.key == old.key },
                    onToggle = { new ->
                        viewModel.replaceInDock(old, new)
                        dockReplaceFor = null
                    },
                    onDismiss = { dockReplaceFor = null },
                )
            }
            pickerFor?.let { target ->
                val list = if (target == GridTarget.HOME) AppList.HOME else AppList.USEFUL
                AppPickerSheet(
                    title = list.label,
                    state = state,
                    viewModel = viewModel,
                    isSelected = { state.isIn(list, it) },
                    onToggle = { viewModel.toggleInGrid(target, it) },
                    onDismiss = { pickerFor = null },
                )
            }
            folderPickerFor?.let { folderId ->
                AppPickerSheet(
                    title = "Añadir a la carpeta",
                    state = state,
                    viewModel = viewModel,
                    isSelected = { app -> state.folderOf(app)?.id == folderId },
                    onToggle = { app ->
                        if (state.folderOf(app)?.id == folderId) {
                            viewModel.removeFromFolder(folderId, app)
                        } else {
                            viewModel.addToFolder(folderId, app)
                        }
                    },
                    onDismiss = { folderPickerFor = null },
                )
            }
            galleryFor?.let { target ->
                WidgetGallerySheet(
                    state = state,
                    viewModel = viewModel,
                    allowBuiltins = target != WidgetTarget.CENTER,
                    onAddProvider = { provider, w, h ->
                        onPickWidget(provider, target, w, h)
                        if (target != WidgetTarget.CENTER) {
                            editing = if (target == WidgetTarget.HOME) GridTarget.HOME else GridTarget.USEFUL
                        }
                    },
                    onAddBuiltin = { kind, w, h ->
                        val gridTarget = if (target == WidgetTarget.USEFUL) GridTarget.USEFUL else GridTarget.HOME
                        viewModel.addBuiltinWidget(gridTarget, kind, w, h)
                        editing = gridTarget
                    },
                    onDismiss = { galleryFor = null },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.pageOffset(page: Int): Float = (currentPage - page) + currentPageOffsetFraction

@Composable
private fun PageIndicator(current: Int, count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val color = LocalWallpaperContent.current.copy(alpha = if (index == current) 0.95f else 0.35f)
            Box(
                Modifier
                    .size(if (index == HOME) 7.dp else 5.dp)
                    .align(Alignment.CenterVertically)
                    .background(color, CircleShape),
            )
        }
    }
}
