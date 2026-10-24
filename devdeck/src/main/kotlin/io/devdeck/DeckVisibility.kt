package io.devdeck

internal object DeckVisibility {
    private var count = 0

    val visibleDrawers: Int
        @Synchronized get() = count

    @Synchronized
    fun shown() {
        count++
    }

    @Synchronized
    fun hidden() {
        count = (count - 1).coerceAtLeast(0)
    }
}
