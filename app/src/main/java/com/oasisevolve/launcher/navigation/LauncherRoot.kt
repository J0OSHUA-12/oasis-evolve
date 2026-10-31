package com.oasisevolve.launcher.navigation

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import com.oasisevolve.launcher.home.AppActionsSheet
import com.oasisevolve.launcher.home.AppPickerSheet
import com.oasisevolve.launcher.home.FolderOverlay
import com.oasisevolve.launcher.home.HomeMenuSheet
import com.oasisevolve.launcher.home.HomeScreen
import com.oasisevolve.launcher.library.LibraryScreen
import com.oasisevolve.launcher.settings.SettingsScreen
import com.oasisevolve.launcher.storage.WidgetTarget
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.EvolveTheme
import com.oasisevolve.launcher.ui.LocalWallpaperContent
import com.oasisevolve.launcher.useful.UsefulScreen
import com.oasisevolve.launcher.wellbeing.WellbeingScreen
import com.oasisevolve.launcher.widgets.WidgetCenterScreen
import com.oasisevolve.launcher.widgets.WidgetPickerSheet
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
    onPickWidget: (AppWidgetProviderInfo, WidgetTarget) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    val iconRevision by viewModel.iconRevision.collectAsStateWithLifecycle()
    val settings = state.settings
    val pageCount = if (settings.libraryEnabled) LIBRARY + 1 else DRAWER + 1
    val pager = rememberPagerState(initialPage = HOME) { pageCount }
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var showHomeMenu by remember { mutableStateOf(false) }
    var editingHomeWidgets by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<AppEntry?>(null) }
    var pickerFor by remember { mutableStateOf<AppList?>(null) }
    var widgetPickerFor by remember { mutableStateOf<WidgetTarget?>(null) }
    var openFolderId by remember { mutableStateOf<Long?>(null) }
    val openFolder = openFolderId?.let(state::folder)?.takeIf { it.apps.isNotEmpty() }

    fun goHome() {
        scope.launch {
            if (settings.reduceMotion) pager.scrollToPage(HOME) else pager.animateScrollToPage(HOME)
        }
    }

    LaunchedEffect(homeRequests) {
        homeRequests.collect {
            showSettings = false
            showHomeMenu = false
            editingHomeWidgets = false
            actionsFor = null
            pickerFor = null
            widgetPickerFor = null
            openFolderId = null
            viewModel.dismissGate()
            goHome()
        }
    }

    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage == WELLBEING) viewModel.refreshUsage()
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
        enabled = gate != null || openFolder != null || showSettings || editingHomeWidgets || pager.currentPage != HOME,
    ) {
        when {
            gate != null -> viewModel.dismissGate()
            openFolder != null -> openFolderId = null
            showSettings -> showSettings = false
            editingHomeWidgets -> editingHomeWidgets = false
            else -> goHome()
        }
    }

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
                                onAddWidget = { widgetPickerFor = WidgetTarget.CENTER },
                            )
                            WELLBEING -> WellbeingScreen(viewModel)
                            HOME -> HomeScreen(
                                state = state,
                                viewModel = viewModel,
                                editingWidgets = editingHomeWidgets,
                                onFinishEditingWidgets = { editingHomeWidgets = false },
                                onLongPress = { actionsFor = it },
                                onOpenFolder = { openFolderId = it },
                                onChooseApps = { pickerFor = AppList.HOME },
                                onHomeMenu = { showHomeMenu = true },
                                onSettings = { showSettings = true },
                            )
                            USEFUL -> UsefulScreen(
                                state = state,
                                viewModel = viewModel,
                                onLongPress = { actionsFor = it },
                                onOpenFolder = { openFolderId = it },
                                onEdit = { pickerFor = it },
                            )
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
                        .padding(bottom = 6.dp),
                )

                openFolder?.let { folder ->
                    FolderOverlay(
                        folder = folder,
                        state = state,
                        viewModel = viewModel,
                        onLongPress = { actionsFor = it },
                        onDismiss = { openFolderId = null },
                    )
                }
                if (showSettings) {
                    SettingsScreen(state, viewModel, onClose = { showSettings = false })
                }
                gate?.let { request ->
                    InterruptionScreen(
                        request = request,
                        waitSeconds = settings.waitSeconds,
                        onContinue = viewModel::continueGate,
                        onDismiss = {
                            viewModel.dismissGate()
                            goHome()
                        },
                    )
                }
            }

            if (showHomeMenu) {
                HomeMenuSheet(
                    hasWidgets = settings.homeWidgetIds.isNotEmpty(),
                    onChooseApps = { pickerFor = AppList.HOME },
                    onAddWidget = { widgetPickerFor = WidgetTarget.HOME },
                    onEditWidgets = { editingHomeWidgets = true },
                    onSettings = { showSettings = true },
                    onDismiss = { showHomeMenu = false },
                )
            }
            actionsFor?.let { app ->
                AppActionsSheet(
                    app = app,
                    state = state,
                    viewModel = viewModel,
                    onDismiss = { actionsFor = null },
                )
            }
            pickerFor?.let { list ->
                AppPickerSheet(
                    list = list,
                    state = state,
                    viewModel = viewModel,
                    onDismiss = { pickerFor = null },
                )
            }
            widgetPickerFor?.let { target ->
                WidgetPickerSheet(
                    onPick = { provider -> onPickWidget(provider, target) },
                    onDismiss = { widgetPickerFor = null },
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
