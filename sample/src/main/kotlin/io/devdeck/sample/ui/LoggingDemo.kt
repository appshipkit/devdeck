@file:SuppressLint("LogNotTimber")

package io.devdeck.sample.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.devdeck.logs.DeckLogger
import timber.log.Timber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoggingDemo(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Logging demo", style = MaterialTheme.typography.titleMedium)
        Text(
            "Logcat lines show up in Logs → Logcat, Timber calls in Logs → Timber logs, and DeckLogger entries in their own tag rows.",
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { Log.i("Checkout", "Cart opened with 3 items") }) { Text("Logcat info") }
            OutlinedButton(onClick = { Log.e("Checkout", "Payment declined", IllegalStateException("Card expired")) }) {
                Text("Logcat error")
            }
            OutlinedButton(onClick = { Timber.d("Product page loaded for sku=AJ1-CHICAGO") }) { Text("Timber debug") }
            OutlinedButton(onClick = { Timber.e(IllegalArgumentException("Size 10 unavailable"), "Add to cart failed") }) {
                Text("Timber error")
            }
            OutlinedButton(onClick = { DeckLogger.log("button_tapped", tag = "Analytics", payload = "{\"screen\":\"main\"}") }) {
                Text("Analytics event")
            }
        }
    }
}
