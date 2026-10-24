package io.devdeck.breadcrumbs

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import io.devdeck.DeckVisibility
import io.devdeck.gesture.inputObserver
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.abs

internal object BreadcrumbCollector {

    private var application: Application? = null
    private var lifecycle = true
    private var taps = true
    private var startedActivities = 0
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val observed: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) =
            screen(activity, if (savedInstanceState == null) "created" else "recreated")

        override fun onActivityStarted(activity: Activity) {
            if (startedActivities++ == 0) Breadcrumbs.add("App came to the foreground", BreadcrumbType.App)
            screen(activity, "started")
        }

        override fun onActivityResumed(activity: Activity) {
            screen(activity, "resumed")
            if (taps) observeTaps(activity)
        }

        override fun onActivityPaused(activity: Activity) = screen(activity, "paused")

        override fun onActivityStopped(activity: Activity) {
            screen(activity, "stopped")
            startedActivities = (startedActivities - 1).coerceAtLeast(0)
            if (startedActivities == 0 && !activity.isChangingConfigurations) {
                Breadcrumbs.add("App went to the background", BreadcrumbType.App)
            }
        }

        override fun onActivityDestroyed(activity: Activity) = screen(activity, "destroyed")

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    private val componentCallbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            Breadcrumbs.add("System memory warning (level $level)", BreadcrumbType.App)
        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            Breadcrumbs.add("Configuration changed", BreadcrumbType.App)
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {
            Breadcrumbs.add("System low memory", BreadcrumbType.App)
        }
    }

    fun install(application: Application, lifecycle: Boolean, taps: Boolean) {
        this.lifecycle = lifecycle
        this.taps = taps
        if (this.application != null) return
        this.application = application
        application.registerActivityLifecycleCallbacks(callbacks)
        application.registerComponentCallbacks(componentCallbacks)
    }

    private fun screen(activity: Activity, event: String) {
        if (lifecycle) Breadcrumbs.add("${activity.javaClass.simpleName} $event", BreadcrumbType.Screen)
    }

    private fun observeTaps(activity: Activity) {
        if (!observed.add(activity)) return
        val observer = activity.inputObserver() ?: return
        val slop = ViewConfiguration.get(activity).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        observer.touchListeners += { event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                }

                MotionEvent.ACTION_UP -> if (taps && abs(event.rawX - downX) < slop && abs(event.rawY - downY) < slop) {
                    val label = describeTap(activity, event.rawX.toInt(), event.rawY.toInt())
                    if (label !=
                        null
                    ) {
                        handler.postDelayed({
                            if (DeckVisibility.visibleDrawers ==
                                0
                            ) {
                                Breadcrumbs.tap(label)
                            }
                        }, DRAWER_CHECK_DELAY_MILLIS)
                    }
                }
            }
        }
    }

    private fun describeTap(activity: Activity, x: Int, y: Int): String? {
        val root = activity.window?.decorView as? ViewGroup ?: return null
        if (DeckVisibility.visibleDrawers > 0) return null
        val target = findTarget(root, x, y) ?: return null
        val screen = activity.javaClass.simpleName
        if (target.javaClass.name.startsWith(COMPOSE_VIEW_PREFIX)) return "$screen · Compose content at ($x, $y)"
        return "$screen · ${describe(target)}"
    }

    private fun findTarget(view: View, x: Int, y: Int): View? {
        if (view.visibility != View.VISIBLE || !view.contains(x, y)) return null
        if (view is ViewGroup) {
            for (index in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(index)
                if (child.tag == DECK_OVERLAY_TAG) continue
                findTarget(child, x, y)?.let { return it }
            }
        }
        return view.takeIf { it.isClickable || it.javaClass.name.startsWith(COMPOSE_VIEW_PREFIX) }
    }

    private fun describe(view: View): String {
        val text = (view as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }
        val description = view.contentDescription?.toString()?.takeIf { it.isNotBlank() }
        val id = if (view.id != View.NO_ID) runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull() else null
        val name = text?.let { "\"$it\"" } ?: description?.let { "\"$it\"" } ?: id ?: "unnamed"
        return "${view.javaClass.simpleName} $name" + if (id != null && (text != null || description != null)) " (#$id)" else ""
    }

    private fun View.contains(x: Int, y: Int): Boolean {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return x >= location[0] && x < location[0] + width && y >= location[1] && y < location[1] + height
    }

    private const val DRAWER_CHECK_DELAY_MILLIS = 150L
    private const val COMPOSE_VIEW_PREFIX = "androidx.compose.ui.platform.AndroidComposeView"
    private const val DECK_OVERLAY_TAG = "io.devdeck.overlay"
}
