package io.devdeck.crash

import io.devdeck.logs.LogEntry
import io.devdeck.network.NetworkCall
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {

    private val file = File("crash.txt")

    @Test
    fun `reads files saved before sections were added`() {
        val text = "1700000000000\nmain\njava.lang.IllegalStateException: boom\n\tat A.b(A.kt:1)\n" +
            "--- Steps before the crash ---\n10:00:00.000  Tap  Buy\n"

        val crash = CrashReporter.parse(file, text)

        assertEquals("java.lang.IllegalStateException: boom", crash.title)
        assertEquals("java.lang.IllegalStateException: boom\n\tat A.b(A.kt:1)", crash.stackTrace)
        assertEquals("10:00:00.000  Tap  Buy", crash.steps)
        assertEquals(listOf("Steps before the crash" to "10:00:00.000  Tap  Buy"), crash.sections)
    }

    @Test
    fun `reads every section in order`() {
        val text = "1700000000000\nmain\nANR: Input dispatching timed out\n\nfull trace\n" +
            "--- App and device ---\nApp: com.example 1.0 (1)\n--- Network calls before the crash ---\nGET 200\n--- User ---\nid=42\n"

        val crash = CrashReporter.parse(file, text)

        assertEquals("ANR: Input dispatching timed out", crash.title)
        assertEquals(listOf("App and device", "Network calls before the crash", "User"), crash.sections.map { it.first })
        assertEquals("id=42", crash.sections.last().second)
        assertNull(crash.steps)
    }

    @Test
    fun `network calls are listed oldest first with errors and failed bodies`() {
        val calls = listOf(
            NetworkCall(
                2,
                2_000,
                "POST",
                "https://api.example.com/cart",
                responseCode = 500,
                durationMillis = 120,
                responseBody = "{\n  \"error\": \"down\"\n}",
            ),
            NetworkCall(
                1,
                1_000,
                "GET",
                "https://api.example.com/items",
                responseCode = 200,
                durationMillis = 80,
                responseBody = "{\"items\":[]}",
            ),
        )

        val text = CrashReportFormat.networkCalls(calls)!!.lines()

        assertTrue(text[0], text[0].contains("GET") && text[0].endsWith("https://api.example.com/items"))
        assertTrue(text[1], text[1].contains("POST") && text[1].contains("500") && text[1].contains("120 ms"))
        assertEquals("    Response: { \"error\": \"down\" }", text[2])
        assertEquals(3, text.size)
    }

    @Test
    fun `logs are listed oldest first and empty lists are skipped`() {
        val logs = listOf(LogEntry(2, 2_000, "Cart", "Checkout failed", "code=7"), LogEntry(1, 1_000, "App", "Started", null))

        val text = CrashReportFormat.logs(logs)!!.lines()

        assertTrue(text[0], text[0].endsWith("[App] Started"))
        assertTrue(text[1], text[1].endsWith("[Cart] Checkout failed"))
        assertEquals("    code=7", text[2])
        assertNull(CrashReportFormat.logs(emptyList()))
        assertNull(CrashReportFormat.networkCalls(emptyList()))
    }

    @Test
    fun `native crash signals and process state read as words`() {
        assertEquals("signal 11 (SIGSEGV)", CrashReportFormat.signal(11))
        assertEquals("signal 31", CrashReportFormat.signal(31))
        assertNull(CrashReportFormat.signal(0))
        assertEquals("in the foreground", CrashReportFormat.importance(100))
        assertEquals("in the background", CrashReportFormat.importance(400))
    }

    @Test
    fun `durations read naturally`() {
        assertEquals("42 s", CrashReportFormat.duration(42_000))
        assertEquals("3 min 5 s", CrashReportFormat.duration(185_000))
        assertEquals("2 h 1 min", CrashReportFormat.duration(7_260_000))
    }
}
