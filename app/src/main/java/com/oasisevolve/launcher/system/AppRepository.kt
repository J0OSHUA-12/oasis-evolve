package com.oasisevolve.launcher.system

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.LruCache
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.oasisevolve.launcher.drawer.normalizeForSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val appCategory: Int,
    val isWorkProfile: Boolean,
)

data class AppIconImage(val bitmap: ImageBitmap, val clipToShape: Boolean)

/** Mayor tamaño de icono en ajustes: los bitmaps se reducen al dibujarse. */
private const val ICON_BITMAP_DP = 72

/** Las capas de un icono adaptativo miden 108 unidades, de las que se ven 72. */
private const val ADAPTIVE_LAYER_INSET = 0.25f

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val iconPack = IconPackLoader(context)
    private var loadedPack: String? = null
    private val icons = object : LruCache<String, AppIconImage>(32 * 1024) {
        override fun sizeOf(key: String, value: AppIconImage) =
            value.bitmap.width * value.bitmap.height * 4 / 1024
    }

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val _iconRevision = MutableStateFlow(0)
    val iconRevision: StateFlow<Int> = _iconRevision

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackagesAvailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) =
            refresh()
        override fun onPackagesUnavailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) =
            refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    private fun invalidate(packageName: String) {
        icons.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach(icons::remove)
        // Si cambia el propio paquete de iconos, hay que volver a leerlo.
        if (packageName == loadedPack) {
            scope.launch(Dispatchers.IO) { reloadIconPack(packageName) }
        }
        refresh()
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val fresh = HashMap<String, LauncherActivityInfo>()
            val mainUser = android.os.Process.myUserHandle()
            val entries = userManager.userProfiles.flatMap { user ->
                launcherApps.getActivityList(null, user)
                    .filter { it.applicationInfo.packageName != context.packageName }
                    .map { info ->
                        val serial = userManager.getSerialNumberForUser(user)
                        val key = "${info.componentName.flattenToShortString()}#$serial"
                        fresh[key] = info
                        AppEntry(
                            key = key,
                            label = info.label.toString(),
                            packageName = info.applicationInfo.packageName,
                            component = info.componentName,
                            user = user,
                            appCategory = info.applicationInfo.category,
                            isWorkProfile = user != mainUser,
                        )
                    }
            }
            infos.clear()
            infos.putAll(fresh)
            val collator = Collator.getInstance(Locale.getDefault())
            _apps.value = entries.sortedWith { a, b ->
                collator.compare(normalizeForSearch(a.label), normalizeForSearch(b.label))
            }
        }
    }

    fun installedIconPacks(): List<IconPackInfo> = iconPack.installed()

    suspend fun setIconPack(packageName: String?) {
        if (packageName == loadedPack) return
        withContext(Dispatchers.IO) { reloadIconPack(packageName) }
    }

    /** Un paquete de iconos sin actividad de launcher puede desinstalarse sin aviso del callback. */
    fun dropIconPackIfUninstalled() {
        val pack = loadedPack ?: return
        if (installedIconPacks().none { it.packageName == pack }) {
            scope.launch(Dispatchers.IO) { reloadIconPack(null) }
        }
    }

    private fun reloadIconPack(packageName: String?) {
        iconPack.load(packageName)
        loadedPack = packageName
        icons.evictAll()
        _iconRevision.value += 1
    }

    suspend fun icon(entry: AppEntry): AppIconImage? {
        icons.get(entry.key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val info = infos[entry.key] ?: return@withContext null
            runCatching { renderIcon(entry, info).also { icons.put(entry.key, it) } }.getOrNull()
        }
    }

    private fun renderIcon(entry: AppEntry, info: LauncherActivityInfo): AppIconImage {
        val size = (ICON_BITMAP_DP * context.resources.displayMetrics.density).toInt()
        val packIcon = iconPack.drawableFor(entry.component.packageName, entry.component.className)
        if (packIcon != null) {
            return AppIconImage(packIcon.toBitmap(size, size, Bitmap.Config.ARGB_8888).asImageBitmap(), false)
        }
        val bitmap = when (val drawable = info.getIcon(0)) {
            is AdaptiveIconDrawable -> renderAdaptive(drawable, size)
            else -> renderLegacy(drawable, size)
        }
        return AppIconImage(badgeForProfile(bitmap, entry).asImageBitmap(), true)
    }

    /** Dibuja el icono a sangre, sin la máscara del sistema, para aplicar la forma elegida en ajustes. */
    private fun renderAdaptive(icon: AdaptiveIconDrawable, size: Int): Bitmap {
        val inset = (size * ADAPTIVE_LAYER_INSET).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        listOfNotNull(icon.background, icon.foreground).forEach { layer ->
            layer.setBounds(-inset, -inset, size + inset, size + inset)
            layer.draw(canvas)
        }
        return bitmap
    }

    /** Un icono antiguo no es recortable: se centra sobre una placa para que la forma no lo corte. */
    private fun renderLegacy(icon: Drawable, size: Int): Bitmap {
        val inset = size / 8
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        icon.setBounds(inset, inset, size - inset, size - inset)
        icon.draw(canvas)
        return bitmap
    }

    private fun badgeForProfile(bitmap: Bitmap, entry: AppEntry): Bitmap {
        if (!entry.isWorkProfile) return bitmap
        val plain = BitmapDrawable(context.resources, bitmap)
        return context.packageManager.getUserBadgedIcon(plain, entry.user)
            .toBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    fun launch(entry: AppEntry) {
        try {
            launcherApps.startMainActivity(entry.component, entry.user, null, null)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No se pudo abrir ${entry.label}", Toast.LENGTH_SHORT).show()
            refresh()
        } catch (_: SecurityException) {
            Toast.makeText(context, "No se pudo abrir ${entry.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(entry: AppEntry) {
        launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null)
    }

    fun uninstall(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, "package:${entry.packageName}".toUri())
            .putExtra(Intent.EXTRA_USER, entry.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openUsageAccessSettings() {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
