package io.devdeck.sample.debugmenu

import android.content.Context
import io.devdeck.plugin.Endpoint
import io.devdeck.plugin.EndpointPlugin

object SampleEndpoints {
    val production = Endpoint("Production", "https://api.example.com")
    val staging = Endpoint("Staging", "https://staging.api.example.com")
    val dev = Endpoint("Dev", "https://dev.api.example.com")

    val all = listOf(production, staging, dev)

    val plugin = EndpointPlugin(all, default = staging)

    fun current(context: Context): Endpoint = plugin.current(context)
}
