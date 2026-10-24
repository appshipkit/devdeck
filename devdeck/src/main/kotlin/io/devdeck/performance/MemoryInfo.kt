package io.devdeck.performance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.devdeck.components.ActionPill
import io.devdeck.components.ActionPillRow
import io.devdeck.components.BulletItem
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
public fun MemoryInfo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val memory by produceState(PerformanceMonitor.memory(context), refresh) {
        while (true) {
            value = PerformanceMonitor.memory(context)
            delay(2_000)
        }
    }
    Column(modifier.fillMaxWidth()) {
        BulletItem("Java heap", "${mb(memory.heapUsedBytes)} of ${mb(memory.heapMaxBytes)}")
        BulletItem("Native heap", mb(memory.nativeHeapBytes))
        BulletItem("Device available", "${mb(memory.systemAvailableBytes)} of ${mb(memory.systemTotalBytes)}")
        BulletItem("Low memory", if (memory.isLowMemory) "Yes" else "No")
        ActionPillRow {
            ActionPill(text = "Run garbage collection", onClick = {
                Runtime.getRuntime().gc()
                refresh++
            })
        }
    }
}

private fun mb(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
