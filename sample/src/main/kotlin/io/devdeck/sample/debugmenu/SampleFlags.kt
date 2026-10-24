package io.devdeck.sample.debugmenu

import io.devdeck.flags.FeatureFlag

object SampleFlags {
    private const val READY = "Ready for Testing ✅"
    private const val IN_DEV = "In Development ⚠️"

    val newCheckout = FeatureFlag.Toggle("new_checkout", default = false, title = "New checkout", section = READY, category = "checkout")
    val oneClickBuy = FeatureFlag.Toggle("one_click_buy", default = true, title = "One-click buy", section = READY, category = "checkout")
    val bannerText = FeatureFlag.Text(
        "checkout_banner",
        default = "Free shipping today",
        title = "Checkout banner text",
        section = READY,
        category = "checkout",
    )
    val maxCartTotal = FeatureFlag.Number(
        "max_cart_total",
        default = 2500.0,
        title = "Max cart total",
        section = READY,
        category = "checkout",
    )
    val checkoutConfig = FeatureFlag.Json(
        "checkout_config",
        default = """{"expressPay":true,"methods":["card","paypal"],"timeoutSeconds":30}""",
        title = "Checkout config",
        section = READY,
        category = "checkout",
    )
    val savedCards = FeatureFlag.Toggle("saved_cards", default = true, title = "Saved cards", section = READY, category = "payment")
    val liveShopping = FeatureFlag.Toggle("live_shopping", default = false, title = "Live shopping", section = IN_DEV, category = "growth")
    val searchPageSize = FeatureFlag.Number(
        "search_limit",
        default = 20.0,
        title = "Search page size",
        section = IN_DEV,
        category = "platform",
        wholeNumber = true,
        range = 1.0..100.0,
    )

    val all = listOf(newCheckout, oneClickBuy, bannerText, maxCartTotal, checkoutConfig, savedCards, liveShopping, searchPageSize)
}
