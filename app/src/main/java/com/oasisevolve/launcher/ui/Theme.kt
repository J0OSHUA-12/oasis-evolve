package com.oasisevolve.launcher.ui

import android.app.Activity
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.storage.ThemeMode
import com.oasisevolve.launcher.system.AppEntry

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EE7B7),
    onPrimary = Color(0xFF06281F),
    surface = Color(0xFF12181A),
    onSurface = Color(0xFFF4F7F6),
    onSurfaceVariant = Color(0xFFB9C4C1),
    background = Color.Transparent,
    onBackground = Color(0xFFF4F7F6),
    outline = Color(0x33FFFFFF),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    surface = Color(0xFFF6F8F7),
    onSurface = Color(0xFF10201B),
    onSurfaceVariant = Color(0xFF47564F),
    background = Color.Transparent,
    onBackground = Color(0xFF10201B),
    outline = Color(0x33000000),
)

@Composable
fun EvolveTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val wallpaperContent = rememberWallpaperContentColor()

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                val darkIcons = wallpaperContent != Color.White
                isAppearanceLightStatusBars = darkIcons
                isAppearanceLightNavigationBars = darkIcons
            }
        }
    }

    CompositionLocalProvider(LocalWallpaperContent provides wallpaperContent) {
        MaterialTheme(colorScheme = colors) {
            // Sin esto, el texto fuera de un Surface es negro también en el tema oscuro.
            CompositionLocalProvider(LocalContentColor provides colors.onSurface, content = content)
        }
    }
}

/**
 * Color del contenido dibujado directamente sobre el fondo. Depende del fondo y no del tema:
 * un tema claro sobre un fondo oscuro haría ilegible el texto.
 */
val LocalWallpaperContent = staticCompositionLocalOf { Color.White }

@Composable
private fun rememberWallpaperContentColor(): Color {
    val context = LocalContext.current
    var color by remember { mutableStateOf(wallpaperContentColor(context)) }
    DisposableEffect(context) {
        val manager = WallpaperManager.getInstance(context)
        val listener = WallpaperManager.OnColorsChangedListener { _, _ ->
            color = wallpaperContentColor(context)
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }
    return color
}

private fun wallpaperContentColor(context: Context): Color {
    if (Build.VERSION.SDK_INT < 31) return Color.White
    val colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
    val supportsDarkText = colors != null && colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
    return if (supportsDarkText) Color(0xFF10201B) else Color.White
}

/** Estilo para texto sobre el fondo real. La sombra mantiene el contraste en fondos mixtos. */
@Composable
fun TextStyle.onWallpaper(alpha: Float = 1f): TextStyle {
    val content = LocalWallpaperContent.current
    val shadow = if (content == Color.White) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.5f)
    return copy(
        color = content.copy(alpha = alpha),
        shadow = Shadow(color = shadow, offset = Offset(0f, 1.5f), blurRadius = 6f),
    )
}

/** Tarjeta translúcida que deja ver el fondo real del dispositivo. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        // Explícito: con un color translúcido, Surface no puede deducir el contraste del contenido.
        contentColor = MaterialTheme.colorScheme.onSurface,
        border =BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun AppIcon(app: AppEntry, viewModel: LauncherViewModel, size: Dp, modifier: Modifier = Modifier) {
    val icon by produceState<ImageBitmap?>(null, app.key) { value = viewModel.icon(app) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f)),
    ) {
        icon?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.size(size))
        } ?: Box(
            Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        )
    }
}

/** Icono con nombre opcional. Si los iconos están desactivados, muestra solo el nombre. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: AppEntry,
    viewModel: LauncherViewModel,
    showIcon: Boolean,
    showLabel: Boolean,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClickLabel = "Abrir ${app.label}",
                onClick = { viewModel.open(app) },
                onLongClick = { onLongPress(app) },
            )
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showIcon) AppIcon(app, viewModel, iconSize)
        if (showLabel || !showIcon) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.onWallpaper(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
