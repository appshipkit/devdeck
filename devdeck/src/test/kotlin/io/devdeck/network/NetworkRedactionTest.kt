package io.devdeck.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRedactionTest {

    private val keys = setOf("password", "access_token", "api_key")

    @Test
    fun `masks secret query parameters and keeps the rest`() {
        val url = NetworkRedaction.url("https://api.example.com/me?access_token=abc123&page=2&API_KEY=k#top", keys)

        assertEquals("https://api.example.com/me?access_token=██&page=2&API_KEY=██#top", url)
    }

    @Test
    fun `masks secret fields in JSON bodies`() {
        val body = NetworkRedaction.body("""{"email":"a@b.com","password":"hunter\"2","nested":{"access_token":"xyz"}}""", keys)

        assertTrue(body.contains(""""email":"a@b.com""""))
        assertTrue(body.contains(""""password":"██""""))
        assertTrue(body.contains(""""access_token":"██""""))
        assertFalse(body.contains("hunter"))
        assertFalse(body.contains("xyz"))
    }

    @Test
    fun `masks secret fields in form bodies`() {
        assertEquals("user=sam&password=██&remember=true", NetworkRedaction.body("user=sam&password=s3cret&remember=true", keys))
    }

    @Test
    fun `leaves urls without secrets untouched`() {
        val url = "https://api.example.com/orders?page=1"
        assertEquals(url, NetworkRedaction.url(url, keys))
    }
}
