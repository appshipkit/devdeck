package io.devdeck

import android.app.Activity

public object DeckExclusions {

    public const val ADYEN: String = "com.adyen."
    public const val PRIMER: String = "io.primer."
    public const val NETCETERA: String = "com.netcetera.threeds."
    public const val CARDINAL: String = "com.cardinalcommerce."
    public const val STRIPE: String = "com.stripe.android."
    public const val BRAINTREE: String = "com.braintreepayments."
    public const val CHECKOUT_COM: String = "com.checkout."
    public const val PAYPAL: String = "com.paypal."

    public val PAYMENT_SDKS: Set<String> = setOf(ADYEN, PRIMER, NETCETERA, CARDINAL, STRIPE, BRAINTREE, CHECKOUT_COM, PAYPAL)

    internal fun isExcluded(activity: Activity, prefixes: Set<String>): Boolean = isExcluded(activity.javaClass.name, prefixes)

    internal fun isExcluded(className: String, prefixes: Set<String>): Boolean = prefixes.any { className.startsWith(it) }
}
