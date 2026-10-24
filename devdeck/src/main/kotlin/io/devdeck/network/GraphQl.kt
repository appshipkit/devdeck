package io.devdeck.network

import java.net.URI
import java.net.URLDecoder
import org.json.JSONObject

internal object GraphQl {

    private val OPERATION_HEADERS = listOf("X-APOLLO-OPERATION-NAME", "apollographql-operation-name", "X-GraphQL-Operation-Name")
    private val QUERY_KEYWORD = Regex("""^\s*(query|mutation|subscription)\s+(\w+)""")

    fun operationName(url: String, headers: List<Pair<String, String>>, body: String?): String? {
        headers.firstOrNull { (name, _) -> OPERATION_HEADERS.any { it.equals(name, ignoreCase = true) } }
            ?.second?.takeIf { it.isNotBlank() }?.let { return it }

        if (body != null && body.trimStart().startsWith("{")) {
            val json = runCatching { JSONObject(body) }.getOrNull()
            if (json != null) {
                json.optString("operationName").takeIf { it.isNotBlank() && it != "null" }?.let { return it }
                json.optString("query").let { QUERY_KEYWORD.find(it)?.groupValues?.get(2) }?.let { return it }
            }
        }

        val query = runCatching { URI(url).rawQuery }.getOrNull() ?: return null
        return query.split('&')
            .firstOrNull { it.startsWith("operationName=") }
            ?.substringAfter('=')
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
            ?.takeIf { it.isNotBlank() }
    }
}
