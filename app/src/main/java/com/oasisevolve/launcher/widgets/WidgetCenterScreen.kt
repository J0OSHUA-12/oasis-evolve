package com.oasisevolve.launcher.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.ui.onWallpaper

@Composable
fun WidgetCenterScreen(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    onAddWidget: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Widgets",
                    style = MaterialTheme.typography.headlineMedium.onWallpaper(),
                    fontWeight = FontWeight.Light,
                )
                if (settings.widgetIds.isNotEmpty()) {
                    TextButton(onClick = { editing = !editing }) {
                        Text(
                            if (editing) "Listo" else "Editar",
                            style = MaterialTheme.typography.labelLarge.onWallpaper(),
                        )
                    }
                }
            }
        }
        items(settings.widgetIds, key = { it }) { id ->
            WidgetCard(
                id = id,
                heightDp = settings.widgetHeights[id],
                editing = editing,
                viewModel = viewModel,
            )
        }
        item {
            Button(onClick = onAddWidget) { Text("Añadir widget") }
            Box(Modifier.height(28.dp))
        }
    }
}
