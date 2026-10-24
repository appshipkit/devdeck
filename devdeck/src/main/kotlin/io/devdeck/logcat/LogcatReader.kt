package io.devdeck.logcat

import android.os.Build
import android.os.Process
import io.devdeck.console.ConsoleBuffer
import io.devdeck.console.ConsoleLevel
import io.devdeck.console.ConsoleLine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

public object LogcatReader {

    public val buffer: ConsoleBuffer = ConsoleBuffer(capacity = 5_000)

    @Volatile
    public var ignoredTags: Set<String> = LogcatNoise.PLATFORM_TAGS

    @Volatile
    public var ignoredTagPrefixes: Set<String> = LogcatNoise.PLATFORM_TAG_PREFIXES

    @Volatile
    public var ignoredMessagePrefixes: Set<String> = LogcatNoise.PLATFORM_MESSAGE_PREFIXES

    private val lock = Any()

    @Volatile
    private var generation = 0
    private var process: java.lang.Process? = null
    private var starting = false

    public val isRunning: Boolean get() = synchronized(lock) { process != null || starting }

    public fun start() {
        val runId = synchronized(lock) {
            if (process != null || starting) return
            starting = true
            ++generation
        }
        thread(isDaemon = true, name = "DevDeck-logcat") { read(runId) }
    }

    public fun stop() {
        synchronized(lock) {
            generation++
            starting = false
            process?.destroy()
            process = null
        }
    }

    private fun read(runId: Int) {
        val pid = Process.myPid()
        val command = buildList {
            add("logcat")
            add("-v")
            add("threadtime")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) add("--pid=$pid")
        }
        val started = runCatching { ProcessBuilder(command).redirectErrorStream(true).start() }.getOrNull()
        val current = synchronized(lock) {
            starting = false
            if (runId == generation && started != null) {
                process = started
                true
            } else {
                false
            }
        }
        if (!current || started == null) {
            started?.destroy()
            return
        }
        runCatching {
            started.inputStream.bufferedReader().useLines { lines ->
                for (raw in lines) {
                    if (runId != generation) break
                    val line = LogcatParser.parse(raw, pid) ?: continue
                    if (!isIgnored(line)) buffer.append(line)
                }
            }
        }
        synchronized(lock) {
            if (runId == generation) process = null
        }
    }

    private fun isIgnored(line: ConsoleLine): Boolean = line.tag in ignoredTags ||
        ignoredTagPrefixes.any { line.tag.startsWith(it) } ||
        ignoredMessagePrefixes.any { line.message.startsWith(it) }
}

internal object LogcatParser {

    private val THREADTIME = Regex("""^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFA])\s+(.*?)\s*: ?(.*)$""")
    private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }

    fun parse(raw: String, pid: Int, nowMillis: Long = System.currentTimeMillis()): ConsoleLine? {
        val match = THREADTIME.matchEntire(raw) ?: return null
        val (time, linePid, tid, level, tag, message) = match.destructured
        if (linePid.toIntOrNull() != pid) return null
        val year = yearFor(time.substring(0, 2).toIntOrNull() ?: 0, nowMillis)
        return ConsoleLine(
            level = ConsoleLevel.fromLetter(level.first()),
            tag = tag.ifBlank { "(no tag)" },
            message = message,
            timestampMillis = runCatching { timeFormat.get()?.parse("$year-$time")?.time }.getOrNull() ?: nowMillis,
            threadId = tid.toIntOrNull(),
        )
    }

    // threadtime omits the year; a line from December read in January belongs to the previous year.
    fun yearFor(month: Int, nowMillis: Long): Int {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val year = now.get(Calendar.YEAR)
        return if (month - (now.get(Calendar.MONTH) + 1) > MONTHS_AHEAD_LIMIT) year - 1 else year
    }

    private const val MONTHS_AHEAD_LIMIT = 6
}
