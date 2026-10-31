package com.oasisevolve.launcher.system

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
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
import androidx.core.graphics.drawable.toBitmap
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

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val icons = object : LruCache<String, ImageBitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

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

    suspend fun icon(entry: AppEntry): ImageBitmap? {
        icons.get(entry.key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val info = infos[entry.key] ?: return@withContext null
            runCatching {
                val size = (56 * context.resources.displayMetrics.density).toInt()
                info.getBadgedIcon(0)
                    .toBitmap(size, size, Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
                    .also { icons.put(entry.key, it) }
            }.getOrNull()
        }
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
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${entry.packageName}"))
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
