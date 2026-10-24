package io.devdeck.network

import androidx.compose.runtime.Immutable
import java.net.URI
import java.net.URLDecoder

@Immutable
public data class NetworkCall(
    val id: Long,
    val startedAtMillis: Long,
    val method: String,
    val url: String,
    val requestHeaders: List<Pair<String, String>> = emptyList(),
    val requestBody: String? = null,
    val requestContentType: String? = null,
    val requestSize: Long? = null,
    val operationName: String? = null,
    val responseCode: Int? = null,
    val responseMessage: String? = null,
    val responseHeaders: List<Pair<String, String>> = emptyList(),
    val responseBody: String? = null,
    val responseContentType: String? = null,
    val responseSize: Long? = null,
    val durationMillis: Long? = null,
    val protocol: String? = null,
    val error: String? = null,
    val simulation: String? = null,
) {
    val isComplete: Boolean get() = responseCode != null || error != null

    val isFailure: Boolean get() = error != null || (responseCode != null && responseCode >= 400)

    val host: String get() = runCatching { URI(url).host }.getOrNull().orEmpty()

    val path: String get() = runCatching { URI(url).rawPath }.getOrNull()?.ifEmpty { "/" } ?: url

    val queryParameters: List<Pair<String, String>>
        get() = runCatching { URI(url).rawQuery }.getOrNull()
            ?.split('&')
            ?.filter { it.isNotEmpty() }
            ?.map { part ->
                val name = part.substringBefore('=')
                val value = part.substringAfter('=', "")
                decode(name) to decode(value)
            }
            .orEmpty()

    val displayName: String get() = operationName ?: path

    private fun decode(value: String) = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
