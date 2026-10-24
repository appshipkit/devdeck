package io.devdeck.network

import androidx.compose.ui.graphics.Color
import io.devdeck.internal.DeckPalette
import io.devdeck.internal.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object NetworkFormat {

    fun size(bytes: Long?): String = when {
        bytes == null || bytes < 0 -> "—"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    fun duration(millis: Long?): String = when {
        millis == null -> "…"
        millis < 1000 -> "$millis ms"
        else -> String.format(Locale.US, "%.2f s", millis / 1000.0)
    }

    fun time(millis: Long): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(millis))

    fun dateTime(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(millis))

    fun status(call: NetworkCall): String = when {
        call.error != null -> "ERR"
        call.responseCode != null -> call.responseCode.toString()
        else -> "…"
    }

    fun isJson(contentType: String?, body: String?): Boolean = contentType?.contains("json", ignoreCase = true) == true || Json.isJson(body)

    fun curl(call: NetworkCall): String = buildString {
        append("curl -X ").append(call.method).append(" '").append(call.url.shellEscape()).append("'")
        call.requestHeaders.forEach { (name, value) -> append(" \\\n  -H '").append("$name: $value".shellEscape()).append("'") }
        call.requestBody?.let { append(" \\\n  --data-raw '").append(it.shellEscape()).append("'") }
    }

    fun summary(call: NetworkCall): String = buildString {
        appendLine("${call.method} ${call.url}")
        call.operationName?.let { appendLine("Operation: $it") }
        appendLine("Status: ${status(call)} ${call.responseMessage.orEmpty()}".trimEnd())
        appendLine("Duration: ${duration(call.durationMillis)}")
        call.error?.let { appendLine("Error: $it") }
        if (call.requestHeaders.isNotEmpty()) {
            appendLine()
            appendLine("Request headers")
            call.requestHeaders.forEach { (name, value) -> appendLine("$name: $value") }
        }
        call.requestBody?.let {
            appendLine()
            appendLine("Request body")
            appendLine(Json.prettyOrNull(it) ?: it)
        }
        if (call.responseHeaders.isNotEmpty()) {
            appendLine()
            appendLine("Response headers")
            call.responseHeaders.forEach { (name, value) -> appendLine("$name: $value") }
        }
        call.responseBody?.let {
            appendLine()
            appendLine("Response body")
            appendLine(Json.prettyOrNull(it) ?: it)
        }
    }

    fun line(call: NetworkCall): String =
        "${time(call.startedAtMillis)} ${call.method} ${status(call)} ${call.url} · ${duration(call.durationMillis)}"

    private fun String.shellEscape(): String = replace("'", "'\\''")
}

internal fun methodColor(method: String, isDark: Boolean): Color = when (method) {
    "GET" -> DeckPalette.Blue
    "POST" -> DeckPalette.Green
    "PUT" -> DeckPalette.Orange
    "PATCH" -> DeckPalette.Purple
    "DELETE" -> DeckPalette.Red
    else -> DeckPalette.Grey
}.pick(isDark)

/** Shared status colors for the network screens, picked for the current theme. */
internal object NetworkColors {
    fun error(isDark: Boolean): Color = DeckPalette.Red.pick(isDark)

    fun success(isDark: Boolean): Color = DeckPalette.Green.pick(isDark)

    fun pending(isDark: Boolean): Color = DeckPalette.Grey.pick(isDark)

    fun callCount(isDark: Boolean): Color = DeckPalette.DeepPurple.pick(isDark)

    fun simulated(isDark: Boolean): Color = DeckPalette.DeepOrange.pick(isDark)
}

internal fun statusColor(call: NetworkCall, isDark: Boolean): Color = when {
    call.error != null -> NetworkColors.error(isDark)
    call.responseCode == null -> NetworkColors.pending(isDark)
    call.responseCode >= 500 -> NetworkColors.error(isDark)
    call.responseCode >= 400 -> DeckPalette.Orange.pick(isDark)
    call.responseCode >= 300 -> DeckPalette.Blue.pick(isDark)
    else -> NetworkColors.success(isDark)
}
