package io.devdeck.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkCallTest {

    private fun call(url: String, method: String = "GET", operation: String? = null, id: Long = 1) = NetworkCall(
        id = id,
        startedAtMillis = id,
        method = method,
        url = url,
        operationName = operation,
    )

    @Test
    fun `parses host path and decoded query parameters`() {
        val call = call("https://api.example.com/users/42/profile?include=orders%2Caddresses&page=1")

        assertEquals("api.example.com", call.host)
        assertEquals("/users/42/profile", call.path)
        assertEquals(listOf("include" to "orders,addresses", "page" to "1"), call.queryParameters)
    }

    @Test
    fun `groups REST calls by method host and path, ignoring query`() {
        val calls = listOf(
            call("https://api.example.com/orders?page=1", id = 1),
            call("https://api.example.com/orders?page=2", id = 2),
            call("https://api.example.com/orders", method = "POST", id = 3),
        )

        val groups = calls.groupByEndpoint()

        assertEquals(2, groups.size)
        assertEquals(2, groups.first { it.method == "GET" }.count)
    }

    @Test
    fun `groups GraphQL calls by operation name`() {
        val calls = listOf(
            call("https://api.example.com/graphql", method = "POST", operation = "Countries", id = 1),
            call("https://api.example.com/graphql", method = "POST", operation = "Countries", id = 2),
            call("https://api.example.com/graphql", method = "POST", operation = "Checkout", id = 3),
        )

        val groups = calls.groupByEndpoint().associateBy { it.title }

        assertEquals(2, groups.getValue("Countries").count)
        assertEquals(1, groups.getValue("Checkout").count)
    }

    @Test
    fun `detects GraphQL operation from header, body, query and url`() {
        val url = "https://api.example.com/graphql"
        assertEquals("FromHeader", GraphQl.operationName(url, listOf("X-APOLLO-OPERATION-NAME" to "FromHeader"), null))
        assertEquals("FromBody", GraphQl.operationName(url, emptyList(), """{"operationName":"FromBody","query":"query Other { a }"}"""))
        assertEquals("FromQuery", GraphQl.operationName(url, emptyList(), """{"query":"query FromQuery { a }"}"""))
        assertEquals("FromUrl", GraphQl.operationName("$url?operationName=FromUrl&variables=%7B%7D", emptyList(), null))
        assertNull(GraphQl.operationName("https://api.example.com/users", emptyList(), """{"name":"x"}"""))
    }
}
