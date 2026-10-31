package com.oasisevolve.launcher.useful

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.items
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
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.AppTile

@Composable
fun UsefulScreen(
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onEdit: (AppList) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(state.settings.gridColumns),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Apps que me aportan valor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
        }
        section(
            title = "Quiero usar más",
            subtitle = "Las eliges tú, sin importar cuánto las abres ahora.",
            apps = state.visible(AppList.WANT_MORE),
            state = state,
            viewModel = viewModel,
            onLongPress = onLongPress,
            onEdit = { onEdit(AppList.WANT_MORE) },
        )
        section(
            title = "Útiles",
            subtitle = "Herramientas que de verdad te sirven.",
            apps = state.visible(AppList.USEFUL),
            state = state,
            viewModel = viewModel,
            onLongPress = onLongPress,
            onEdit = { onEdit(AppList.USEFUL) },
        )
    }
}

private fun LazyGridScope.section(
    title: String,
    subtitle: String,
    apps: List<AppEntry>,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
    onEdit: () -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Editar",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable(onClick = onEdit)
                    .padding(8.dp),
            )
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            if (apps.isEmpty()) "$subtitle Toca Editar para añadirlas." else subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
    items(apps, key = { "$title/${it.key}" }) { app ->
        AppTile(app, viewModel, state.settings.showIcons, state.settings.showLabels, onLongPress)
    }
}
