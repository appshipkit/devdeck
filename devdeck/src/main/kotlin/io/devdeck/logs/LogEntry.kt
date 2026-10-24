package io.devdeck.logs

import androidx.compose.runtime.Immutable

@Immutable
public data class LogEntry(val id: Long, val timestampMillis: Long, val tag: String, val message: String, val payload: String?)
