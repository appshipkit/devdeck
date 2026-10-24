package io.devdeck.device

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import androidx.compose.runtime.Immutable
import androidx.core.os.ConfigurationCompat
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Immutable
public data class SnapshotSection(val title: String, val rows: List<Pair<String, String>>)

@Immutable
public data class SystemSnapshot(val appName: String, val generatedAtMillis: Long, val sections: List<SnapshotSection>) {

    public fun toText(): String = buildString {
        appendLine("$appName · device & app snapshot")
        sections.forEach { section ->
            appendLine()
            appendLine(section.title)
            section.rows.forEach { (label, value) -> appendLine("• $label: $value") }
        }
        appendLine()
        append("Generated ${formatDateTime(generatedAtMillis)}")
    }
}

public fun systemSnapshot(context: Context, buildInfo: Map<String, String> = emptyMap()): SystemSnapshot {
    val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
    return SystemSnapshot(
        appName = appName,
        generatedAtMillis = System.currentTimeMillis(),
        sections = listOfNotNull(
            SnapshotSection("App", appRows(context)),
            buildInfo.takeIf { it.isNotEmpty() }?.let { SnapshotSection("Build", it.toList()) },
            SnapshotSection("Device", deviceRows(context)),
        ),
    )
}

internal fun appRows(context: Context): List<Pair<String, String>> {
    val packageInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    val version = appVersionInfo(context)
    val appInfo = context.applicationInfo
    val debuggable = appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val buildConfig = buildConfigValues(context)
    return listOfNotNull(
        version?.let { "Version" to "${it.versionName} (${it.versionCode})" },
        "Package" to context.packageName,
        buildConfig["BUILD_TYPE"]?.let { "Build type" to it },
        buildConfig["FLAVOR"]?.takeIf { it.isNotEmpty() }?.let { "Flavor" to it },
        "Debuggable" to if (debuggable) "Yes" else "No",
        "R8" to r8State(),
        signingSummary(context)?.let { "Signed with" to it },
        "Installed from" to installerName(context),
        "Target SDK" to appInfo.targetSdkVersion.toString(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) "Min SDK" to appInfo.minSdkVersion.toString() else null,
        packageInfo?.let { "Installed" to formatDateTime(it.firstInstallTime) },
        packageInfo?.let { "Updated" to formatDateTime(it.lastUpdateTime) },
    )
}

// BuildConfig lives in the app's namespace, which can differ from the application id (e.g. an `.qa` suffix),
// so try the id, its parent packages and the Application class's package. R8 usually removes it.
private fun buildConfigValues(context: Context): Map<String, String> {
    val packageName = context.packageName
    val candidates = buildList {
        var name = packageName
        while (name.contains('.')) {
            add(name)
            name = name.substringBeforeLast('.')
        }
        context.applicationInfo.className?.substringBeforeLast('.', "")?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }.distinct()
    val buildConfig = candidates.firstNotNullOfOrNull { runCatching { Class.forName("$it.BuildConfig") }.getOrNull() } ?: return emptyMap()
    return listOf("BUILD_TYPE", "FLAVOR").mapNotNull { field ->
        runCatching { buildConfig.getField(field).get(null) as? String }.getOrNull()?.let { field to it }
    }.toMap()
}

// R8 renames classes when it obfuscates and drops Kotlin metadata when it shrinks, unless the app keeps them.
private class R8Probe

private fun r8State(): String {
    val renamed = R8Probe::class.java.name != "io.devdeck.device.R8Probe"
    val metadataStripped = !R8Probe::class.java.isAnnotationPresent(Metadata::class.java)
    return when {
        renamed -> "Obfuscated"
        metadataStripped -> "Shrunk, not obfuscated"
        else -> "Off"
    }
}

private fun signingSummary(context: Context): String? = runCatching {
    val pm = context.packageManager
    val certificates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
    }
    val bytes = certificates?.firstOrNull()?.toByteArray() ?: return null
    val certificate = CertificateFactory.getInstance("X.509").generateCertificate(bytes.inputStream()) as X509Certificate
    val key = if (certificate.subjectX500Principal.name.contains("CN=Android Debug")) "Debug key" else "Release key"
    val fingerprint = MessageDigest.getInstance("SHA-256").digest(bytes).take(FINGERPRINT_BYTES)
        .joinToString(":") { "%02X".format(it) }
    "$key · SHA-256 $fingerprint…"
}.getOrNull()

private fun installerName(context: Context): String {
    val installer = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()
    return when (installer) {
        null -> "adb or sideloaded"
        "com.android.vending" -> "Google Play"
        "dev.firebase.appdistribution" -> "Firebase App Distribution"
        "com.android.shell" -> "adb"
        else -> installer
    }
}

private const val FINGERPRINT_BYTES = 8

private fun deviceRows(context: Context): List<Pair<String, String>> {
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(metrics)
    val configuration = context.resources.configuration
    return listOf(
        "Device" to "${Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.US) }} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Security patch" to Build.VERSION.SECURITY_PATCH,
        "Screen" to "${metrics.widthPixels} × ${metrics.heightPixels} px · " +
            "${(metrics.widthPixels / metrics.density).roundToInt()} × ${(metrics.heightPixels / metrics.density).roundToInt()} dp",
        "Density" to "${metrics.densityDpi} dpi (${metrics.density}x)",
        "Font scale" to "${configuration.fontScale}x",
        "Locale" to ConfigurationCompat.getLocales(configuration)[0]?.toLanguageTag().orEmpty(),
        "Time zone" to TimeZone.getDefault().id,
        "CPU ABI" to Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
    )
}

private fun formatDateTime(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))
