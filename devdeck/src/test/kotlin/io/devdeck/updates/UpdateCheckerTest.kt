package io.devdeck.updates

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @After
    fun tearDown() {
        UpdateChecker.reset()
    }

    @Test
    fun `available release is shown and notified`() = runBlocking {
        val update = AvailableUpdate("1.4.0", 412, "Fixes")
        val notified = mutableListOf<AvailableUpdate>()
        UpdateChecker.use(FakeSource(result = { update }))
        UpdateChecker.notifier = { notified += it }

        UpdateChecker.refresh()

        assertEquals(UpdateStatus.Available(update), UpdateChecker.status)
        assertEquals(update, UpdateChecker.availableUpdate)
        assertEquals(listOf(update), notified)
        assertEquals("v1.4.0 (412)", update.label)
    }

    @Test
    fun `no release means up to date`() = runBlocking {
        UpdateChecker.use(FakeSource(result = { null }))

        UpdateChecker.refresh()

        assertEquals(UpdateStatus.UpToDate, UpdateChecker.status)
        assertNull(UpdateChecker.availableUpdate)
        assertEquals(1_000L, UpdateChecker.lastCheckedAtMillis)
    }

    @Test
    fun `errors are reported as failed checks`() = runBlocking {
        UpdateChecker.use(FakeSource(result = { throw IOException("Tester not signed in") }))

        UpdateChecker.refresh()

        assertEquals(UpdateStatus.Failed("Tester not signed in"), UpdateChecker.status)
    }

    @Test
    fun `after installing it shows the latest version`() = runBlocking {
        val update = AvailableUpdate("1.4.0", 412)
        UpdateChecker.use(FakeSource(result = { update }))
        UpdateChecker.refresh()

        UpdateChecker.installUpdate(UpdateChecker.source!!, update)

        assertEquals(UpdateStatus.UpToDate, UpdateChecker.status)
        assertNull(UpdateChecker.availableUpdate)
    }

    @Test
    fun `checks are due when never run or older than the interval`() {
        assertTrue(isCheckDue(null, 5_000, 1_000))
        assertTrue(isCheckDue(1_000, 2_000, 1_000))
        assertFalse(isCheckDue(1_500, 2_000, 1_000))
    }

    private fun UpdateChecker.use(source: UpdateSource) {
        reset()
        clock = { 1_000L }
        attachForTest(source)
    }

    private class FakeSource(private val result: () -> AvailableUpdate?) : UpdateSource {
        override val name: String = "Fake"

        override suspend fun checkForUpdate(): AvailableUpdate? = result()

        override suspend fun install(update: AvailableUpdate, onProgress: (Float?) -> Unit) = Unit
    }
}
