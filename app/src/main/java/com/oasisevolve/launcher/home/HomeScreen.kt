package com.oasisevolve.launcher.home

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oasisevolve.launcher.AppList
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.WidgetTarget
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppTile
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.ui.LocalWallpaperContent
import com.oasisevolve.launcher.ui.onWallpaper
import com.oasisevolve.launcher.widgets.WidgetCard
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherState,
    viewModel: LauncherViewModel,
    editingWidgets: Boolean,
    onFinishEditingWidgets: () -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onChooseApps: () -> Unit,
    onHomeMenu: () -> Unit,
    onSettings: () -> Unit,
) {
    val settings = state.settings
    val dockApps = state.visible(AppList.DOCK)
    val widgetIds = if (settings.showHomeWidgets) settings.homeWidgetIds else emptyList()
    val grid = rememberLazyGridState()
    val drag = rememberEntryDrag(AppList.HOME, "", state, viewModel, grid)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = settings.pageMarginDp.dp)
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onClick = {},
                onLongClick = onHomeMenu,
                onLongClickLabel = "Editar la pantalla principal",
            ),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Ajustes",
                    tint = LocalWallpaperContent.current.copy(alpha = 0.8f),
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(settings.gridColumns),
            state = grid,
            verticalArrangement = Arrangement.spacedBy(settings.gridSpacingDp.dp),
            horizontalArrangement = Arrangement.spacedBy(settings.gridSpacingDp.dp / 2),
            contentPadding = PaddingValues(bottom = 12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (settings.showClock) {
                item(key = "clock", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.padding(bottom = 16.dp)) { Clock() }
                }
            }
            items(
                items = widgetIds,
                key = { "widget:$it" },
                span = { GridItemSpan(maxLineSpan) },
            ) { id ->
                WidgetCard(
                    id = id,
                    target = WidgetTarget.HOME,
                    heightDp = settings.widgetHeights[id],
                    editing = editingWidgets,
                    viewModel = viewModel,
                )
            }
            if (editingWidgets) {
                item(key = "done", span = { GridItemSpan(maxLineSpan) }) {
                    TextButton(onClick = onFinishEditingWidgets) {
                        Text("Terminar de editar", style = MaterialTheme.typography.labelLarge.onWallpaper())
                    }
                }
            }
            if (drag.entries.isEmpty() && widgetIds.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    GlassCard(Modifier.clickable(onClick = onChooseApps)) {
                        Text("Tu pantalla está en blanco a propósito", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Toca aquí para elegir las pocas apps que quieras ver. Mantén pulsado un espacio libre para añadir widgets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            entryItems(drag, state, viewModel, onLongPress, onOpenFolder)
        }

        if (settings.showDock && dockApps.isNotEmpty()) {
            GlassCard(Modifier.padding(bottom = 18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    dockApps.take(5).forEach { app ->
                        AppTile(app, viewModel, settings.showIcons, !settings.showIcons, onLongPress, onSurface = true)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun Clock() {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val time = DateFormat.getTimeFormat(context).format(Date(now))
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")
    val date = DateFormat.format(pattern, now).toString().replaceFirstChar { it.titlecase() }

    Column(horizontalAlignment = Alignment.Start) {
        Text(
            time,
            fontSize = 64.sp,
            lineHeight = 72.sp,
            fontWeight = FontWeight.Light,
            style = MaterialTheme.typography.displayLarge.onWallpaper(),
        )
        Text(
            date,
            style = MaterialTheme.typography.titleMedium.onWallpaper(alpha = 0.9f),
            fontWeight = FontWeight.Normal,
        )
    }
}
