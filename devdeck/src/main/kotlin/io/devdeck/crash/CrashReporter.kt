package io.devdeck.crash

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.devdeck.breadcrumbs.Breadcrumbs
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
public data class CrashRecord(
    val file: File,
    val timestampMillis: Long,
    val thread: String,
    val title: String,
    val stackTrace: String,
    val steps: String? = null,
)

public object CrashReporter {

    public var maxCrashes: Int = 20

    internal var version by mutableIntStateOf(0)
        private set

    private var directory: File? = null

    public fun install(context: Context) {
        if (directory != null) return
        val dir = File(context.filesDir, "devdeck/crashes").apply { mkdirs() }
        directory = dir
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(dir, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    public fun crashes(): List<CrashRecord> = directory?.listFiles { file -> file.extension == "txt" }
        .orEmpty()
        .sortedByDescending { it.lastModified() }
        .mapNotNull(::read)

    public fun clear() {
        directory?.listFiles()?.forEach { it.delete() }
        version++
    }

    private fun write(dir: File, thread: Thread, error: Throwable) {
        val now = System.currentTimeMillis()
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        File(dir, "crash_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(now))}.txt")
            .writeText("$now\n${thread.name}\n$trace" + stepsSection())
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(maxCrashes)?.forEach { it.delete() }
    }

    private fun stepsSection(): String {
        val steps = Breadcrumbs.recent(STEPS_IN_CRASH)
        return if (steps.isEmpty()) "" else "\n$STEPS_MARKER\n${Breadcrumbs.format(steps)}"
    }

    private fun read(file: File): CrashRecord? = runCatching {
        val (body, steps) = file.readText().split("\n$STEPS_MARKER\n", limit = 2).let { it[0] to it.getOrNull(1) }
        val lines = body.lines()
        CrashRecord(
            file = file,
            timestampMillis = lines[0].toLong(),
            thread = lines[1],
            title = lines.getOrNull(2).orEmpty(),
            stackTrace = lines.drop(2).joinToString("\n").trimEnd(),
            steps = steps?.trimEnd(),
        )
    }.getOrNull()

    private const val STEPS_IN_CRASH = 50
    private const val STEPS_MARKER = "--- Steps before the crash ---"
}
