package io.devdeck.logs

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public object DeckLogger {
    public const val DEFAULT_TAG: String = "General"

    public var maxEntries: Int = 500

    private val nextId = AtomicLong()
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    public val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    public fun log(message: String, tag: String = DEFAULT_TAG, payload: String? = null) {
        val entry = LogEntry(nextId.incrementAndGet(), System.currentTimeMillis(), tag, message, payload)
        _entries.update { (listOf(entry) + it).take(maxEntries) }
    }

    public fun clear(tag: String? = null) {
        _entries.update { entries -> if (tag == null) emptyList() else entries.filterNot { it.tag == tag } }
    }
}
