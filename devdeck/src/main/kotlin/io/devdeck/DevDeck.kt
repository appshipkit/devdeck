package io.devdeck

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.doOnAttach
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.devdeck.gesture.SWIPE_DISTANCE
import io.devdeck.gesture.TwoFingerSwipeTracker
import io.devdeck.gesture.activePointers
import io.devdeck.gesture.inputObserver
import io.devdeck.plugin.DeckPlugin
import io.devdeck.plugin.DeckPluginContent
import io.devdeck.plugin.DeckPluginRegistry
import io.devdeck.plugin.DeckPlugins

public object DevDeck {

    public val state: DeckState = DeckState()

    public var isEnabled: Boolean by mutableStateOf(true)

    public var config: DeckConfig? by mutableStateOf(null)
        private set

    public val isInstalled: Boolean get() = application != null

    private var application: Application? = null

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) = attach(activity)
        override fun onActivityStopped(activity: Activity) {
            if (state.isOpen) state.close()
        }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            if (!activity.isChangingConfigurations) state.navigateToRoot()
        }
    }

    public fun install(application: Application, config: DeckConfig) {
        this.config = config
        DeckPluginRegistry.attach(config.plugins, application, reattachReplaced = true)
        if (this.application == null) {
            this.application = application
            application.registerActivityLifecycleCallbacks(callbacks)
        }
    }

    public fun install(
        application: Application,
        plugins: List<DeckPlugin> = DeckPlugins.defaults(),
        trigger: DeckTrigger = DeckTrigger.FloatingButton(),
        gestures: DeckGestures = DeckGestures(),
        style: DeckStyle = DeckStyle(),
        side: DeckSide = DeckSide.End,
        includeActivity: (Activity) -> Boolean = { true },
        excludedActivityPrefixes: Set<String> = DeckExclusions.PAYMENT_SDKS,
    ) {
        install(
            application,
            DeckConfig(
                plugins = plugins,
                trigger = trigger,
                gestures = gestures,
                style = style,
                side = side,
                includeActivity = includeActivity,
                excludedActivityPrefixes = excludedActivityPrefixes,
            ),
        )
    }

    public fun updateConfig(transform: (DeckConfig) -> DeckConfig) {
        val updated = config?.let(transform) ?: return
        application?.let { DeckPluginRegistry.attach(updated.plugins, it, reattachReplaced = true) }
        config = updated
    }

    public fun uninstall() {
        application?.unregisterActivityLifecycleCallbacks(callbacks)
        application = null
        config = null
        state.snapClosed()
        state.navigateToRoot()
    }

    public fun open(): Boolean {
        if (!isInstalled || !isEnabled) return false
        state.open()
        return true
    }

    public fun close() {
        state.close()
    }

    public fun toggle() {
        if (state.isOpen) close() else open()
    }

    private fun attach(activity: Activity) {
        val current = config ?: return
        if (activity !is ComponentActivity) return
        if (DeckExclusions.isExcluded(activity, current.excludedActivityPrefixes) || !current.includeActivity(activity)) return
        val decorView = activity.window.decorView as? ViewGroup ?: return
        if (decorView.findViewWithTag<View>(DECK_OVERLAY_TAG) != null || HostedActivities.contains(activity)) return
        // A ComposeView can't be measured before it's attached, and some windows (edge-to-edge activities on recent
        // Android) measure views added in onStart without attaching them first. So the overlay is added after the
        // window is attached, posted so it isn't added in the middle of the attach pass (which would skip it).
        if (!decorView.isAttachedToWindow) {
            decorView.doOnAttach { decorView.post { attach(activity) } }
            return
        }

        val overlay = ComposeView(activity).apply {
            tag = DECK_OVERLAY_TAG
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setViewTreeOnBackPressedDispatcherOwner(activity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val activeConfig = config
                if (activeConfig != null) {
                    DevDeckOverlay(
                        drawerContent = {
                            DeckPluginContent(
                                activeConfig.plugins,
                                collapsibleSections = activeConfig.style.collapsibleSections,
                                oneSectionAtATime = activeConfig.style.oneSectionAtATime,
                            )
                            activeConfig.content?.invoke(this)
                        },
                        state = state,
                        enabled = isEnabled,
                        side = activeConfig.side,
                        gestures = activeConfig.gestures,
                        trigger = activeConfig.trigger,
                        style = activeConfig.style,
                    )
                }
            }
        }
        decorView.addView(overlay, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        observeInput(activity)
    }

    private fun observeInput(activity: Activity) {
        val observer = activity.inputObserver() ?: return
        if (observer.hasGlobalListeners) return
        observer.hasGlobalListeners = true
        val tracker = TwoFingerSwipeTracker(SWIPE_DISTANCE.value * activity.resources.displayMetrics.density)
        observer.touchListeners += { event ->
            val active = config
            if (active != null && isEnabled && active.gestures.twoFingerSwipe && !state.isOpen && !HostedActivities.contains(activity)) {
                val rtl = activity.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
                val drawerOnRight = (active.side == DeckSide.End) != rtl
                if (tracker.update(event.activePointers(), towardsLeft = drawerOnRight)) state.open()
            }
        }
        observer.keyListeners += { event ->
            isEnabled && !HostedActivities.contains(activity) && config?.gestures?.keyboardShortcut.handle(event, state)
        }
    }

    internal fun removeOverlay(activity: Activity) {
        val decorView = activity.window?.decorView as? ViewGroup ?: return
        decorView.findViewWithTag<View>(DECK_OVERLAY_TAG)?.let(decorView::removeView)
    }
}

/** Tag of the view DevDeck adds to each activity, so captures and tap tracking can skip it. */
internal const val DECK_OVERLAY_TAG = "io.devdeck.overlay"
