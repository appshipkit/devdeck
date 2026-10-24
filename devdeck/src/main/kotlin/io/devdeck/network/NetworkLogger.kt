package io.devdeck.network

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public object NetworkLogger {

    public var maxCalls: Int = 300

    public var maxBodyChars: Int = 250_000

    public var redactedHeaders: Set<String> = setOf("Authorization", "Cookie", "Set-Cookie", "Proxy-Authorization")

    private val nextId = AtomicLong()
    private val _calls = MutableStateFlow<List<NetworkCall>>(emptyList())
    public val calls: StateFlow<List<NetworkCall>> = _calls.asStateFlow()

    public fun start(
        method: String,
        url: String,
        headers: List<Pair<String, String>> = emptyList(),
        body: String? = null,
        contentType: String? = null,
        size: Long? = null,
        simulation: String? = null,
    ): Long {
        val id = nextId.incrementAndGet()
        val call = NetworkCall(
            id = id,
            startedAtMillis = System.currentTimeMillis(),
            method = method.uppercase(),
            url = url,
            requestHeaders = redact(headers),
            requestBody = body?.truncate(),
            requestContentType = contentType,
            requestSize = size,
            operationName = GraphQl.operationName(url, headers, body),
            simulation = simulation,
        )
        _calls.update { (listOf(call) + it).take(maxCalls) }
        return id
    }

    public fun complete(
        id: Long,
        code: Int,
        message: String? = null,
        headers: List<Pair<String, String>> = emptyList(),
        body: String? = null,
        contentType: String? = null,
        size: Long? = null,
        durationMillis: Long? = null,
        protocol: String? = null,
    ) {
        update(id) {
            it.copy(
                responseCode = code,
                responseMessage = message,
                responseHeaders = redact(headers),
                responseBody = body?.truncate(),
                responseContentType = contentType,
                responseSize = size,
                durationMillis = durationMillis,
                protocol = protocol,
            )
        }
    }

    public fun fail(id: Long, error: String, durationMillis: Long? = null) {
        update(id) { it.copy(error = error, durationMillis = durationMillis) }
    }

    public fun clear() {
        _calls.value = emptyList()
    }

    private fun update(id: Long, transform: (NetworkCall) -> NetworkCall) {
        _calls.update { calls -> calls.map { if (it.id == id) transform(it) else it } }
    }

    private fun redact(headers: List<Pair<String, String>>): List<Pair<String, String>> = headers.map { (name, value) ->
        if (redactedHeaders.any { it.equals(name, ignoreCase = true) }) name to REDACTED else name to value
    }

    private fun String.truncate(): String =
        if (length <= maxBodyChars) this else take(maxBodyChars) + "\n… truncated (${length - maxBodyChars} more chars)"

    private const val REDACTED = "██ redacted ██"
}
