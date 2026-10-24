package io.devdeck.network

import java.net.URLDecoder

internal object NetworkRedaction {

    fun url(url: String, keys: Set<String>): String {
        if (keys.isEmpty() || '?' !in url) return url
        val normalized = normalizeAll(keys)
        val base = url.substringBefore('?')
        val rest = url.substringAfter('?')
        val query = rest.substringBefore('#')
        val fragment = rest.substringAfter('#', "").let { if (it.isEmpty()) "" else "#$it" }
        val redacted = query.split('&').joinToString("&") { part ->
            val name = part.substringBefore('=')
            if ('=' in part && normalize(decode(name)) in normalized) "$name=$MASK" else part
        }
        return "$base?$redacted$fragment"
    }

    fun body(body: String, keys: Set<String>): String {
        if (keys.isEmpty() || body.isEmpty()) return body
        val normalized = normalizeAll(keys)
        val json = JSON_STRING.replace(body) { match ->
            val separator = match.groups[2]?.value
            if (separator != null && normalize(match.groupValues[1]) in normalized) {
                "\"${match.groupValues[1]}\"$separator\"$MASK\""
            } else {
                match.value
            }
        }
        return FORM_FIELD.replace(json) { match ->
            val name = match.groupValues[2]
            if (normalize(decode(name)) in normalized) "${match.groupValues[1]}$name=$MASK" else match.value
        }
    }

    fun matches(name: String, keys: Set<String>): Boolean {
        val normalized = normalize(name)
        return keys.any { normalize(it) == normalized }
    }

    // `accessToken`, `access_token` and `Access-Token` all compare equal.
    fun normalize(name: String): String = buildString(name.length) {
        name.forEach { if (it != '_' && it != '-') append(it.lowercaseChar()) }
    }

    private fun normalizeAll(keys: Set<String>): Set<String> = keys.mapTo(HashSet()) { normalize(it) }

    private fun decode(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    // Consumes every JSON string in order so keys and values stay aligned; groups 2 and 3 match only for `"key": scalar`.
    private const val STRING_BODY = "[^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+"

    private val JSON_STRING =
        Regex("\"($STRING_BODY)\"(?:(\\s*:\\s*)(\"$STRING_BODY\"|-?\\d[\\d.eE+-]*|true|false|null))?")

    private val FORM_FIELD = Regex("(^|&)([^=&\\s]+)=[^&\\s]*")

    const val MASK = "██"
}
