package io.devdeck.breadcrumbs

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public object Breadcrumbs {

    public var maxEntries: Int = DEFAULT_MAX_ENTRIES

    public var captureNetwork: Boolean = true

    internal var clock: () -> Long = System::currentTimeMillis

    private val _entries = MutableStateFlow<List<Breadcrumb>>(emptyList())
    public val entries: StateFlow<List<Breadcrumb>> = _entries.asStateFlow()

    public fun add(message: String, type: BreadcrumbType = BreadcrumbType.Custom) {
        val crumb = Breadcrumb(clock(), type, message)
        _entries.update { (it + crumb).takeLast(maxEntries) }
    }

    public fun screen(name: String) {
        add(name, BreadcrumbType.Screen)
    }

    public fun tap(label: String) {
        add(label, BreadcrumbType.Tap)
    }

    public fun clear() {
        _entries.value = emptyList()
    }

    public fun recent(count: Int): List<Breadcrumb> = _entries.value.takeLast(count)

    public fun format(crumbs: List<Breadcrumb>): String {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return crumbs.joinToString("\n") {
            "${formatter.format(Date(it.timestampMillis))}  ${it.type.label.padEnd(TYPE_WIDTH)} ${it.message}"
        }
    }

    private const val DEFAULT_MAX_ENTRIES = 300
    private const val TYPE_WIDTH = 8
}
