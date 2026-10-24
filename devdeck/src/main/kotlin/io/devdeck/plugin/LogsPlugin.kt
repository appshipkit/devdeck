package io.devdeck.plugin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.devdeck.components.PageLinkItem
import io.devdeck.logs.DeckLogger
import io.devdeck.logs.LogList

public class LogsPlugin(
    private val tags: List<String> = listOf(DeckLogger.DEFAULT_TAG),
    private val lifecycleLogs: Boolean = false,
    private val allLogs: Boolean = false,
    override val section: String? = "Logs 📚",
) : DeckPlugin {

    override val id: String = "logs"

    override fun onAttach(context: Context) {
        if (lifecycleLogs) (context as? Application)?.registerActivityLifecycleCallbacks(LifecycleLogger)
    }

    @Composable
    override fun Content() {
        val entries by DeckLogger.entries.collectAsState()
        if (allLogs) LogPageLink("All logs", entries.size, tag = null)
        tags.forEach { tag -> LogPageLink("$tag logs", entries.count { it.tag == tag }, tag) }
        if (lifecycleLogs) LogPageLink("Lifecycle logs", entries.count { it.tag == LIFECYCLE_TAG }, LIFECYCLE_TAG)
    }

    @Composable
    private fun LogPageLink(title: String, count: Int, tag: String?) {
        PageLinkItem(
            title = title,
            subtitle = when (count) {
                0 -> "No entries"
                1 -> "1 entry"
                else -> "$count entries"
            },
        ) { LogList(tag = tag) }
    }

    public companion object {
        public const val LIFECYCLE_TAG: String = "Lifecycle"
    }

    private object LifecycleLogger : Application.ActivityLifecycleCallbacks {
        private fun log(activity: Activity, event: String) = DeckLogger.log("${activity.javaClass.simpleName}: $event", tag = LIFECYCLE_TAG)

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = log(activity, "onCreate")
        override fun onActivityStarted(activity: Activity) = log(activity, "onStart")
        override fun onActivityResumed(activity: Activity) = log(activity, "onResume")
        override fun onActivityPaused(activity: Activity) = log(activity, "onPause")
        override fun onActivityStopped(activity: Activity) = log(activity, "onStop")
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = log(activity, "onDestroy")
    }
}
