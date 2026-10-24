package io.devdeck.network

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkLoggerBudgetTest {

    @Before
    fun setUp() {
        NetworkLogger.clear()
        NetworkLogger.maxTotalBodyChars = 1_000
    }

    @After
    fun tearDown() {
        NetworkLogger.clear()
        NetworkLogger.maxTotalBodyChars = 12_000_000
    }

    @Test
    fun `drops the oldest bodies first once over budget and keeps metadata`() {
        val first = NetworkLogger.start("POST", "https://x.io/1", body = "a".repeat(300))
        NetworkLogger.complete(first, 200, body = "b".repeat(300))
        val second = NetworkLogger.start("POST", "https://x.io/2", body = "c".repeat(300))
        NetworkLogger.complete(second, 201, body = "d".repeat(300))

        val (newest, oldest) = NetworkLogger.calls.value
        assertEquals("c".repeat(300), newest.requestBody)
        assertEquals("d".repeat(300), newest.responseBody)
        assertNotEquals("a".repeat(300), oldest.requestBody)
        assertNotEquals("b".repeat(300), oldest.responseBody)
        assertEquals(200, oldest.responseCode)
        assertEquals("https://x.io/1", oldest.url)
        assertTrue(NetworkLogger.calls.value.sumOf { (it.requestBody?.length ?: 0) + (it.responseBody?.length ?: 0) } <= 1_000)
    }

    @Test
    fun `keeps every body while under budget`() {
        repeat(3) { NetworkLogger.start("GET", "https://x.io/$it", body = "x".repeat(100)) }

        assertTrue(NetworkLogger.calls.value.all { it.requestBody == "x".repeat(100) })
    }
}
