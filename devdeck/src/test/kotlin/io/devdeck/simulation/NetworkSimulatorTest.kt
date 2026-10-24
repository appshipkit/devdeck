package io.devdeck.simulation

import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkSimulatorTest {

    @After
    fun tearDown() {
        NetworkSimulator.reset()
        NetworkSimulator.random = Random.Default
    }

    @Test
    fun `normal network has no effect`() {
        assertNull(NetworkSimulator.effectFor("https://api.example.com/orders"))
    }

    @Test
    fun `slow network delays every request`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Slow, delayMillis = 2_000, bytesPerSecond = 1_024)

        assertEquals(NetworkEffect.Slow(2_000, 1_024), NetworkSimulator.effectFor("https://api.example.com/orders"))
    }

    @Test
    fun `failing network uses the chosen failure`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing, failure = NetworkFailure.Timeout)

        assertEquals(NetworkEffect.Fail(NetworkFailure.Timeout), NetworkSimulator.effectFor("https://api.example.com/orders"))
    }

    @Test
    fun `failure rate fails only that share of requests`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing, failureRatePercent = 30)

        NetworkSimulator.random = fixedRandom(29)
        assertEquals(NetworkEffect.Fail(NetworkFailure.NoConnection), NetworkSimulator.effectFor("https://api.example.com"))

        NetworkSimulator.random = fixedRandom(30)
        assertNull(NetworkSimulator.effectFor("https://api.example.com"))
    }

    @Test
    fun `url filter limits the simulation to matching requests`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing, urlFilter = "/Checkout")

        assertEquals(NetworkEffect.Fail(NetworkFailure.NoConnection), NetworkSimulator.effectFor("https://api.example.com/checkout/pay"))
        assertNull(NetworkSimulator.effectFor("https://api.example.com/orders"))
    }

    @Test
    fun `invalid values are clamped`() {
        NetworkSimulator.simulation =
            NetworkSimulation(delayMillis = -5, bytesPerSecond = -1, failureRatePercent = 500, urlFilter = "  /a ")

        val simulation = NetworkSimulator.simulation
        assertEquals(0L, simulation.delayMillis)
        assertEquals(0L, simulation.bytesPerSecond)
        assertEquals(100, simulation.failureRatePercent)
        assertEquals("/a", simulation.urlFilter)
    }

    private fun fixedRandom(value: Int) = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0

        override fun nextInt(until: Int): Int = value
    }
}
