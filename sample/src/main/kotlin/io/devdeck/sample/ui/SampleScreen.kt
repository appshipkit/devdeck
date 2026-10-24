package io.devdeck.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.devdeck.DevDeckHost
import io.devdeck.flags.FeatureFlagStore
import io.devdeck.plugin.DeckPlugin
import io.devdeck.rememberDeckState
import io.devdeck.sample.debugmenu.sampleDeckStyle

@Composable
fun SampleScreen(plugins: List<DeckPlugin>, flagStore: FeatureFlagStore, onOpenLegacy: () -> Unit) {
    val drawerState = rememberDeckState()
    val options = rememberDrawerOptions()

    DevDeckHost(
        plugins = plugins,
        state = drawerState,
        enabled = options.enabled,
        side = options.side,
        gestures = options.gestures,
        trigger = options.trigger,
        style = sampleDeckStyle,
    ) {
        Scaffold { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("DevDeck sample", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "This screen hosts the drawer with DevDeckHost. Open it with a two-finger swipe left, " +
                        "the floating bug, the button below, or Ctrl + Shift + D on a keyboard (works in the emulator).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = drawerState::open, enabled = options.enabled, modifier = Modifier.fillMaxWidth()) {
                    Text("Open drawer")
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DrawerControls(options)

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                CurrentValues(flagStore)

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NetworkDemo()

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                LoggingDemo()

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                OutlinedButton(onClick = onOpenLegacy, modifier = Modifier.fillMaxWidth()) {
                    Text("Open View-based screen (global install)")
                }
            }
        }
    }
}
