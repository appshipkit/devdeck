package io.devdeck.network

import java.net.URLDecoder
import java.util.regex.Matcher
import java.util.regex.Pattern

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
        // Most bodies contain no secret field at all; one cheap pass skips the regex work for them.
        if (!mayContainAny(body, normalized)) return body
        val json = JSON_STRING.replaceEach(body) { match ->
            val separator = match.group(2)
            if (separator != null && normalize(match.group(1)) in normalized) "\"${match.group(1)}\"$separator\"$MASK\"" else null
        }
        return FORM_FIELD.replaceEach(json) { match ->
            val name = match.group(2)
            if (normalize(decode(name)) in normalized) "${match.group(1)}$name=$MASK" else null
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

    // URL-encoded form names (`pass%5Fword`) only match after decoding, so those bodies always get the full pass.
    private fun mayContainAny(body: String, normalizedKeys: Set<String>): Boolean {
        if ('%' in body) return true
        val flat = normalize(body)
        return normalizedKeys.any { it in flat }
    }

    /**
     * Replaces matches with [transform]'s result, or keeps them when it returns null. Uses one Matcher for the whole
     * input: Kotlin's Regex.replace makes a new Matcher per match, and on Android each one copies the whole input
     * into native memory, which ran large bodies out of memory.
     */
    private inline fun Pattern.replaceEach(input: String, transform: (Matcher) -> String?): String {
        val matcher = matcher(input)
        if (!matcher.find()) return input
        val out = StringBuilder(input.length)
        var last = 0
        var replaced = false
        do {
            val replacement = transform(matcher) ?: continue
            out.append(input, last, matcher.start()).append(replacement)
            last = matcher.end()
            replaced = true
        } while (matcher.find())
        if (!replaced) return input
        return out.append(input, last, input.length).toString()
    }

    private fun decode(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    // Consumes every JSON string in order so keys and values stay aligned; groups 2 and 3 match only for `"key": scalar`.
    private const val STRING_BODY = "[^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+"

    private val JSON_STRING =
        Pattern.compile("\"($STRING_BODY)\"(?:(\\s*:\\s*)(\"$STRING_BODY\"|-?\\d[\\d.eE+-]*|true|false|null))?")

    private val FORM_FIELD = Pattern.compile("(^|&)([^=&\\s]+)=[^&\\s]*")

    const val MASK = "██"
}
