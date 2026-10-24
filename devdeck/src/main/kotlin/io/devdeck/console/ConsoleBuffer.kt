package io.devdeck.console

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public class ConsoleBuffer(public val capacity: Int = 5_000) {

    private val _lines = MutableStateFlow<List<ConsoleLine>>(emptyList())
    public val lines: StateFlow<List<ConsoleLine>> = _lines.asStateFlow()

    private val pending = ArrayList<ConsoleLine>()
    private var flushScheduled = false
    private var generation = 0L

    public fun append(line: ConsoleLine) {
        synchronized(pending) {
            pending.add(line)
            if (flushScheduled) return
            flushScheduled = true
        }
        scheduler.schedule(::flush, FLUSH_DELAY_MILLIS, TimeUnit.MILLISECONDS)
    }

    public fun clear() {
        synchronized(pending) {
            generation++
            pending.clear()
            _lines.value = emptyList()
        }
    }

    private fun flush() = publish(takeBatch())

    internal fun takeBatch(): Batch = synchronized(pending) {
        flushScheduled = false
        Batch(generation, pending.toList().also { pending.clear() })
    }

    // A batch taken before clear() belongs to the old generation and is dropped.
    internal fun publish(batch: Batch) {
        if (batch.lines.isEmpty()) return
        synchronized(pending) {
            if (batch.generation == generation) _lines.update { (it + batch.lines).takeLast(capacity) }
        }
    }

    internal class Batch(val generation: Long, val lines: List<ConsoleLine>)

    private companion object {
        const val FLUSH_DELAY_MILLIS = 250L

        val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "DevDeck-console").apply { isDaemon = true }
        }
    }
}
