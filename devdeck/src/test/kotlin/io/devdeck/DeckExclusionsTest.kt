package io.devdeck

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckExclusionsTest {

    @Test
    fun `payment SDK activities are excluded by default`() {
        val defaults = DeckExclusions.PAYMENT_SDKS
        assertTrue(DeckExclusions.isExcluded("com.adyen.threeds2.internal.ui.activity.ChallengeActivity", defaults))
        assertTrue(DeckExclusions.isExcluded("io.primer.android.PrimerActivity", defaults))
        assertTrue(DeckExclusions.isExcluded("com.stripe.android.paymentsheet.PaymentSheetActivity", defaults))
    }

    @Test
    fun `app activities are not excluded`() {
        assertFalse(DeckExclusions.isExcluded("com.myapp.checkout.CheckoutActivity", DeckExclusions.PAYMENT_SDKS))
    }

    @Test
    fun `custom prefixes extend or replace the list`() {
        assertTrue(DeckExclusions.isExcluded("com.mybank.sdk.AuthActivity", DeckExclusions.PAYMENT_SDKS + "com.mybank.sdk."))
        assertFalse(DeckExclusions.isExcluded("com.adyen.checkout.DropInActivity", emptySet()))
    }
}
