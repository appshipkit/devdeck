package io.devdeck.actions

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public object AppActions {

    public var isWebViewDebuggingEnabled: Boolean by mutableStateOf(false)
        private set

    public fun setWebViewDebugging(enabled: Boolean): Boolean {
        val applied = runCatching { WebView.setWebContentsDebuggingEnabled(enabled) }.isSuccess
        if (applied) isWebViewDebuggingEnabled = enabled
        return applied
    }

    public fun clearCache(context: Context) {
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    }

    /** Same as [clearCache], but deletes on [Dispatchers.IO] so it is safe to call from the main thread. */
    public suspend fun clearCacheInBackground(context: Context) {
        withContext(Dispatchers.IO) { clearCache(context) }
    }

    public fun clearAppData(context: Context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
    }

    public fun restart(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val component = launchIntent.component ?: return
        context.startActivity(Intent.makeRestartActivityTask(component))
        exitProcess(0)
    }

    public fun killApp() {
        Process.killProcess(Process.myPid())
    }
}
