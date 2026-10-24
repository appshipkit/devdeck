package io.devdeck.device

import android.content.Context
import android.os.Build

internal data class AppVersionInfo(val versionName: String?, val versionCode: Long)

internal fun appVersionInfo(context: Context): AppVersionInfo? = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    AppVersionInfo(info.versionName, code)
}.getOrNull()
