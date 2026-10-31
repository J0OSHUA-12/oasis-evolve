package com.oasisevolve.launcher.widgets

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.AlarmClock
import android.speech.RecognizerIntent
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.R
import com.oasisevolve.launcher.storage.HomeItemKind
import com.oasisevolve.launcher.ui.AppIcon
import com.oasisevolve.launcher.usage.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(28.dp)
private val LightCard = Color(0xF2F1F2F2)
private val DarkInk = Color(0xFF1B1C1E)
private const val GOOGLE_APP = "com.google.android.googlequicksearchbox"

@Composable
fun BuiltinWidget(
    kind: HomeItemKind,
    state: LauncherState,
    viewModel: LauncherViewModel,
    onOpenWellbeing: () -> Unit,
    editing: Boolean,
    modifier: Modifier = Modifier,
) {
    when (kind) {
        HomeItemKind.CLOCK -> ClockWidget(modifier)
        HomeItemKind.PHOTO -> PhotoWidget(state, viewModel, modifier)
        HomeItemKind.SCREEN_TIME -> ScreenTimeWidget(state, viewModel, onOpenWellbeing, modifier)
        HomeItemKind.SEARCH -> SearchWidget(state, viewModel, modifier)
        else -> Unit
    }
}

private fun Context.startSafely(intent: Intent, failure: String): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    Toast.makeText(this, failure, Toast.LENGTH_SHORT).show()
    false
} catch (_: SecurityException) {
    Toast.makeText(this, failure, Toast.LENGTH_SHORT).show()
    false
}

/** Hora, día y fecha reales del teléfono. No incluye clima: consultarlo exigiría acceso a Internet. */
@Composable
private fun ClockWidget(modifier: Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val locale = Locale.getDefault()
    val time = DateFormat.format(if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm", now).toString()
    val day = android.icu.text.SimpleDateFormat("EEE", locale).format(Date(now)).trimEnd('.').uppercase(locale)
    val date = android.icu.text.SimpleDateFormat("MMM d", locale).format(Date(now)).replace(".", "").uppercase(locale)

    Column(
        modifier
            .clip(CardShape)
            .background(LightCard)
            .clickable {
                context.startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS), "No se pudo abrir el reloj")
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = "Reloj: $time, $day $date" },
        verticalArrangement = Arrangement.Center,
    ) {
        Text(time, color = DarkInk, fontSize = 38.sp, lineHeight = 44.sp, fontFamily = FontFamily.Monospace)
        Text(day, color = Color(0xFFE8631C), fontSize = 28.sp, lineHeight = 34.sp, fontFamily = FontFamily.Monospace)
        Text(date, color = DarkInk, fontSize = 28.sp, lineHeight = 34.sp, fontFamily = FontFamily.Monospace)
    }
}

/** Una foto elegida por el usuario. Cambia cada día, o al tocarla. */
@Composable
private fun PhotoWidget(state: LauncherState, viewModel: LauncherViewModel, modifier: Modifier) {
    val photos = state.settings.photos
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris -> viewModel.setPhotos(uris) }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    var skip by remember { mutableIntStateOf(0) }
    if (photos.isEmpty()) {
        Column(
            modifier
                .clip(CardShape)
                .background(Color(0xC4141618))
                .clickable { pick() }
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Elegir fotos", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                "Se guardan solo en este teléfono",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }

    val index = ((System.currentTimeMillis() / 86_400_000L + skip) % photos.size).toInt()
    val photo = photos[index]
    val bitmap by produceState<ImageBitmap?>(null, photo.file) {
        value = withContext(Dispatchers.IO) { decodePhoto(viewModel.photoPath(photo.file)) }
    }
    val dateText = photo.takenAt?.let {
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
            .replace(".", "")
            .uppercase(Locale.getDefault())
    }

    Box(
        modifier
            .clip(CardShape)
            .background(Color(0xFF202225))
            .clickable { skip += 1 }
            .semantics { contentDescription = "Fotografía" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(dateText.orEmpty(), color = Color.White, style = MaterialTheme.typography.labelMedium)
            Text(
                "Cambiar",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { pick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

private fun decodePhoto(file: File): ImageBitmap? =
    runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull()

/** Tiempo real de hoy, calculado con el mismo motor que el panel de Bienestar. */
@Composable
private fun ScreenTimeWidget(
    state: LauncherState,
    viewModel: LauncherViewModel,
    onOpenWellbeing: () -> Unit,
    modifier: Modifier,
) {
    val hasAccess by viewModel.hasUsageAccess.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val labels = remember(state.allApps) { state.allApps.associate { it.packageName to it.label } }
    val overview = usage

    Column(
        modifier
            .clip(CardShape)
            .background(LightCard)
            .clickable { if (hasAccess) onOpenWellbeing() else viewModel.openUsageAccess() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "Tiempo de pantalla" },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tiempo de pantalla",
                color = DarkInk,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Actualizar",
                tint = DarkInk,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { viewModel.refreshUsage(force = true) },
            )
        }
        when {
            !hasAccess -> Text(
                "Toca para permitir el acceso a datos de uso",
                color = DarkInk,
                style = MaterialTheme.typography.bodySmall,
            )
            overview == null -> Text("Calculando…", color = DarkInk, style = MaterialTheme.typography.bodySmall)
            else -> {
                Text(
                    formatDuration(overview.total.todayMs),
                    color = DarkInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                // Los servicios del sistema cuentan en el total, pero no son apps que se puedan abrir.
                overview.apps.filter { it.todayMs > 0 && it.packageName in labels }.take(3).forEachIndexed { index, app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            shortDuration(app.todayMs),
                            color = if (index == 0) Color.White else DarkInk,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (index) {
                                        0 -> Color(0xFF1B1C1E)
                                        1 -> Color(0xFFB9BDC6)
                                        else -> Color(0xFFE2E5F2)
                                    },
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            labels[app.packageName] ?: app.packageName.orEmpty(),
                            color = DarkInk,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun shortDuration(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 1 -> "<1m"
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0L -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

/** Accesos a la búsqueda de Google. El logotipo es el icono real de la app, si está instalada. */
@Composable
private fun SearchWidget(state: LauncherState, viewModel: LauncherViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val googleApp = remember(state.allApps) { state.allApps.firstOrNull { it.packageName == GOOGLE_APP } }
    fun search() {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", "")
        if (googleApp != null) intent.setPackage(GOOGLE_APP)
        context.startSafely(intent, "No hay ninguna app de búsqueda disponible")
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xE6DCDDDF))
                .clickable { search() }
                .padding(horizontal = 18.dp, vertical = 9.dp)
                .semantics { contentDescription = "Búsqueda de Google" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (googleApp != null) {
                AppIcon(googleApp, viewModel, size = 30.dp)
            } else {
                Text("G", color = DarkInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Icon(
                painterResource(R.drawable.ic_search_mic),
                contentDescription = "Búsqueda por voz",
                tint = DarkInk,
                modifier = Modifier
                    .size(26.dp)
                    .clickable {
                        context.startSafely(
                            Intent(RecognizerIntent.ACTION_WEB_SEARCH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH),
                            "La búsqueda por voz no está disponible",
                        )
                    },
            )
            Spacer(Modifier.width(22.dp))
            Icon(
                painterResource(R.drawable.ic_search_lens),
                contentDescription = "Buscar con la cámara",
                tint = DarkInk,
                modifier = Modifier
                    .size(26.dp)
                    .clickable {
                        context.startSafely(
                            Intent(Intent.ACTION_VIEW, "googleapp://lens".toUri()).setPackage(GOOGLE_APP),
                            "Google Lens no está disponible",
                        )
                    },
            )
        }
    }
}
