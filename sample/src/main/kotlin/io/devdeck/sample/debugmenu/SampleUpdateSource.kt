package io.devdeck.sample.debugmenu

import io.devdeck.updates.AvailableUpdate
import io.devdeck.updates.UpdateSource
import kotlinx.coroutines.delay

object SampleUpdateSource : UpdateSource {

    override val name: String = "the sample update feed"

    override suspend fun checkForUpdate(): AvailableUpdate {
        delay(1_200)
        // Pretends a newer build is out; DevDeck ignores anything at or below the installed version code.
        return AvailableUpdate(
            versionName = "9.0.0",
            versionCode = 90000,
            releaseNotes = "• Card layout for the drawer\n• Network and memory simulators\n• Bug fixes",
        )
    }

    override suspend fun availableBuilds(): List<AvailableUpdate> = listOf(
        AvailableUpdate("9.0.0", 90000, "Card layout, simulators and fixes"),
        AvailableUpdate("1.0.1", 10001, "Fixes for the network inspector"),
        AvailableUpdate("1.0.0", 10000, "First stable build"),
    )

    override suspend fun install(update: AvailableUpdate, onProgress: (Float?) -> Unit) {
        onProgress(null)
        delay(600)
        for (step in 1..10) {
            delay(200)
            onProgress(step / 10f)
        }
    }
}
