package io.devdeck.sample

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.devdeck.DeckHeaderChip
import io.devdeck.DeckHeaderChips
import io.devdeck.DevDeck
import io.devdeck.crash.CrashReporter
import io.devdeck.flags.FeatureFlagStore
import io.devdeck.flags.SharedPreferencesFlagStore
import io.devdeck.logs.DeckLogger
import io.devdeck.plugin.DeckPlugin
import io.devdeck.sample.debugmenu.SampleEndpoints
import io.devdeck.sample.debugmenu.sampleDeckStyle
import io.devdeck.sample.debugmenu.samplePlugins
import timber.log.Timber

class SampleApp : Application() {

    lateinit var flagStore: FeatureFlagStore
        private set

    lateinit var plugins: List<DeckPlugin>
        private set

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        flagStore = SharedPreferencesFlagStore(this)
        plugins = samplePlugins(this, flagStore)

        DevDeck.install(
            application = this,
            plugins = plugins,
            style = sampleDeckStyle,
            includeActivity = { it is LegacyViewActivity || it is EndlessFeedActivity },
        )
        // Extra context in every crash report, and a header chip that follows the selected endpoint.
        CrashReporter.addSection("Endpoint") { SampleEndpoints.current(this).url }
        DeckHeaderChips.add("endpoint") {
            val endpoint = SampleEndpoints.current(LocalContext.current)
            if (endpoint.name != "Production") DeckHeaderChip(endpoint.name, Color(0xFFF57C00)) else null
        }
        DeckLogger.log("App started")
        Timber.i("Sample app started")
    }
}
