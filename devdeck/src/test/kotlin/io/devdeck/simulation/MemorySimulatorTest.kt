package io.devdeck.simulation

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class MemorySimulatorTest {

    @After
    fun tearDown() {
        MemorySimulator.release()
    }

    @Test
    fun `allocate holds memory until released`() {
        val allocated = MemorySimulator.allocate(8)

        assertEquals(8L * MB, allocated)
        assertEquals(8L * MB, MemorySimulator.heldBytes)

        MemorySimulator.release()
        assertEquals(0L, MemorySimulator.heldBytes)
    }

    private companion object {
        const val MB = 1024L * 1024L
    }
}
