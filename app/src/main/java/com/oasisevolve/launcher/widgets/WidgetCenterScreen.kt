package com.oasisevolve.launcher.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.oasisevolve.launcher.EvolveApp
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.LauncherSettings
import com.oasisevolve.launcher.ui.GlassCard
import com.oasisevolve.launcher.ui.onWallpaper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCenterScreen(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    onPickWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { (context.applicationContext as EvolveApp).widgets }
    var picking by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Widgets",
                style = MaterialTheme.typography.headlineMedium.onWallpaper(),
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        items(settings.widgetIds, key = { it }) { id ->
            val info = controller.info(id)
            GlassCard {
                if (info == null) {
                    Text("Este widget ya no está disponible.")
                } else {
                    val height = with(LocalDensity.current) {
                        info.minHeight.toDp().coerceAtLeast(110.dp)
                    }
                    AndroidView(
                        factory = { viewContext ->
                            controller.createView(viewContext, id, info)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height),
                    )
                }
                TextButton(onClick = { controller.remove(id, viewModel) }) { Text("Quitar") }
            }
        }
        item {
            Button(onClick = { picking = true }) { Text("Añadir widget") }
            Box(Modifier.height(28.dp))
        }
    }

    if (picking) {
        val providers = remember { controller.providers() }
        ModalBottomSheet(
            onDismissRequest = { picking = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding()) {
                Text("Elige un widget", style = MaterialTheme.typography.titleLarge)
                if (providers.isEmpty()) {
                    Text("Ninguna app instalada ofrece widgets.", Modifier.padding(vertical = 16.dp))
                }
                LazyColumn {
                    items(providers, key = { "${it.provider.flattenToShortString()}/${it.profile.hashCode()}" }) { provider ->
                        Text(
                            controller.label(provider),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    picking = false
                                    onPickWidget(provider)
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
