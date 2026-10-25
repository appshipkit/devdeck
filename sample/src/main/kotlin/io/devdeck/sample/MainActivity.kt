package io.devdeck.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import io.devdeck.sample.ui.SampleScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as SampleApp
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                SampleScreen(
                    plugins = app.plugins,
                    flagStore = app.flagStore,
                    onOpenLegacy = { startActivity(Intent(this, LegacyViewActivity::class.java)) },
                    onOpenFeed = { startActivity(Intent(this, EndlessFeedActivity::class.java)) },
                )
            }
        }
    }
}
