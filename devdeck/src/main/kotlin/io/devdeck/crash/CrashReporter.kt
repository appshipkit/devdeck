package io.devdeck.crash

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import io.devdeck.breadcrumbs.Breadcrumbs
import io.devdeck.device.appVersionInfo
import io.devdeck.internal.DeckSettings
import io.devdeck.logs.DeckLogger
import io.devdeck.network.NetworkLogger
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * One saved crash. [sections] holds what was captured around it, in order: app and device details, the steps,
 * network calls and logs before the crash, and any sections the app added with [CrashReporter.addSection].
 */
@Immutable
public data class CrashRecord(
    val file: File,
    val timestampMillis: Long,
    val thread: String,
    val title: String,
    val stackTrace: String,
    val steps: String? = null,
    val sections: List<Pair<String, String>> = emptyList(),
) {

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        file: File,
        timestampMillis: Long,
        thread: String,
        title: String,
        stackTrace: String,
        steps: String? = null,
    ) : this(file, timestampMillis, thread, title, stackTrace, steps, emptyList())

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        file: File = this.file,
        timestampMillis: Long = this.timestampMillis,
        thread: String = this.thread,
        title: String = this.title,
        stackTrace: String = this.stackTrace,
        steps: String? = this.steps,
    ): CrashRecord = copy(
        file = file,
        timestampMillis = timestampMillis,
        thread = thread,
        title = title,
        stackTrace = stackTrace,
        steps = steps,
        sections = sections,
    )

    /** The full report as text, the same as the saved file. */
    val report: String get() = runCatching { file.readText() }.getOrDefault(stackTrace)
}

/**
 * Saves uncaught exceptions to files before the app dies, with what led up to them, and shows them after the
 * relaunch. On Android 11+ it also picks up ANRs and native crashes from the system on the next launch.
 */
public object CrashReporter {

    public var maxCrashes: Int = 20

    /** How many of the latest network calls a report includes. */
    public var networkCallsInReport: Int = 20

    /** How many of the latest DeckLogger entries a report includes. */
    public var logsInReport: Int = 50

    /** How many of the latest breadcrumbs a report includes. */
    public var stepsInReport: Int = 50

    internal var version by mutableIntStateOf(0)
        private set

    /** The newest crash the tester hasn't looked at yet, set on launch; null once seen or dismissed. */
    public var unseenCrash: CrashRecord? by mutableStateOf(null)
        private set

    private var directory: File? = null
    private var appContext: Context? = null
    private var deviceSummary: String = ""
    private val customSections = CopyOnWriteArrayList<Pair<String, () -> String?>>()

    /**
     * Adds a section to every crash report, such as the signed-in user or the current experiment. [content] runs on
     * the crashing thread just before the app dies, so it must be quick and must not block; return null to skip it.
     * Adding a section with the same [title] again replaces it.
     */
    public fun addSection(title: String, content: () -> String?) {
        customSections.removeAll { it.first == title }
        customSections += title to content
    }

    public fun removeSection(title: String) {
        customSections.removeAll { it.first == title }
    }

    public fun install(context: Context) {
        if (directory != null) return
        val app = context.applicationContext
        val dir = File(app.filesDir, "devdeck/crashes").apply { mkdirs() }
        directory = dir
        appContext = app
        deviceSummary = deviceSummary(app)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(dir, thread, error) }
            previous?.uncaughtException(thread, error)
        }
        thread(name = "DevDeck crash check", isDaemon = true) {
            runCatching { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) importExitReasons(app, dir) }
            runCatching { findUnseen(app) }
        }
    }

    public fun crashes(): List<CrashRecord> = directory?.listFiles { file -> file.extension == "txt" }
        .orEmpty()
        .sortedByDescending { it.lastModified() }
        .mapNotNull(::read)

    public fun clear() {
        directory?.listFiles()?.forEach { it.delete() }
        markSeen()
        version++
    }

    /** Stops offering [unseenCrash] after a relaunch; the crash stays in the list. */
    public fun markSeen() {
        val crash = unseenCrash
        unseenCrash = null
        val context = appContext ?: return
        if (crash != null) DeckSettings.of(context).edit { putLong(KEY_LAST_SEEN, crash.timestampMillis) }
    }

    private fun findUnseen(context: Context) {
        val lastSeen = DeckSettings.of(context).getLong(KEY_LAST_SEEN, 0L)
        unseenCrash = crashes().firstOrNull { it.timestampMillis > lastSeen }
    }

    private fun write(dir: File, thread: Thread, error: Throwable) {
        val now = System.currentTimeMillis()
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        save(dir, now, thread.name, trace, captureSections(now))
    }

    private fun save(dir: File, timestampMillis: Long, thread: String, trace: String, sections: List<Pair<String, String>>) {
        val text = buildString {
            append(timestampMillis).append('\n').append(thread).append('\n').append(trace.trimEnd())
            sections.forEach { (title, content) -> append("\n").append(marker(title)).append('\n').append(content.trimEnd()) }
            append('\n')
        }
        File(dir, "crash_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestampMillis))}.txt").writeText(text)
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(maxCrashes)?.forEach { it.delete() }
    }

    // Each part is guarded on its own, so a failing one never stops the crash from being saved.
    private fun captureSections(now: Long): List<Pair<String, String>> {
        val builtIn = listOf<Pair<String, () -> String?>>(
            SECTION_APP to { appSection(now) },
            SECTION_STEPS to { Breadcrumbs.recent(stepsInReport).takeIf { it.isNotEmpty() }?.let(Breadcrumbs::format) },
            SECTION_NETWORK to { CrashReportFormat.networkCalls(NetworkLogger.calls.value.take(networkCallsInReport)) },
            SECTION_LOGS to { CrashReportFormat.logs(DeckLogger.entries.value.take(logsInReport)) },
        )
        return (builtIn + customSections).mapNotNull { (title, content) ->
            runCatching { content() }.getOrNull()?.takeIf { it.isNotBlank() }?.let { title to it }
        }
    }

    private fun appSection(now: Long): String {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val uptime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CrashReportFormat.duration(SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime())
        } else {
            null
        }
        return buildString {
            append(deviceSummary)
            append(
                "\nHeap: ",
            ).append(CrashReportFormat.megabytes(used)).append(" used of ").append(CrashReportFormat.megabytes(runtime.maxMemory()))
            if (uptime != null) append("\nRunning for: ").append(uptime)
            append("\nTime: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date(now)))
        }
    }

    private fun deviceSummary(context: Context): String {
        val version = appVersionInfo(context)
        val memory = runCatching {
            val info = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
            CrashReportFormat.megabytes(info.totalMem)
        }.getOrNull()
        return buildString {
            append("App: ").append(context.packageName)
            if (version != null) append(' ').append(version.versionName).append(" (").append(version.versionCode).append(')')
            append("\nDevice: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            append("\nAndroid: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(')')
            if (memory != null) append("\nDevice memory: ").append(memory)
        }
    }

    /** Saves ANRs and native crashes the system recorded since the last check; Java crashes are already saved. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun importExitReasons(context: Context, dir: File) {
        val prefs = DeckSettings.of(context)
        val lastImported = prefs.getLong(KEY_LAST_EXIT, 0L)
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val exits = manager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS)
            .filter { it.timestamp > lastImported && it.reason in IMPORTED_REASONS }
        exits.forEach { exit ->
            val anr = exit.reason == ApplicationExitInfo.REASON_ANR
            val title = if (anr) {
                "ANR: " + (exit.description ?: "the app stopped responding")
            } else {
                "Native crash: " + (CrashReportFormat.signal(exit.status) ?: exit.description ?: "no details from the system")
            }
            val details = buildString {
                append(title)
                if (anr) {
                    append("\n\n").append(exit.readTrace() ?: "The system kept no ANR trace for this one.")
                } else {
                    append("\n\nThe native stack trace is in the device's tombstone; pull it with `adb bugreport`.")
                }
            }
            val app = buildString {
                append(deviceSummary)
                append("\nState: ").append(CrashReportFormat.importance(exit.importance))
                if (exit.pss > 0) append("\nMemory at exit (PSS): ").append(CrashReportFormat.megabytes(exit.pss * 1024))
                append("\nRecorded by the system; steps, network calls and logs are not available for this one.")
            }
            save(dir, exit.timestamp, "main", details, listOf(SECTION_APP to app))
        }
        exits.maxOfOrNull { it.timestamp }?.let { newest -> prefs.edit { putLong(KEY_LAST_EXIT, newest) } }
        if (exits.isNotEmpty()) version++
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun ApplicationExitInfo.readTrace(): String? = runCatching {
        traceInputStream?.bufferedReader()?.use { reader ->
            val buffer = CharArray(MAX_TRACE_CHARS)
            val read = reader.read(buffer)
            if (read <= 0) null else String(buffer, 0, read) + if (reader.read() != -1) "\n… (trace cut short)" else ""
        }
    }.getOrNull()

    private fun read(file: File): CrashRecord? = runCatching { parse(file, file.readText()) }.getOrNull()

    /** Old files only have the steps section; they read the same way. */
    internal fun parse(file: File, text: String): CrashRecord {
        val titles = SECTION_SPLIT.findAll(text).map { it.groupValues[1] }.toList()
        val parts = text.split(SECTION_SPLIT)
        val sections = titles.zip(parts.drop(1)) { title, content -> title to content.trimEnd() }
        val lines = parts[0].lines()
        return CrashRecord(
            file = file,
            timestampMillis = lines[0].toLong(),
            thread = lines[1],
            title = lines.getOrNull(2).orEmpty(),
            stackTrace = lines.drop(2).joinToString("\n").trimEnd(),
            steps = sections.firstOrNull { it.first == SECTION_STEPS }?.second,
            sections = sections,
        )
    }

    private fun marker(title: String) = "--- $title ---"

    internal const val SECTION_APP = "App and device"
    internal const val SECTION_STEPS = "Steps before the crash"
    internal const val SECTION_NETWORK = "Network calls before the crash"
    internal const val SECTION_LOGS = "Logs before the crash"

    private val SECTION_SPLIT = Regex("\n--- (.+?) ---\n")
    private val IMPORTED_REASONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        setOf(ApplicationExitInfo.REASON_ANR, ApplicationExitInfo.REASON_CRASH_NATIVE)
    } else {
        emptySet()
    }
    private const val MAX_EXITS = 10
    private const val MAX_TRACE_CHARS = 200_000
    private const val KEY_LAST_SEEN = "crash_last_seen"
    private const val KEY_LAST_EXIT = "crash_last_exit_imported"
}
