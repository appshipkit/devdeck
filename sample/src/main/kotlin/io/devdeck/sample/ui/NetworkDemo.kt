package io.devdeck.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.devdeck.sample.network.SampleApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetworkDemo(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Network demo", style = MaterialTheme.typography.titleMedium)
        Text(
            "Each button sends a real request through DevDeckOkHttpInterceptor. Open Logs → Network activity to inspect them, " +
                "or Simulate → Network to make them slow or fail.",
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = SampleApi::loadProfile) { Text("Load profile") }
            OutlinedButton(onClick = SampleApi::placeOrder) { Text("Place order") }
            OutlinedButton(onClick = SampleApi::listEuropeanCountries) { Text("List countries") }
            OutlinedButton(onClick = SampleApi::loadMissingProduct) { Text("Missing product") }
            OutlinedButton(onClick = SampleApi::searchProducts) { Text("Slow search") }
            OutlinedButton(onClick = SampleApi::checkPaymentsHealth) { Text("Payments offline") }
        }
        RequestStatus()
    }
}

@Composable
private fun RequestStatus() {
    val inFlight by SampleApi.inFlight.collectAsState()
    val lastResult by SampleApi.lastResult.collectAsState()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (inFlight > 0) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Text("Loading $inFlight request(s)…", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(lastResult ?: "No requests yet", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
