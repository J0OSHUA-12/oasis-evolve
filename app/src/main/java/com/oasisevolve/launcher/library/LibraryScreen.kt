package com.oasisevolve.launcher.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.drawer.filterBySearch
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppIcon
import com.oasisevolve.launcher.ui.AppTile
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.ui.SearchField
import com.oasisevolve.launcher.ui.onWallpaper

@Composable
fun LibraryScreen(
    state: LauncherState,
    viewModel: LauncherViewModel,
    isCurrentPage: Boolean,
    onLongPress: (AppEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var openCategory by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            query = ""
            openCategory = null
            focusManager.clearFocus()
        }
    }

    val apps = state.allApps.filter { state.level(it).inVisualScreens }
    val groups = remember(apps, state.settings.categoryOverrides) {
        val byCategory = apps.groupBy(state::category)
        (DEFAULT_CATEGORIES + byCategory.keys).distinct()
            .mapNotNull { name -> byCategory[name]?.let { name to it } }
    }
    val opened = openCategory?.let { name -> groups.firstOrNull { it.first == name } }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchField(query, onQueryChange = { query = it }, placeholder = "Biblioteca de apps")

        when {
            query.isNotBlank() -> AppGrid(filterBySearch(apps, query) { it.label }, state, viewModel, onLongPress)
            opened != null -> {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        opened.first,
                        style = MaterialTheme.typography.headlineSmall.onWallpaper(),
                        fontWeight = FontWeight.Light,
                    )
                    TextButton(onClick = { openCategory = null }) {
                        Text("Cerrar", style = MaterialTheme.typography.labelLarge.onWallpaper())
                    }
                }
                AppGrid(opened.second, state, viewModel, onLongPress)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(groups, key = { it.first }) { (name, categoryApps) ->
                    CategoryCard(name, categoryApps, viewModel) { openCategory = name }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    name: String,
    apps: List<AppEntry>,
    viewModel: LauncherViewModel,
    onOpen: () -> Unit,
) {
    GlassCard(Modifier.clickable(onClick = onOpen)) {
        val rows = apps.take(4).chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Siempre dos filas, para que todas las tarjetas tengan la misma altura.
            repeat(2) { index ->
                Row(Modifier.height(44.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.getOrNull(index)?.forEach { AppIcon(it, viewModel, 44.dp) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                apps.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppEntry>,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
) {
    LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(apps, key = { it.key }) { app ->
            AppTile(app, viewModel, showIcon = true, showLabel = state.settings.showLabels, onLongPress = onLongPress)
        }
    }
}
