package io.devdeck.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleBufferTest {

    private fun line(message: String) = ConsoleLine(level = ConsoleLevel.Info, tag = "Test", message = message)

    @Test
    fun `flushes appended lines`() {
        val buffer = ConsoleBuffer()
        buffer.append(line("one"))
        buffer.publish(buffer.takeBatch())

        assertEquals(listOf("one"), buffer.lines.value.map { it.message })
    }

    @Test
    fun `a batch taken before clear is dropped`() {
        val buffer = ConsoleBuffer()
        buffer.append(line("old"))
        val inFlight = buffer.takeBatch()

        buffer.clear()
        buffer.publish(inFlight)

        assertTrue(buffer.lines.value.isEmpty())
    }

    @Test
    fun `lines appended after clear still show up`() {
        val buffer = ConsoleBuffer()
        buffer.append(line("old"))
        val inFlight = buffer.takeBatch()
        buffer.clear()
        buffer.append(line("new"))

        buffer.publish(inFlight)
        buffer.publish(buffer.takeBatch())

        assertEquals(listOf("new"), buffer.lines.value.map { it.message })
    }
}
