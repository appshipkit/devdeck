package io.devdeck.network

import java.net.URLDecoder

internal object NetworkRedaction {

    fun url(url: String, keys: Set<String>): String {
        if (keys.isEmpty() || '?' !in url) return url
        val base = url.substringBefore('?')
        val rest = url.substringAfter('?')
        val query = rest.substringBefore('#')
        val fragment = rest.substringAfter('#', "").let { if (it.isEmpty()) "" else "#$it" }
        val redacted = query.split('&').joinToString("&") { part ->
            val name = part.substringBefore('=')
            if ('=' in part && keys.any { it.equals(decode(name), ignoreCase = true) }) "$name=$MASK" else part
        }
        return "$base?$redacted$fragment"
    }

    fun body(body: String, keys: Set<String>): String {
        if (keys.isEmpty() || body.isEmpty()) return body
        var result = body
        keys.forEach { key ->
            val name = Regex.escape(key)
            result =
                Regex("(\"$name\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"", RegexOption.IGNORE_CASE).replace(result) {
                    "${it.groupValues[1]}\"$MASK\""
                }
            result = Regex("((?:^|&)$name=)[^&\\s]*", RegexOption.IGNORE_CASE).replace(result) { "${it.groupValues[1]}$MASK" }
        }
        return result
    }

    private fun decode(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    const val MASK = "██"
}
