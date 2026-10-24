package io.devdeck.sample

import android.app.Application
import io.devdeck.DevDeck
import io.devdeck.flags.FeatureFlagStore
import io.devdeck.flags.SharedPreferencesFlagStore
import io.devdeck.logs.DeckLogger
import io.devdeck.plugin.DeckPlugin
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
            includeActivity = { it is LegacyViewActivity },
        )
        DeckLogger.log("App started")
        Timber.i("Sample app started")
    }
}
