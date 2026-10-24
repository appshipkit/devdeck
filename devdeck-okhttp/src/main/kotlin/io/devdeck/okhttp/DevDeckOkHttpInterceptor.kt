package io.devdeck.okhttp

import io.devdeck.network.NetworkLogger
import io.devdeck.simulation.NetworkEffect
import io.devdeck.simulation.NetworkFailure
import io.devdeck.simulation.NetworkSimulator
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.Throttler
import okio.buffer

public class DevDeckOkHttpInterceptor(
    private val maxBodyBytes: Long = 250_000L,
    private val simulateNetwork: Boolean = true,
    private val enabled: () -> Boolean = { true },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!enabled()) return chain.proceed(request)

        val effect = if (simulateNetwork) NetworkSimulator.effectFor(request.url.toString()) else null
        val requestBody = request.body
        val id = NetworkLogger.start(
            method = request.method,
            url = request.url.toString(),
            headers = request.headers.toPairs() + requestBody.contentHeaders(),
            body = requestBody?.readText(),
            contentType = requestBody?.contentType()?.toString(),
            size = requestBody?.contentLength()?.takeIf { it >= 0 },
            simulation = effect?.label,
        )

        val startNanos = System.nanoTime()
        val response = try {
            when (effect) {
                is NetworkEffect.Fail -> simulateFailure(chain, effect.failure)

                is NetworkEffect.Slow -> {
                    pause(chain, effect.delayMillis)
                    chain.proceed(request)
                }

                null -> chain.proceed(request)
            }
        } catch (error: Exception) {
            NetworkLogger.fail(id, error.toString(), elapsedMillis(startNanos))
            throw error
        }
        val duration = elapsedMillis(startNanos)

        val responseBody = response.body
        val contentType = responseBody?.contentType()
        val peeked = responseBody != null && contentType != null && !contentType.isStreaming() && contentType.isText() &&
            isSafeToPeek(responseBody.contentLength(), contentType)
        val bodyText = when {
            responseBody == null -> null
            peeked -> runCatching { response.peekBody(maxBodyBytes).string() }.getOrNull()
            contentType.isStreaming() -> "(streamed body not captured: $contentType)"
            contentType == null -> "(untyped body not captured)"
            !contentType.isText() -> "(binary body, $contentType)"
            else -> "(open-ended body not captured: $contentType)"
        }
        NetworkLogger.complete(
            id = id,
            code = response.code,
            message = response.message,
            headers = response.headers.toPairs(),
            body = bodyText,
            contentType = contentType?.toString(),
            size = responseBody?.contentLength()?.takeIf { it >= 0 } ?: bodyText?.takeIf { peeked }?.length?.toLong(),
            durationMillis = duration,
            protocol = response.protocol.toString(),
        )
        return if (effect is NetworkEffect.Slow && effect.bytesPerSecond > 0) response.throttled(effect.bytesPerSecond) else response
    }

    private fun simulateFailure(chain: Interceptor.Chain, failure: NetworkFailure): Response {
        val request = chain.request()
        when (failure) {
            NetworkFailure.NoConnection ->
                throw UnknownHostException("Unable to resolve host \"${request.url.host}\": No address associated with hostname $SIMULATED")

            NetworkFailure.Timeout -> {
                pause(chain, chain.readTimeoutMillis().takeIf { it > 0 }?.toLong() ?: DEFAULT_TIMEOUT_MILLIS)
                throw SocketTimeoutException("timeout $SIMULATED")
            }

            NetworkFailure.ServerError -> return serverError(request)
        }
    }

    private fun serverError(request: Request): Response {
        val now = System.currentTimeMillis()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(HTTP_UNAVAILABLE)
            .message("Service Unavailable")
            .header("Retry-After", "5")
            .body("""{"error":"Service Unavailable","detail":"$SIMULATED"}""".toResponseBody("application/json".toMediaType()))
            .sentRequestAtMillis(now)
            .receivedResponseAtMillis(now)
            .build()
    }

    private fun pause(chain: Interceptor.Chain, millis: Long) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis)
        while (true) {
            if (chain.call().isCanceled()) throw IOException("Canceled")
            val remainingNanos = deadline - System.nanoTime()
            if (remainingNanos <= 0) return
            val step = minOf(remainingNanos, TimeUnit.MILLISECONDS.toNanos(PAUSE_STEP_MILLIS))
            try {
                Thread.sleep(step / NANOS_PER_MILLI, (step % NANOS_PER_MILLI).toInt())
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException("Interrupted").apply { initCause(error) }
            }
        }
    }

    private fun Response.throttled(bytesPerSecond: Long): Response {
        val original = body ?: return this
        val chunk = minOf(bytesPerSecond, THROTTLE_CHUNK_BYTES)
        val throttler = Throttler().apply { bytesPerSecond(bytesPerSecond, waitByteCount = chunk, maxByteCount = chunk) }
        val throttledBody = throttler.source(original.source()).buffer().asResponseBody(original.contentType(), original.contentLength())
        return newBuilder().body(throttledBody).build()
    }

    private fun RequestBody.readText(): String? {
        if (isDuplex() || isOneShot()) return "(streamed body not captured)"
        val length = contentLength()
        if (contentType() == null) {
            if (length !in 0..maxBodyBytes) return "(untyped body not captured)"
        } else if (!contentType().isText()) {
            return "(binary body, ${contentType()})"
        }
        if (length > maxBodyBytes * LARGE_BODY_FACTOR) return "(body too large to capture: $length bytes)"
        return runCatching {
            val buffer = Buffer()
            writeTo(buffer)
            val charset = contentType()?.charset() ?: Charsets.UTF_8
            buffer.readString(minOf(buffer.size, maxBodyBytes), charset)
        }.getOrNull()
    }

    private fun RequestBody?.contentHeaders(): List<Pair<String, String>> {
        if (this == null) return emptyList()
        return listOfNotNull(
            contentType()?.let { "Content-Type" to it.toString() },
            contentLength().takeIf { it >= 0 }?.let { "Content-Length" to it.toString() },
        )
    }

    private fun Headers.toPairs(): List<Pair<String, String>> = (0 until size).map { name(it) to value(it) }

    private fun MediaType?.isStreaming(): Boolean {
        if (this == null) return false
        val subtype = subtype.lowercase()
        return STREAMING_SUBTYPES.any { subtype.contains(it) }
    }

    private fun MediaType?.isText(): Boolean {
        if (this == null) return false
        val subtype = subtype.lowercase()
        return type == "text" || TEXT_SUBTYPES.any { subtype.contains(it) }
    }

    // peekBody blocks until maxBodyBytes or EOF, so only peek bodies that end: a known length, or a document
    // type (JSON, XML, HTML, GraphQL) that is not used for open-ended streams. Chunked HTTP/1.1, HTTP/2 and
    // close-delimited bodies all report an unknown length.
    private fun isSafeToPeek(contentLength: Long, contentType: MediaType): Boolean {
        if (contentLength >= 0) return true
        val subtype = contentType.subtype.lowercase()
        return DOCUMENT_SUBTYPES.any { subtype.contains(it) }
    }

    private fun MediaType.charset(): Charset? = charset(null)

    private fun elapsedMillis(startNanos: Long) = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)

    private companion object {
        const val SIMULATED = "(simulated by DevDeck)"
        const val HTTP_UNAVAILABLE = 503
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L
        const val PAUSE_STEP_MILLIS = 50L
        const val NANOS_PER_MILLI = 1_000_000L
        const val LARGE_BODY_FACTOR = 4
        const val THROTTLE_CHUNK_BYTES = 8_192L
        val STREAMING_SUBTYPES = listOf("event-stream", "x-mixed-replace", "x-ndjson")
        val DOCUMENT_SUBTYPES = listOf("json", "xml", "html", "graphql")
        val TEXT_SUBTYPES = listOf("json", "xml", "html", "plain", "x-www-form-urlencoded", "graphql", "javascript")
    }
}
