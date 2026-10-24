package io.devdeck.network

import androidx.compose.runtime.Immutable

@Immutable
public data class NetworkGroup(
    val key: String,
    val title: String,
    val method: String,
    val host: String,
    val isGraphQl: Boolean,
    val calls: List<NetworkCall>,
) {
    val count: Int get() = calls.size

    val failures: Int get() = calls.count { it.isFailure }

    val pending: Int get() = calls.count { !it.isComplete }

    val latest: NetworkCall get() = calls.first()

    private val durations: List<Long> get() = calls.mapNotNull { it.durationMillis }

    val averageDurationMillis: Long? get() = durations.takeIf { it.isNotEmpty() }?.average()?.toLong()

    val minDurationMillis: Long? get() = durations.minOrNull()

    val maxDurationMillis: Long? get() = durations.maxOrNull()
}

public val NetworkCall.groupKey: String
    get() = operationName?.let { "graphql:$it" } ?: "$method $host$path"

internal fun List<NetworkCall>.groupByEndpoint(): List<NetworkGroup> = groupBy { it.groupKey }
    .map { (key, calls) ->
        val first = calls.first()
        NetworkGroup(
            key = key,
            title = first.displayName,
            method = first.method,
            host = first.host,
            isGraphQl = first.operationName != null,
            calls = calls,
        )
    }
    .sortedByDescending { it.latest.startedAtMillis }
