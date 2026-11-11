package io.devdeck.flags

import io.devdeck.plugin.matches
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagSearchTest {

    private val flag = FeatureFlag.Toggle("checkout_v2_enabled", default = false, title = "New checkout")

    @Test
    fun `matches the title or the key, ignoring case and separators`() {
        assertTrue(flag.matches("new check"))
        assertTrue(flag.matches("CHECKOUT"))
        assertTrue(flag.matches("checkout_v2"))
        assertTrue(flag.matches("checkout v2"))
        assertTrue(flag.matches("CheckoutV2Enabled"))
    }

    @Test
    fun `doesn't match unrelated or empty queries`() {
        assertFalse(flag.matches("payment"))
        assertFalse(flag.matches("  "))
        assertFalse(flag.matches("_"))
    }
}
