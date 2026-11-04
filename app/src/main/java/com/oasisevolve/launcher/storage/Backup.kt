package com.oasisevolve.launcher.storage

import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_FORMAT = "oasis-evolve-backup"
private const val BACKUP_VERSION = 1

data class Backup(val settings: LauncherSettings, val notes: List<Note>)

/**
 * Copia de seguridad en un archivo JSON legible: ajustes, pantallas, carpetas y notas. Las fotos del
 * widget y los widgets de otras apps no van incluidos: dependen de este teléfono.
 */
fun Backup.toJson(exportedAt: Long): String = JSONObject()
    .put("format", BACKUP_FORMAT)
    .put("version", BACKUP_VERSION)
    .put("exportedAt", exportedAt)
    .put("settings", JSONObject(settings.toJson()))
    .put(
        "notes",
        JSONArray(notes.map { JSONObject().put("text", it.text).put("updatedAt", it.updatedAt) }),
    )
    .toString(2)

/** `null` si el archivo no es una copia de Oasis Evolve. */
fun parseBackup(json: String): Backup? {
    val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
    if (o.optString("format") != BACKUP_FORMAT) return null
    val settings = o.optJSONObject("settings") ?: return null
    val notes = o.optJSONArray("notes")?.let { array ->
        List(array.length()) { index ->
            val note = array.getJSONObject(index)
            Note(text = note.getString("text"), updatedAt = note.getLong("updatedAt"))
        }
    }.orEmpty()
    return Backup(LauncherSettings.fromJson(settings.toString()), notes)
}
