package io.devdeck.okhttp

import io.devdeck.network.NetworkLogger
import io.devdeck.simulation.NetworkCondition
import io.devdeck.simulation.NetworkFailure
import io.devdeck.simulation.NetworkSimulation
import io.devdeck.simulation.NetworkSimulator
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class NetworkSimulationInterceptorTest {

    private val server = MockWebServer()

    private val client = OkHttpClient.Builder()
        .addInterceptor(DevDeckOkHttpInterceptor())
        .readTimeout(300, TimeUnit.MILLISECONDS)
        .build()

    @Before
    fun setUp() {
        server.start()
        NetworkLogger.clear()
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkSimulator.reset()
    }

    @Test
    fun `normal network passes requests through`() {
        server.enqueue(MockResponse().setBody("ok"))

        val body = call("/orders").use { it.body!!.string() }

        assertEquals("ok", body)
        assertEquals(1, server.requestCount)
        assertNull(NetworkLogger.calls.value.single().simulation)
    }

    @Test
    fun `slow network delays the response but still succeeds`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Slow, delayMillis = 400, bytesPerSecond = 0)
        server.enqueue(MockResponse().setBody("ok"))

        val started = System.nanoTime()
        val response = call("/orders")
        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)

        assertEquals(200, response.code)
        assertEquals("ok", response.use { it.body!!.string() })
        assertTrue("took $elapsed ms", elapsed >= 400)
        assertEquals("Slow", NetworkLogger.calls.value.single().simulation)
    }

    @Test
    fun `slow network throttles the download speed`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Slow, delayMillis = 0, bytesPerSecond = 16 * 1024)
        server.enqueue(MockResponse().setBody("x".repeat(32 * 1024)))

        val started = System.nanoTime()
        val length = call("/catalog").use { it.body!!.string().length }
        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)

        assertEquals(32 * 1024, length)
        assertTrue("took $elapsed ms", elapsed >= 1_000)
    }

    @Test
    fun `no connection fails without reaching the server`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing, failure = NetworkFailure.NoConnection)

        assertThrows<UnknownHostException> { call("/orders") }

        assertEquals(0, server.requestCount)
        val logged = NetworkLogger.calls.value.single()
        assertEquals("No connection", logged.simulation)
        assertTrue(logged.error!!.contains("UnknownHostException"))
    }

    @Test
    fun `timeout waits for the read timeout and then fails`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing, failure = NetworkFailure.Timeout)

        val started = System.nanoTime()
        assertThrows<SocketTimeoutException> { call("/orders") }
        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)

        assertTrue("took $elapsed ms", elapsed >= 300)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `server error returns a 503 without reaching the server`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing, failure = NetworkFailure.ServerError)

        val response = call("/orders")

        assertEquals(503, response.code)
        assertTrue(response.use { it.body!!.string() }.contains("simulated by DevDeck"))
        assertEquals(0, server.requestCount)
        assertEquals(503, NetworkLogger.calls.value.single().responseCode)
    }

    @Test
    fun `requests work again after switching back to normal`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing)
        assertThrows<UnknownHostException> { call("/orders") }

        NetworkSimulator.reset()
        server.enqueue(MockResponse().setBody("ok"))

        assertEquals("ok", call("/orders").use { it.body!!.string() })
    }

    @Test
    fun `url filter only affects matching requests`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing, urlFilter = "/checkout")
        server.enqueue(MockResponse().setBody("ok"))

        assertEquals(200, call("/orders").use { it.code })
        assertThrows<UnknownHostException> { call("/checkout/pay") }
    }

    @Test
    fun `simulation can be turned off per interceptor`() {
        NetworkSimulator.simulation = NetworkSimulation(condition = NetworkCondition.Failing)
        val plainClient = OkHttpClient.Builder().addInterceptor(DevDeckOkHttpInterceptor(simulateNetwork = false)).build()
        server.enqueue(MockResponse().setBody("ok"))

        val code = plainClient.newCall(Request.Builder().url(server.url("/orders")).build()).execute().use { it.code }

        assertEquals(200, code)
    }

    private fun call(path: String) = client.newCall(Request.Builder().url(server.url(path)).build()).execute()

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return
            throw error
        }
        fail("Expected ${T::class.java.simpleName}")
    }
}
