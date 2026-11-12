package io.devdeck.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkTest {

    private val link = DeepLink("Product page", "myapp://product/123", group = "Shop", description = "Sneaker with sizes")

    @Test
    fun `search matches name, group, description and uri`() {
        assertTrue(link.matches("product page"))
        assertTrue(link.matches("shop"))
        assertTrue(link.matches("sizes"))
        assertTrue(link.matches("product/123"))
        assertFalse(link.matches("checkout"))
    }
}

class DeepLinkUrisTest {

    @Test
    fun `adds the base URL only to links without a scheme`() {
        assertEquals("myapp://product/1", DeepLinkUris.withBaseUrl("product/1", "myapp://"))
        assertEquals("myapp://product/1", DeepLinkUris.withBaseUrl("/product/1", "myapp://"))
        assertEquals("https://shop.example.com/sneakers", DeepLinkUris.withBaseUrl("sneakers", "https://shop.example.com"))
        assertEquals("other://x", DeepLinkUris.withBaseUrl("other://x", "myapp://"))
        assertEquals("https://a.com", DeepLinkUris.withBaseUrl("https://a.com", "myapp://"))
        assertEquals("product/1", DeepLinkUris.withBaseUrl("product/1", null))
    }

    @Test
    fun `fills placeholders and encodes values but keeps slashes`() {
        val uri = "myapp://product/{id}?q={query}"
        assertEquals(listOf("id", "query"), DeepLinkUris.placeholders(uri))
        assertEquals("myapp://product/42?q=air%20max", DeepLinkUris.fill(uri, mapOf("id" to "42", "query" to "air max")))
        assertEquals("myapp://browse/sneakers/nike", DeepLinkUris.fill("myapp://browse/{path}", mapOf("path" to "sneakers/nike")))
    }

    @Test
    fun `empty optional values drop their query parameter`() {
        val uri = "myapp://product/{id}?size={size}&color={color}"
        assertEquals("myapp://product/1?color=red", DeepLinkUris.fill(uri, mapOf("id" to "1", "color" to "red")))
        assertEquals("myapp://product/1?size=10", DeepLinkUris.fill(uri, mapOf("id" to "1", "size" to "10")))
        assertEquals("myapp://product/1", DeepLinkUris.fill(uri, mapOf("id" to "1")))
    }

    @Test
    fun `placeholders without an input are asked for as required`() {
        val link =
            DeepLink(
                "Product",
                "myapp://product/{id}?size={size}",
                inputs = listOf(DeepLinkInput("size", label = "Size", required = false)),
            )
        assertTrue(link.needsInput)
        assertEquals(listOf("id", "size"), link.fields.map { it.key })
        assertTrue(link.fields[0].required)
        assertEquals("Size", link.fields[1].label)
        assertFalse(DeepLink("Home", "myapp://home").needsInput)
    }
}
