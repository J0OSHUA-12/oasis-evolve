package com.oasisevolve.launcher.library

import android.content.pm.ApplicationInfo

val DEFAULT_CATEGORIES = listOf(
    "Educación",
    "Productividad",
    "Herramientas",
    "Comunicación",
    "Creatividad",
    "Finanzas",
    "Entretenimiento",
    "Redes sociales",
    "Juegos",
    "Otras",
)

private val PACKAGE_HINTS = listOf(
    "Redes sociales" to listOf("instagram", "facebook.katana", "tiktok", "musically", "twitter", "reddit", "snapchat", "threads", "pinterest", "linkedin"),
    "Comunicación" to listOf("whatsapp", "telegram", "signal", "gmail", "google.android.gm", "outlook", "discord", "messaging", "dialer", "contacts", "teams", "zoom"),
    "Entretenimiento" to listOf("youtube", "netflix", "spotify", "twitch", "primevideo", "disney", "music", "podcast"),
    "Educación" to listOf("classroom", "duolingo", "moodle", "coursera", "khan", "udemy", "anki", "edu"),
    "Finanzas" to listOf("bank", "banco", "wallet", "paypal", "binance", "finance", "yape", "tigo"),
    "Productividad" to listOf("notion", "obsidian", "calendar", "docs", "sheets", "slides", "keep", "drive", "todo", "office", "pdf"),
    "Creatividad" to listOf("photos", "gallery", "camera", "canva", "lightroom", "capcut", "sketch", "figma"),
    "Herramientas" to listOf("calculator", "settings", "clock", "files", "filemanager", "browser", "chrome", "brave", "firefox", "maps", "translate", "termux"),
)

/** Categoría inicial: primero pistas conocidas por paquete y después la categoría que declara la app. */
fun autoCategory(packageName: String, appCategory: Int): String {
    val pkg = packageName.lowercase()
    PACKAGE_HINTS.firstOrNull { (_, hints) -> hints.any { it in pkg } }?.let { return it.first }
    return when (appCategory) {
        ApplicationInfo.CATEGORY_GAME -> "Juegos"
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> "Entretenimiento"
        ApplicationInfo.CATEGORY_IMAGE -> "Creatividad"
        ApplicationInfo.CATEGORY_SOCIAL -> "Redes sociales"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productividad"
        ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Herramientas"
        else -> "Otras"
    }
}
