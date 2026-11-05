package io.devdeck.crash

import io.devdeck.logs.LogEntry
import io.devdeck.network.NetworkCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Plain-text pieces of a crash report. Lists arrive newest first and are written oldest first, ending at the crash. */
internal object CrashReportFormat {

    fun networkCalls(calls: List<NetworkCall>): String? {
        if (calls.isEmpty()) return null
        val time = timeFormat()
        return calls.asReversed().joinToString("\n") { call ->
            buildString {
                append(time.format(Date(call.startedAtMillis))).append("  ")
                append(call.method.padEnd(METHOD_WIDTH)).append(' ')
                append((call.responseCode?.toString() ?: if (call.error != null) "ERR" else "…").padEnd(STATUS_WIDTH))
                append((call.durationMillis?.let { "$it ms" } ?: "").padStart(DURATION_WIDTH)).append("  ")
                append(call.url)
                call.error?.let { append("\n    Error: ").append(it.oneLine(MAX_ERROR_CHARS)) }
                // Only failed calls carry a body excerpt; bodies were already redacted by NetworkLogger.
                if (call.isFailure) {
                    call.responseBody?.takeIf {
                        it.isNotBlank()
                    }?.let { append("\n    Response: ").append(it.oneLine(MAX_BODY_CHARS)) }
                }
            }
        }
    }

    fun logs(entries: List<LogEntry>): String? {
        if (entries.isEmpty()) return null
        val time = timeFormat()
        return entries.asReversed().joinToString("\n") { entry ->
            buildString {
                append(
                    time.format(Date(entry.timestampMillis)),
                ).append("  [").append(entry.tag).append("] ").append(entry.message.oneLine(MAX_MESSAGE_CHARS))
                entry.payload?.takeIf { it.isNotBlank() }?.let { append("\n    ").append(it.oneLine(MAX_PAYLOAD_CHARS)) }
            }
        }
    }

    fun duration(millis: Long): String {
        val seconds = millis / 1000
        return when {
            seconds < 60 -> "$seconds s"
            seconds < 3600 -> "${seconds / 60} min ${seconds % 60} s"
            else -> "${seconds / 3600} h ${seconds % 3600 / 60} min"
        }
    }

    /** The signal that ended a native crash, such as "signal 11 (SIGSEGV)"; null when there's none. */
    fun signal(status: Int): String? = if (status <= 0) null else "signal $status" + (SIGNALS[status]?.let { " ($it)" } ?: "")

    /** ActivityManager importance as words; the numbers are stable platform constants. */
    fun importance(importance: Int): String = when {
        importance <= 100 -> "in the foreground"
        importance <= 200 -> "visible"
        importance <= 300 -> "running a service"
        else -> "in the background"
    }

    fun megabytes(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

    private fun String.oneLine(max: Int): String {
        val flat = replace(Regex("\\s+"), " ").trim()
        return if (flat.length <= max) flat else flat.take(max) + "…"
    }

    private fun timeFormat() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val SIGNALS = mapOf(4 to "SIGILL", 5 to "SIGTRAP", 6 to "SIGABRT", 7 to "SIGBUS", 8 to "SIGFPE", 11 to "SIGSEGV")

    private const val METHOD_WIDTH = 6
    private const val STATUS_WIDTH = 4
    private const val DURATION_WIDTH = 8
    private const val MAX_ERROR_CHARS = 300
    private const val MAX_BODY_CHARS = 500
    private const val MAX_MESSAGE_CHARS = 500
    private const val MAX_PAYLOAD_CHARS = 300
}
