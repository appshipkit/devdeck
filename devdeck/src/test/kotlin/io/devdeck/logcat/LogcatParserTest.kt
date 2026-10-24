package io.devdeck.logcat

import io.devdeck.console.ConsoleLevel
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
}
