package io.devdeck.breadcrumbs

import androidx.compose.runtime.Immutable

public enum class BreadcrumbType(public val label: String) {
    Screen("Screen"),
    Tap("Tap"),
    Network("Network"),
    App("App"),
    Custom("Custom"),
}

@Immutable
public data class Breadcrumb(val timestampMillis: Long, val type: BreadcrumbType, val message: String)
