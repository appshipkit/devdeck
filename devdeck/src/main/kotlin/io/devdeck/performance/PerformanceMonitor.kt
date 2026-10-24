package io.devdeck.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

public object PerformanceMonitor {

    public var isOverlayEnabled: Boolean by mutableStateOf(false)

    public fun memory(context: Context): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val system = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(system)
        return MemorySnapshot(
            heapUsedBytes = runtime.totalMemory() - runtime.freeMemory(),
            heapMaxBytes = runtime.maxMemory(),
            nativeHeapBytes = Debug.getNativeHeapAllocatedSize(),
            systemAvailableBytes = system.availMem,
            systemTotalBytes = system.totalMem,
            isLowMemory = system.lowMemory,
        )
    }
}

@Immutable
public data class MemorySnapshot(
    val heapUsedBytes: Long,
    val heapMaxBytes: Long,
    val nativeHeapBytes: Long,
    val systemAvailableBytes: Long,
    val systemTotalBytes: Long,
    val isLowMemory: Boolean,
)
