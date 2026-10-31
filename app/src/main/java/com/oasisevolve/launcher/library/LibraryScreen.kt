package com.oasisevolve.launcher.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.drawer.filterBySearch
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppIcon
import com.oasisevolve.launcher.ui.AppTile
import com.oasisevolve.launcher.ui.AppTileContent
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.ui.SearchField
import com.oasisevolve.launcher.ui.onWallpaper
import com.oasisevolve.launcher.ui.reorderable
import com.oasisevolve.launcher.ui.rememberReorderState

private const val HEADER_PREFIX = "cat:"
private const val APP_PREFIX = "app:"

@Composable
fun LibraryScreen(
    state: LauncherState,
    viewModel: LauncherViewModel,
    isCurrentPage: Boolean,
    onLongPress: (AppEntry) -> Unit,
    onSettings: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var openCategory by remember { mutableStateOf<String?>(null) }
    var editingCategories by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val settings = state.settings

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            query = ""
            openCategory = null
            focusManager.clearFocus()
        }
    }

    val apps = state.allApps.filter { state.level(it).inVisualScreens && state.category(it) !in settings.hiddenCategories }
    val groups = remember(apps, settings.categoryOverrides, settings.categoryRenames, settings.categories) {
        val byCategory = apps.groupBy(state::category)
        (settings.categories + byCategory.keys).distinct()
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchField(query, onQueryChange = { query = it }, placeholder = "Biblioteca de apps")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { viewModel.updateSettings { it.copy(libraryExpanded = !it.libraryExpanded) } }) {
                Text(
                    if (settings.libraryExpanded) "Vista compacta" else "Vista expandida",
                    style = MaterialTheme.typography.labelLarge.onWallpaper(),
                )
            }
            TextButton(onClick = { editingCategories = true }) {
                Text("Categorías", style = MaterialTheme.typography.labelLarge.onWallpaper())
            }
        }

        when {
            query.isNotBlank() -> AppGrid(filterBySearch(apps, query) { it.label }, state, viewModel, onLongPress)
            settings.libraryExpanded -> ExpandedLibrary(groups, state, viewModel, onLongPress, onSettings)
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
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                item(key = "settings") { SettingsCard(onSettings) }
                items(groups, key = { it.first }) { (name, categoryApps) ->
                    CategoryCard(name, categoryApps, viewModel) { openCategory = name }
                }
            }
        }
    }

    if (editingCategories) {
        CategoryEditorSheet(settings, viewModel, onDismiss = { editingCategories = false })
    }
}

/** Todas las categorías desplegadas. Arrastrar una app sobre otra categoría la mueve allí. */
@Composable
private fun ExpandedLibrary(
    groups: List<Pair<String, List<AppEntry>>>,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onSettings: () -> Unit,
) {
    val settings = state.settings
    val grid = rememberLazyGridState()
    val categoryOfApp = groups.flatMap { (name, apps) -> apps.map { APP_PREFIX + it.key to name } }.toMap()
    val reorder = rememberReorderState(
        grid = grid,
        liveReorder = false,
        isTarget = { it is String && (it.startsWith(HEADER_PREFIX) || it.startsWith(APP_PREFIX)) },
    )
    val hover = reorder.hoverKey as? String
    val hoverCategory = hover?.let { it.removePrefix(HEADER_PREFIX).takeIf { _ -> hover.startsWith(HEADER_PREFIX) } ?: categoryOfApp[it] }

    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.gridColumns),
        state = grid,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item(key = "settings", span = { GridItemSpan(maxLineSpan) }) { SettingsCard(onSettings) }
        groups.forEach { (name, apps) ->
            item(key = HEADER_PREFIX + name, span = { GridItemSpan(maxLineSpan) }) {
                val isTarget = hoverCategory == name && reorder.draggingKey != null
                Text(
                    name,
                    style = if (isTarget) MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                    else MaterialTheme.typography.titleMedium.onWallpaper(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .then(
                            if (isTarget) Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            else Modifier,
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            items(apps, key = { APP_PREFIX + it.key }) { app ->
                val key = APP_PREFIX + app.key
                AppTileContent(
                    app = app,
                    viewModel = viewModel,
                    showIcon = true,
                    showLabel = settings.showLabels,
                    modifier = Modifier.reorderable(
                        state = reorder,
                        key = key,
                        label = app.label,
                        onTap = { viewModel.open(app) },
                        onLongPress = { onLongPress(app) },
                        onDrop = {
                            hoverCategory?.takeIf { it != name }?.let { viewModel.setCategory(app, it) }
                        },
                    ),
                )
            }
        }
    }
}

/** Acceso a los ajustes del launcher: el primer elemento de la última pantalla. */
@Composable
private fun SettingsCard(onSettings: () -> Unit) {
    GlassCard(
        Modifier
            .clickable(onClick = onSettings)
            .semantics { contentDescription = "Ajustes de Oasis Evolve" },
    ) {
        // Misma altura que las dos filas de iconos de una categoría.
        Box(Modifier.height(96.dp)) {
            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(44.dp))
        }
        Text("Ajustes", style = MaterialTheme.typography.labelLarge)
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
                    rows.getOrNull(index)?.forEach { AppIcon(it, viewModel, size = 44.dp) }
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(state.settings.gridColumns),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            AppTile(app, viewModel, showIcon = true, showLabel = state.settings.showLabels, onLongPress = onLongPress)
        }
    }
}
