package io.devdeck.device

import android.app.Activity
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.devdeck.internal.DeckSettings

internal enum class PermissionKind { Runtime, InstallTime, Special, Unavailable }

internal data class AppPermission(val name: String, val granted: Boolean, val kind: PermissionKind = PermissionKind.Runtime) {
    val displayName: String
        get() = name.substringAfterLast('.').lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    val owner: String?
        get() = name.substringBeforeLast('.', "").takeIf { it.isNotEmpty() && it != ANDROID_PERMISSION_PREFIX }
}

internal val PermissionOrder: Comparator<AppPermission> =
    compareBy({ it.granted }, { it.kind == PermissionKind.InstallTime }, { it.displayName })

internal object AppPermissions {

    fun load(context: Context): List<AppPermission> {
        val info = runCatching {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }.getOrNull() ?: return emptyList()
        return info.requestedPermissions.orEmpty()
            .map { name ->
                val kind = kindOf(context, name)
                AppPermission(name, isGranted(context, name, kind), kind)
            }
            .sortedWith(PermissionOrder)
    }

    fun request(activity: Activity, permission: AppPermission) {
        val prefs = DeckSettings.of(activity)
        val requestedBefore = prefs.getStringSet(KEY_REQUESTED, emptySet()).orEmpty().contains(permission.name)
        val blocked = requestedBefore && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.name)
        if (blocked) {
            openAppSettings(activity)
            return
        }
        prefs.edit { putStringSet(KEY_REQUESTED, prefs.getStringSet(KEY_REQUESTED, emptySet()).orEmpty() + permission.name) }
        ActivityCompat.requestPermissions(activity, arrayOf(permission.name), REQUEST_CODE)
    }

    fun canRevoke(permission: AppPermission): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permission.granted && permission.kind == PermissionKind.Runtime

    fun revoke(activity: Activity, permission: AppPermission): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        activity.revokeSelfPermissionOnKill(permission.name)
        activity.moveTaskToBack(true)
        return true
    }

    fun openSettings(context: Context, permission: AppPermission) {
        val intent = specialSettingsIntent(context, permission.name)
        if (intent == null || !context.tryStart(intent)) openAppSettings(context)
    }

    fun openAppSettings(context: Context) {
        context.tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)))
    }

    private fun kindOf(context: Context, name: String): PermissionKind {
        if (isSpecialSupported(name)) return PermissionKind.Special
        val info = runCatching { context.packageManager.getPermissionInfo(name, 0) }.getOrNull() ?: return PermissionKind.Unavailable
        val protection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.protection
        } else {
            @Suppress("DEPRECATION")
            info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
        }
        return if (protection == PermissionInfo.PROTECTION_DANGEROUS) PermissionKind.Runtime else PermissionKind.InstallTime
    }

    private fun isGranted(context: Context, name: String, kind: PermissionKind): Boolean {
        if (kind == PermissionKind.Special) {
            specialGranted(context, name)?.let { return it }
        }
        return ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
    }

    private fun isSpecialSupported(name: String): Boolean = when (name) {
        SYSTEM_ALERT_WINDOW, WRITE_SETTINGS -> true
        REQUEST_INSTALL_PACKAGES -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        MANAGE_EXTERNAL_STORAGE -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        SCHEDULE_EXACT_ALARM -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        else -> false
    }

    private fun specialGranted(context: Context, name: String): Boolean? = when {
        name == SYSTEM_ALERT_WINDOW -> Settings.canDrawOverlays(context)

        name == WRITE_SETTINGS -> Settings.System.canWrite(context)

        name == REQUEST_INSTALL_PACKAGES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
            context.packageManager.canRequestPackageInstalls()

        name == MANAGE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()

        name == SCHEDULE_EXACT_ALARM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

        else -> null
    }

    private fun specialSettingsIntent(context: Context, name: String): Intent? {
        val action = when {
            name == SYSTEM_ALERT_WINDOW -> Settings.ACTION_MANAGE_OVERLAY_PERMISSION

            name == WRITE_SETTINGS -> Settings.ACTION_MANAGE_WRITE_SETTINGS

            name == REQUEST_INSTALL_PACKAGES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES

            name == MANAGE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION

            name == SCHEDULE_EXACT_ALARM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM

            else -> return null
        }
        return Intent(action, packageUri(context))
    }

    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

    private fun Context.tryStart(intent: Intent): Boolean = try {
        startActivity(if (this is Activity) intent else intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private const val KEY_REQUESTED = "permissions.requested"
    private const val REQUEST_CODE = 0x0de9
    private const val SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW"
    private const val WRITE_SETTINGS = "android.permission.WRITE_SETTINGS"
    private const val REQUEST_INSTALL_PACKAGES = "android.permission.REQUEST_INSTALL_PACKAGES"
    private const val MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE"
    private const val SCHEDULE_EXACT_ALARM = "android.permission.SCHEDULE_EXACT_ALARM"
}

private const val ANDROID_PERMISSION_PREFIX = "android.permission"
