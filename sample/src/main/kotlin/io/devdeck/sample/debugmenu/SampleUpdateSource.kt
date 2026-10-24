package io.devdeck.sample.debugmenu

import io.devdeck.updates.AvailableUpdate
import io.devdeck.updates.UpdateSource
import kotlinx.coroutines.delay

object SampleUpdateSource : UpdateSource {

    override val name: String = "the sample update feed"

    override suspend fun checkForUpdate(): AvailableUpdate {
        delay(1_200)
        return AvailableUpdate(
            versionName = "0.2.0",
            versionCode = 200,
            releaseNotes = "• Card layout for the drawer\n• Network and memory simulators\n• Bug fixes",
        )
    }

    override suspend fun availableBuilds(): List<AvailableUpdate> = listOf(
        AvailableUpdate("0.2.0", 200, "Card layout, simulators and fixes"),
        AvailableUpdate("0.1.1", 110, "Fixes for the network inspector"),
        AvailableUpdate("0.1.0", 100, "First test build"),
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
