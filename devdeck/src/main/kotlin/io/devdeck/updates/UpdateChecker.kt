package io.devdeck.updates

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

public object UpdateChecker {

    public var source: UpdateSource? by mutableStateOf(null)
        private set

    public var status: UpdateStatus by mutableStateOf(UpdateStatus.Idle)
        internal set

    public var lastCheckedAtMillis: Long? by mutableStateOf(null)
        private set

    public var isSignedIn: Boolean by mutableStateOf(true)
        private set

    public val availableUpdate: AvailableUpdate?
        get() = when (val current = status) {
            is UpdateStatus.Available -> current.update
            is UpdateStatus.Installing -> current.update
            else -> null
        }

    internal var checkIntervalMillis: Long = DEFAULT_CHECK_INTERVAL_MILLIS
    internal var notifier: ((AvailableUpdate) -> Unit)? = null
    internal var clock: () -> Long = System::currentTimeMillis

    private var prefs: SharedPreferences? = null
    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    private var job: Job? = null

    public fun attach(context: Context, source: UpdateSource, checkIntervalMillis: Long = DEFAULT_CHECK_INTERVAL_MILLIS) {
        this.source = source
        this.checkIntervalMillis = checkIntervalMillis
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        isSignedIn = source.isSignedIn()
    }

    public fun check() {
        launchExclusive { refresh() }
    }

    public fun checkIfDue() {
        if (isCheckDue(lastCheckedAtMillis, clock(), checkIntervalMillis)) check()
    }

    public fun signIn() {
        val current = source ?: return
        launchExclusive {
            runCatching { current.signIn() }.onFailure { status = UpdateStatus.Failed(it.describe()) }
            isSignedIn = current.isSignedIn()
            if (isSignedIn) refresh()
        }
    }

    public fun signOut() {
        if (job?.isActive == true) return
        val current = source ?: return
        current.signOut()
        isSignedIn = current.isSignedIn()
        status = UpdateStatus.Idle
    }

    public fun install() {
        val current = source ?: return
        val update = availableUpdate ?: return
        launchExclusive { installUpdate(current, update) }
    }

    private fun launchExclusive(block: suspend () -> Unit) {
        if (job?.isActive == true) return
        job = scope.launch { block() }
    }

    internal suspend fun installUpdate(source: UpdateSource, update: AvailableUpdate) {
        status = UpdateStatus.Installing(update, null)
        runCatching { source.install(update) { progress -> status = UpdateStatus.Installing(update, progress) } }
            .onSuccess {
                status = UpdateStatus.UpToDate
                lastCheckedAtMillis = clock()
            }
            .onFailure { status = UpdateStatus.Failed(it.describe()) }
    }

    internal suspend fun refresh() {
        val current = source ?: return
        status = UpdateStatus.Checking
        status = runCatching { current.checkForUpdate() }.fold(
            onSuccess = { update -> if (update == null) UpdateStatus.UpToDate else UpdateStatus.Available(update) },
            onFailure = { UpdateStatus.Failed(it.describe()) },
        )
        lastCheckedAtMillis = clock()
        isSignedIn = current.isSignedIn()
        (status as? UpdateStatus.Available)?.update?.let(::notifyOnce)
    }

    internal fun attachForTest(source: UpdateSource) {
        this.source = source
        isSignedIn = source.isSignedIn()
    }

    internal fun reset() {
        source = null
        status = UpdateStatus.Idle
        lastCheckedAtMillis = null
        isSignedIn = true
        notifier = null
        prefs = null
        clock = System::currentTimeMillis
    }

    private fun notifyOnce(update: AvailableUpdate) {
        val notify = notifier ?: return
        val key = update.label
        if (prefs?.getString(KEY_NOTIFIED, null) == key) return
        prefs?.edit { putString(KEY_NOTIFIED, key) }
        notify(update)
    }

    private fun Throwable.describe(): String = message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName

    private const val PREFS = "devdeck_updates"
    private const val KEY_NOTIFIED = "notified_release"
    public const val DEFAULT_CHECK_INTERVAL_MILLIS: Long = 4L * 60 * 60 * 1000
}

internal fun isCheckDue(lastCheckedAtMillis: Long?, nowMillis: Long, intervalMillis: Long): Boolean =
    lastCheckedAtMillis == null || nowMillis - lastCheckedAtMillis >= intervalMillis
