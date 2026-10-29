package com.github.damontecres.wholphin.util

import android.content.Context
import com.github.damontecres.wholphin.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Translates the names the server gives to libraries and home rows ("Movies", "Popular Series"...) into the language of
 * the app, with the "English=Translation" pairs of the `server_name_translations` array of that language. A whole name
 * that is in the list is replaced as is, otherwise the phrases of the list are replaced inside it, longest first, so
 * "Hidden Gems - Movies" can become "Comori ascunse - Filme". Names the list knows nothing about stay as they are, and so
 * does everything when the language has no list (English).
 */
fun localizeServerName(
    context: Context,
    name: String,
): String {
    val rules = rulesFor(context)
    if (rules.isEmpty()) return name
    rules.firstOrNull { it.english.equals(name, ignoreCase = true) }?.let { return it.local }
    var result = name
    rules.forEach { rule -> result = rule.regex.replace(result, rule.replacement) }
    return result
}

private class NameRule(
    val english: String,
    val local: String,
) {
    val regex = Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(english) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
    val replacement: String = Regex.escapeReplacement(local)
}

// Parsed once per language, the names are translated again every time a page recomposes
private val rulesCache = ConcurrentHashMap<String, List<NameRule>>()

private fun rulesFor(context: Context): List<NameRule> =
    rulesCache.getOrPut(context.resources.configuration.locales.toLanguageTags()) {
        context.resources
            .getStringArray(R.array.server_name_translations)
            .mapNotNull { entry ->
                entry.split('=', limit = 2).takeIf { it.size == 2 }?.let { NameRule(it[0].trim(), it[1].trim()) }
            }.sortedByDescending { it.english.length }
    }
