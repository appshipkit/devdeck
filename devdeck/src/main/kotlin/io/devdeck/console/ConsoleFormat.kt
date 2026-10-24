package io.devdeck.console

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object ConsoleFormat {

    fun time(millis: Long): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(millis))

    fun export(lines: List<ConsoleLine>): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return lines.joinToString("\n") { line ->
            "${formatter.format(Date(line.timestampMillis))} ${line.level.letter}/${line.tag}: ${line.message}"
        }
    }
}
