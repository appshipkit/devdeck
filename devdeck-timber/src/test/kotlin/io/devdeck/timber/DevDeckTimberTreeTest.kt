package io.devdeck.timber

import android.content.ContextWrapper
import io.devdeck.console.ConsoleLevel
import io.devdeck.console.ConsoleLine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class DevDeckTimberTreeTest {

    @Before
    fun setUp() {
        Timber.uprootAll()
        TimberLogs.buffer.clear()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        TimberLogs.buffer.clear()
    }

    @Test
    fun `logs reach the buffer with their tag and level`() {
        Timber.plant(DevDeckTimberTree())

        Timber.tag("Checkout").w("Card declined")
        Timber.tag("Checkout").e("Payment failed")

        val lines = awaitLines(2)
        assertEquals(listOf("Card declined", "Payment failed"), lines.map { it.message })
        assertEquals(listOf("Checkout", "Checkout"), lines.map { it.tag })
        assertEquals(listOf(ConsoleLevel.Warn, ConsoleLevel.Error), lines.map { it.level })
    }

    @Test
    fun `untagged logs get a tag from the calling class`() {
        Timber.plant(DevDeckTimberTree())

        Timber.i("Started")

        val line = awaitLines(1).single()
        assertEquals("DevDeckTimberTreeTest", line.tag)
        assertEquals(ConsoleLevel.Info, line.level)
    }

    @Test
    fun `plugin plants its tree once`() {
        val plugin = TimberLogsPlugin()

        plugin.onAttach(FakeContext)
        plugin.onAttach(FakeContext)

        assertEquals(1, Timber.forest().count { it is DevDeckTimberTree })
    }

    @Test
    fun `plugin leaves Timber alone when planting is turned off`() {
        TimberLogsPlugin(plantTree = false).onAttach(FakeContext)

        assertTrue(Timber.forest().isEmpty())
    }

    @Test
    fun `plugin does not plant a second tree next to the app's own`() {
        Timber.plant(DevDeckTimberTree(defaultTag = "App"))

        TimberLogsPlugin().onAttach(FakeContext)

        assertEquals(1, Timber.forest().size)
    }

    private fun awaitLines(count: Int): List<ConsoleLine> {
        val deadline = System.currentTimeMillis() + 5_000
        while (TimberLogs.buffer.lines.value.size < count && System.currentTimeMillis() < deadline) Thread.sleep(20)
        return TimberLogs.buffer.lines.value.also { assertEquals(count, it.size) }
    }

    private object FakeContext : ContextWrapper(null)
}
