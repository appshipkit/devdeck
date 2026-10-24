package io.devdeck.okhttp

import io.devdeck.network.NetworkLogger
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DevDeckOkHttpInterceptorBodyTest {

    private val server = MockWebServer()

    private val client = OkHttpClient.Builder()
        .addInterceptor(DevDeckOkHttpInterceptor())
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        server.start()
        NetworkLogger.clear()
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkLogger.clear()
    }

    @Test
    fun `captures text bodies with a known length`() {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"ok":true}"""))

        call().close()

        assertEquals("""{"ok":true}""", loggedBody())
    }

    @Test
    fun `does not peek untyped bodies`() {
        server.enqueue(MockResponse().setBody("raw"))

        assertEquals("raw", call().use { it.body!!.string() })
        assertEquals("(untyped body not captured)", loggedBody())
    }

    @Test
    fun `does not block on a chunked text stream`() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/plain")
                .setChunkedBody("tick\n".repeat(20), 5)
                .throttleBody(5, 1, TimeUnit.SECONDS),
        )

        val started = System.nanoTime()
        val response = call()
        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        response.close()

        assertTrue("interceptor blocked for $elapsedMillis ms", elapsedMillis < 2_000)
        assertTrue(loggedBody().orEmpty().startsWith("(chunked body not captured"))
    }

    @Test
    fun `still captures chunked JSON documents`() {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setChunkedBody("""{"items":[1,2,3]}""", 4))

        call().close()

        assertEquals("""{"items":[1,2,3]}""", loggedBody())
    }

    private fun call() = client.newCall(Request.Builder().url(server.url("/feed")).build()).execute()

    private fun loggedBody() = NetworkLogger.calls.value.single().responseBody
}
