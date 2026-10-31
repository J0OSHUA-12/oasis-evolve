package com.oasisevolve.launcher.navigation

import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oasisevolve.launcher.AppList
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.drawer.DrawerScreen
import com.oasisevolve.launcher.focus.InterruptionScreen
import com.oasisevolve.launcher.home.AppActionsSheet
import com.oasisevolve.launcher.home.AppPickerSheet
import com.oasisevolve.launcher.home.HomeScreen
import com.oasisevolve.launcher.library.LibraryScreen
import com.oasisevolve.launcher.settings.SettingsScreen
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.EvolveTheme
import com.oasisevolve.launcher.useful.UsefulScreen
import com.oasisevolve.launcher.wellbeing.WellbeingScreen
import com.oasisevolve.launcher.widgets.WidgetCenterScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val WIDGETS = 0
private const val WELLBEING = 1
private const val HOME = 2
private const val USEFUL = 3
private const val DRAWER = 4
private const val LIBRARY = 5

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    homeRequests: Flow<Unit>,
    onPickWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    val settings = state.settings
    val pageCount = if (settings.libraryEnabled) LIBRARY + 1 else DRAWER + 1
    val pager = rememberPagerState(initialPage = HOME) { pageCount }
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<AppEntry?>(null) }
    var pickerFor by remember { mutableStateOf<AppList?>(null) }

    fun goHome() {
        scope.launch {
            if (settings.reduceMotion) pager.scrollToPage(HOME) else pager.animateScrollToPage(HOME)
        }
    }

    LaunchedEffect(homeRequests) {
        homeRequests.collect {
            showSettings = false
            actionsFor = null
            pickerFor = null
            viewModel.dismissGate()
            goHome()
        }
    }

    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage == WELLBEING) viewModel.refreshUsage()
    }

    BackHandler(enabled = showSettings || pager.currentPage != HOME) {
        if (showSettings) showSettings = false else goHome()
    }

    EvolveTheme(settings.themeMode) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pager,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Box(
                        Modifier
                            .fillMaxSize()
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
                            WIDGETS -> WidgetCenterScreen(viewModel, settings, onPickWidget)
                            WELLBEING -> WellbeingScreen(viewModel)
                            HOME -> HomeScreen(
                                state = state,
                                viewModel = viewModel,
                                onLongPress = { actionsFor = it },
                                onEdit = { pickerFor = AppList.HOME },
                                onSettings = { showSettings = true },
                            )
                            USEFUL -> UsefulScreen(
                                state = state,
                                viewModel = viewModel,
                                onLongPress = { actionsFor = it },
                                onEdit = { pickerFor = it },
                            )
                            DRAWER -> DrawerScreen(state, viewModel, onLongPress = { actionsFor = it })
                            LIBRARY -> LibraryScreen(state, viewModel, onLongPress = { actionsFor = it })
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.pageOffset(page: Int): Float = (currentPage - page) + currentPageOffsetFraction

@Composable
private fun PageIndicator(current: Int, count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (index == current) 0.95f else 0.3f)
            Box(
                Modifier
                    .size(if (index == HOME) 7.dp else 5.dp)
                    .align(Alignment.CenterVertically)
                    .background(color, CircleShape),
            )
        }
    }
}
