package com.oasisevolve.launcher.drawer

import java.text.Normalizer

private val DIACRITICS = "\\p{M}+".toRegex()
private val SEPARATORS = "[^\\p{L}\\p{N}]+".toRegex()

fun normalizeForSearch(text: String): String =
    DIACRITICS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase()

/** Letra del riel alfabético para una etiqueta; `#` si no empieza por una letra. */
fun railLetter(label: String): Char {
    val first = normalizeForSearch(label).firstOrNull()?.uppercaseChar() ?: return '#'
    return if (first in 'A'..'Z') first else '#'
}

/**
 * Puntuación de una etiqueta para una búsqueda; `null` si no coincide. Menor es mejor:
 * inicio de nombre, inicio de palabra, iniciales y, por último, cualquier subcadena.
 */
fun searchScore(label: String, query: String): Int? {
    val needle = normalizeForSearch(query).trim()
    if (needle.isEmpty()) return 0
    val name = normalizeForSearch(label)
    val words = name.split(SEPARATORS).filter { it.isNotEmpty() }
    return when {
        name.startsWith(needle) -> 0
        words.any { it.startsWith(needle) } -> 1
        words.joinToString("") { it.take(1) }.startsWith(needle) -> 2
        needle in name -> 3
        else -> null
    }
}

fun <T> filterBySearch(items: List<T>, query: String, label: (T) -> String): List<T> =
    if (query.isBlank()) items
    else items.mapNotNull { item -> searchScore(label(item), query)?.let { item to it } }
        .sortedWith(compareBy({ it.second }, { normalizeForSearch(label(it.first)) }))
        .map { it.first }
