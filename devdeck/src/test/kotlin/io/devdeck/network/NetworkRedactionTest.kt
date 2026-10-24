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
    fun `matches camelCase and dashed variants of secret keys`() {
        val body = NetworkRedaction.body(
            """{"accessToken":"a1","refreshToken":"r1","idToken":"i1","clientSecret":"c1","apiKey":"k1","user":"sam"}""",
            NetworkLogger.redactedFields,
        )

        listOf("a1", "r1", "i1", "c1", "k1").forEach { assertFalse(body, body.contains("\"$it\"")) }
        assertTrue(body.contains(""""accessToken":"██""""))
        assertTrue(body.contains(""""user":"sam""""))
        assertEquals("a=1&accessToken=██", NetworkRedaction.body("a=1&accessToken=xyz", keys))
        assertEquals("https://x.io/?Api-Key=██&q=1", NetworkRedaction.url("https://x.io/?Api-Key=k&q=1", keys))
    }

    @Test
    fun `masks non-string JSON values`() {
        val body = NetworkRedaction.body("""{"password": 123456, "api_key": true, "access_token": -1.5e3, "page": 2}""", keys)

        assertEquals("""{"password": "██", "api_key": "██", "access_token": "██", "page": 2}""", body)
    }

    @Test
    fun `does not treat string values as keys`() {
        val body = NetworkRedaction.body("""{"note":"password","items":["password",":"],"x":{"password":"p"}}""", keys)

        assertEquals("""{"note":"password","items":["password",":"],"x":{"password":"██"}}""", body)
    }

    @Test
    fun `handles very long string values`() {
        val long = "a".repeat(200_000)
        val body = NetworkRedaction.body("""{"blob":"$long","password":"p"}""", keys)

        assertTrue(body.endsWith(""""password":"██"}"""))
    }

    @Test
    fun `redacts the default auth headers`() {
        NetworkLogger.clear()
        val headers = listOf("X-Auth-Token", "x-access-token", "X-CSRF-Token", "X-XSRF-TOKEN", "Authorization").map { it to "secret" }
        NetworkLogger.start("GET", "https://x.io", headers = headers + ("Accept" to "json"))

        val logged = NetworkLogger.calls.value.single().requestHeaders
        assertTrue(logged.filter { it.first != "Accept" }.none { it.second == "secret" })
        assertEquals("json", logged.single { it.first == "Accept" }.second)
        NetworkLogger.clear()
    }

    @Test
    fun `leaves urls without secrets untouched`() {
        val url = "https://api.example.com/orders?page=1"
        assertEquals(url, NetworkRedaction.url(url, keys))
    }
}
