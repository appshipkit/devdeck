package io.devdeck.internal

/** "1 line", "2 lines". */
internal fun plural(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"
