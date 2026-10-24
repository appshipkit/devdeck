package io.devdeck.timber

import io.devdeck.console.ConsoleBuffer

public object TimberLogs {
    public val buffer: ConsoleBuffer = ConsoleBuffer(capacity = 5_000)
}
