package io.devdeck.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.devdeck.flags.FeatureFlagStore
import io.devdeck.sample.debugmenu.SampleEndpoints
import io.devdeck.sample.debugmenu.SampleFlags

@Composable
fun CurrentValues(flagStore: FeatureFlagStore, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Current values", style = MaterialTheme.typography.titleMedium)
        Text("Endpoint: ${SampleEndpoints.current(context).name}")
        SampleFlags.all.forEach { flag ->
            Text("${flag.title}: ${flagStore[flag]}", maxLines = 1)
        }
    }
}
