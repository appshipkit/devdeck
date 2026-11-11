package io.devdeck.actions

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
