package io.devdeck.network

import io.devdeck.breadcrumbs.BreadcrumbType
import io.devdeck.breadcrumbs.Breadcrumbs
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public object NetworkLogger {

    public var maxCalls: Int = 300

    public var maxBodyChars: Int = 250_000

    /** Total request + response body chars kept across all calls; the oldest calls' bodies are dropped first. */
    public var maxTotalBodyChars: Int = 12_000_000

    public var redactedHeaders: Set<String> = setOf(
        "Authorization",
        "Cookie",
        "Set-Cookie",
        "Proxy-Authorization",
        "X-Api-Key",
        "X-Auth-Token",
        "X-Access-Token",
        "X-CSRF-Token",
        "X-XSRF-Token",
    )

    public var redactedFields: Set<String> = setOf(
        "password",
        "passwd",
        "token",
        "access_token",
        "refresh_token",
        "id_token",
        "client_secret",
        "api_key",
        "apikey",
        "secret",
    )

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
            url = NetworkRedaction.url(url, redactedFields),
            requestHeaders = redact(headers),
            requestBody = body?.let { NetworkRedaction.body(it, redactedFields) }?.truncate(),
            requestContentType = contentType,
            requestSize = size,
            operationName = GraphQl.operationName(url, headers, body),
            simulation = simulation,
        )
        _calls.update { (listOf(call) + it).take(maxCalls).withinBodyBudget() }
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
                responseBody = body?.let { NetworkRedaction.body(it, redactedFields) }?.truncate(),
                responseContentType = contentType,
                responseSize = size,
                durationMillis = durationMillis,
                protocol = protocol,
            )
        }
        breadcrumb(id)
    }

    public fun fail(id: Long, error: String, durationMillis: Long? = null) {
        update(id) { it.copy(error = error, durationMillis = durationMillis) }
        breadcrumb(id)
    }

    public fun clear() {
        _calls.value = emptyList()
    }

    private fun breadcrumb(id: Long) {
        if (!Breadcrumbs.captureNetwork) return
        val call = _calls.value.firstOrNull { it.id == id } ?: return
        val outcome =
            call.error?.let { "failed: ${it.substringBefore(':').substringAfterLast('.')}" } ?: call.responseCode?.toString().orEmpty()
        val duration = call.durationMillis?.let { " · $it ms" }.orEmpty()
        Breadcrumbs.add("${call.method} ${call.displayName} → $outcome$duration", BreadcrumbType.Network)
    }

    private fun update(id: Long, transform: (NetworkCall) -> NetworkCall) {
        _calls.update { calls -> calls.map { if (it.id == id) transform(it) else it }.withinBodyBudget() }
    }

    private fun List<NetworkCall>.withinBodyBudget(): List<NetworkCall> {
        val budget = maxTotalBodyChars.toLong()
        if (sumOf { it.bodyChars() } <= budget) return this
        var total = 0L
        var evicting = false
        return map { call ->
            total += call.bodyChars()
            if (total > budget) evicting = true
            if (evicting) call.withoutBodies() else call
        }
    }

    private fun NetworkCall.bodyChars(): Long = (requestBody?.length ?: 0).toLong() + (responseBody?.length ?: 0)

    private fun NetworkCall.withoutBodies(): NetworkCall =
        if ((requestBody == null || requestBody == EVICTED_BODY) && (responseBody == null || responseBody == EVICTED_BODY)) {
            this
        } else {
            copy(requestBody = requestBody?.let { EVICTED_BODY }, responseBody = responseBody?.let { EVICTED_BODY })
        }

    private fun redact(headers: List<Pair<String, String>>): List<Pair<String, String>> = headers.map { (name, value) ->
        if (NetworkRedaction.matches(name, redactedHeaders)) name to REDACTED else name to value
    }

    private fun String.truncate(): String =
        if (length <= maxBodyChars) this else take(maxBodyChars) + "\n… truncated (${length - maxBodyChars} more chars)"

    private const val REDACTED = "██ redacted ██"
    private const val EVICTED_BODY = "(body dropped to stay within NetworkLogger.maxTotalBodyChars)"
}
