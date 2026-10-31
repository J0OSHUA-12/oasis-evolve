package com.oasisevolve.launcher.useful

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.AppList
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.home.EntryDrag
import com.oasisevolve.launcher.home.entryItems
import com.oasisevolve.launcher.home.rememberEntryDrag
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.onWallpaper

@Composable
fun UsefulScreen(
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onEdit: (AppList) -> Unit,
) {
    val settings = state.settings
    val grid = rememberLazyGridState()
    val wantMore = rememberEntryDrag(AppList.WANT_MORE, "want/", state, viewModel, grid)
    val useful = rememberEntryDrag(AppList.USEFUL, "useful/", state, viewModel, grid)

    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.gridColumns),
        state = grid,
        verticalArrangement = Arrangement.spacedBy(settings.gridSpacingDp.dp),
        horizontalArrangement = Arrangement.spacedBy(settings.gridSpacingDp.dp / 2),
        contentPadding = PaddingValues(bottom = 28.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = settings.pageMarginDp.dp),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Apps que me aportan valor",
                style = MaterialTheme.typography.headlineMedium.onWallpaper(),
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
        }
        section(
            title = "Quiero usar más",
            subtitle = "Las eliges tú, sin importar cuánto las abres ahora.",
            drag = wantMore,
            state = state,
            viewModel = viewModel,
            onLongPress = onLongPress,
            onOpenFolder = onOpenFolder,
            onEdit = { onEdit(AppList.WANT_MORE) },
        )
        section(
            title = "Útiles",
            subtitle = "Herramientas que de verdad te sirven.",
            drag = useful,
            state = state,
            viewModel = viewModel,
            onLongPress = onLongPress,
            onOpenFolder = onOpenFolder,
            onEdit = { onEdit(AppList.USEFUL) },
        )
    }
}

private fun LazyGridScope.section(
    title: String,
    subtitle: String,
    drag: EntryDrag,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onEdit: () -> Unit,
) {
    item(key = "header/$title", span = { GridItemSpan(maxLineSpan) }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge.onWallpaper())
            Text(
                "Editar",
                style = MaterialTheme.typography.labelLarge.onWallpaper(),
                modifier = Modifier
                    .clickable(onClick = onEdit)
                    .padding(8.dp),
            )
        }
    }
    item(key = "subtitle/$title", span = { GridItemSpan(maxLineSpan) }) {
        Text(
            if (drag.entries.isEmpty()) "$subtitle Toca Editar para añadirlas." else subtitle,
            style = MaterialTheme.typography.bodySmall.onWallpaper(alpha = 0.85f),
        )
    }
    entryItems(drag, state, viewModel, onLongPress, onOpenFolder)
}
