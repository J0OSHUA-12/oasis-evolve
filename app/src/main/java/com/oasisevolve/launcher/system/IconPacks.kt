package com.oasisevolve.launcher.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(val packageName: String, val label: String)

private val THEME_ACTIONS = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")
private val APP_FILTER_COMPONENT = "ComponentInfo\\{([^/}]+)/([^}]+)\\}".toRegex()

/** Convierte `ComponentInfo{paquete/.Clase}` en `paquete/paquete.Clase`; `null` si no es válido. */
fun normalizeAppFilterComponent(raw: String): String? {
    val match = APP_FILTER_COMPONENT.matchEntire(raw.trim()) ?: return null
    val (pkg, cls) = match.destructured
    return "$pkg/${if (cls.startsWith(".")) pkg + cls else cls}"
}

/** Lee paquetes con el formato estándar `appfilter.xml`. Solo se leen sus recursos: nunca se ejecuta su código. */
class IconPackLoader(private val context: Context) {
    private var resources: Resources? = null
    private var packPackage: String? = null
    private var mapping: Map<String, String> = emptyMap()

    fun installed(): List<IconPackInfo> {
        val pm = context.packageManager
        return THEME_ACTIONS
            .flatMap { action -> pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA) }
            .distinctBy { it.activityInfo.packageName }
            .map { IconPackInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    fun load(packageName: String?) {
        resources = null
        packPackage = null
        mapping = emptyMap()
        if (packageName == null) return
        runCatching {
            val res = context.packageManager.getResourcesForApplication(packageName)
            mapping = readMapping(res, packageName)
            resources = res
            packPackage = packageName
        }
    }

    fun drawableFor(packageName: String, className: String): Drawable? {
        val res = resources ?: return null
        val name = mapping["$packageName/$className"] ?: return null
        return runCatching {
            @SuppressLint("DiscouragedApi")
            val id = res.getIdentifier(name, "drawable", packPackage)
            if (id == 0) null else res.getDrawable(id, null)
        }.getOrNull()
    }

    @SuppressLint("DiscouragedApi")
    private fun readMapping(res: Resources, packageName: String): Map<String, String> {
        val xmlId = res.getIdentifier("appfilter", "xml", packageName)
        val parser: XmlPullParser = if (xmlId != 0) {
            res.getXml(xmlId)
        } else {
            Xml.newPullParser().apply { setInput(res.assets.open("appfilter.xml"), null) }
        }
        val result = HashMap<String, String>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")?.let(::normalizeAppFilterComponent)
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) result.putIfAbsent(component, drawable)
            }
            parser.next()
        }
        return result
    }
}
