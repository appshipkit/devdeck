package io.devdeck.console

import androidx.compose.runtime.Immutable
import java.util.concurrent.atomic.AtomicLong

@Immutable
public data class ConsoleLine(
    val level: ConsoleLevel,
    val tag: String,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val threadId: Int? = null,
    val id: Long = nextId.incrementAndGet(),
) {
    private companion object {
        val nextId = AtomicLong()
    }
}
