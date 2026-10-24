package io.devdeck.simulation

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Collections
import java.util.WeakHashMap

public object MemorySimulator {

    public var heldBytes: Long by mutableLongStateOf(0L)
        private set

    public var killOnBackground: Boolean by mutableStateOf(false)

    private val chunks = mutableListOf<ByteArray>()
    private val activities: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())
    private var application: Application? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activities += activity
        }

        override fun onActivityStopped(activity: Activity) {
            if (killOnBackground && !activity.isChangingConfigurations) {
                handler.postDelayed({ if (killOnBackground && isInBackground()) Process.killProcess(Process.myPid()) }, KILL_DELAY_MILLIS)
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            activities -= activity
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    public fun init(application: Application) {
        if (this.application != null) return
        this.application = application
        application.registerActivityLifecycleCallbacks(callbacks)
    }

    @Suppress("DEPRECATION")
    public fun send(warning: MemoryWarning) {
        val app = application ?: return
        val targets: List<ComponentCallbacks2> = listOf(app) + activities.toList()
        targets.forEach { target ->
            val level = warning.level
            if (level == null) target.onLowMemory() else target.onTrimMemory(level)
        }
    }

    public fun allocate(megabytes: Int): Long {
        val before = heldBytes
        try {
            repeat(megabytes) { chunks += ByteArray(CHUNK_BYTES) }
        } catch (_: OutOfMemoryError) {
            chunks.removeLastOrNull()
        }
        heldBytes = chunks.size.toLong() * CHUNK_BYTES
        return heldBytes - before
    }

    public fun fillHeap(percent: Int): Long {
        val runtime = Runtime.getRuntime()
        val target = runtime.maxMemory() * percent.coerceIn(1, 99) / 100
        val used = runtime.totalMemory() - runtime.freeMemory()
        val missing = target - used
        if (missing <= 0) return 0L
        return allocate((missing / CHUNK_BYTES).toInt())
    }

    public fun release() {
        chunks.clear()
        heldBytes = 0L
        Runtime.getRuntime().gc()
    }

    private fun isInBackground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    private const val CHUNK_BYTES = 1024 * 1024
    private const val KILL_DELAY_MILLIS = 1_000L
}
