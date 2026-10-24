package io.devdeck

import android.app.Activity
import java.util.Collections
import java.util.WeakHashMap

internal object HostedActivities {
    private val activities: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())

    fun add(activity: Activity) {
        synchronized(activities) { activities.add(activity) }
    }

    fun remove(activity: Activity) {
        synchronized(activities) { activities.remove(activity) }
    }

    fun contains(activity: Activity): Boolean = synchronized(activities) { activity in activities }
}
