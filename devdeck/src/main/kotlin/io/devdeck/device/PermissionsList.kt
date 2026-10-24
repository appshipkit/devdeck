package io.devdeck.device

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.devdeck.LocalDeckColors
import io.devdeck.components.ActionPill
import io.devdeck.components.HorizontalPadding
import io.devdeck.components.TextItem

@Composable
public fun PermissionsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permissions = remember(context) { requestedPermissions(context) }
    Column(modifier.fillMaxWidth()) {
        if (permissions.isEmpty()) {
            TextItem("No permissions requested")
            return@Column
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = HorizontalPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${permissions.count { it.granted }} of ${permissions.size} granted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            ActionPill(text = "Manage", onClick = { openAppSettings(context) })
        }
        permissions.forEach { PermissionRow(it) }
    }
}

@Composable
private fun PermissionRow(permission: AppPermission) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = HorizontalPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(permission.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            permission.owner?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        StatusPill(granted = permission.granted)
    }
}

@Composable
private fun StatusPill(granted: Boolean) {
    val isDark = LocalDeckColors.current?.isDark == true
    val color = when {
        granted && isDark -> Color(0xFF81C784)
        granted -> Color(0xFF2E7D32)
        isDark -> Color(0xFFEF9A9A)
        else -> Color(0xFFC62828)
    }
    Text(
        text = if (granted) "Granted" else "Denied",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

internal data class AppPermission(val name: String, val granted: Boolean) {
    val displayName: String
        get() = name.substringAfterLast('.').lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    val owner: String?
        get() = name.substringBeforeLast('.', "").takeIf { it.isNotEmpty() && it != ANDROID_PERMISSION_PREFIX }
}

private fun requestedPermissions(context: Context): List<AppPermission> {
    val info: PackageInfo = runCatching {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
    }.getOrNull() ?: return emptyList()
    return info.requestedPermissions.orEmpty()
        .map { AppPermission(it, ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED) }
        .sortedWith(compareBy({ it.granted }, { it.displayName }))
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

private const val ANDROID_PERMISSION_PREFIX = "android.permission"
