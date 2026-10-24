package io.devdeck.logcat

import io.devdeck.console.ConsoleLevel
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LogcatParserTest {

    @Test
    fun `parses a threadtime line`() {
        val line = LogcatParser.parse("10-24 04:45:22.358 31419 31445 E Checkout: Payment declined", pid = 31419)

        assertNotNull(line)
        assertEquals(ConsoleLevel.Error, line!!.level)
        assertEquals("Checkout", line.tag)
        assertEquals("Payment declined", line.message)
        assertEquals(31445, line.threadId)
    }

    @Test
    fun `keeps colons inside the message`() {
        val line = LogcatParser.parse("10-24 04:45:22.358 31419 31419 I SampleApi: GET /orders → 200: ok", pid = 31419)

        assertEquals("SampleApi", line?.tag)
        assertEquals("GET /orders → 200: ok", line?.message)
    }

    @Test
    fun `ignores other processes`() {
        assertNull(LogcatParser.parse("10-24 04:45:22.358 1234 1234 D Other: hello", pid = 31419))
    }

    @Test
    fun `ignores buffer headers`() {
        assertNull(LogcatParser.parse("--------- beginning of main", pid = 31419))
    }

    @Test
    fun `maps fatal to assert`() {
        assertEquals(ConsoleLevel.Assert, LogcatParser.parse("10-24 04:45:22.358 1 1 F libc: abort", pid = 1)?.level)
    }

    @Test
    fun `uses the previous year for December lines read in January`() {
        val newYear = millis(2027, Calendar.JANUARY, 1)
        val line = LogcatParser.parse("12-31 23:59:59.900 1 1 I Clock: tick", pid = 1, nowMillis = newYear)

        assertEquals(millis(2026, Calendar.DECEMBER, 31, 23, 59, 59, 900), line?.timestampMillis)
    }

    @Test
    fun `uses the current year for recent months`() {
        assertEquals(2027, LogcatParser.yearFor(month = 1, nowMillis = millis(2027, Calendar.JANUARY, 1)))
        assertEquals(2027, LogcatParser.yearFor(month = 6, nowMillis = millis(2027, Calendar.MARCH, 1)))
        assertEquals(2026, LogcatParser.yearFor(month = 11, nowMillis = millis(2027, Calendar.FEBRUARY, 1)))
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0, ms: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, second)
            set(Calendar.MILLISECOND, ms)
        }.timeInMillis
}
