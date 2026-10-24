package io.devdeck.device

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import io.devdeck.LocalDeckColors
import io.devdeck.components.ActionPill
import io.devdeck.components.HorizontalPadding
import io.devdeck.components.TextItem
import io.devdeck.internal.findActivity
import kotlinx.coroutines.delay

@Composable
public fun PermissionsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val permissions = remember(context, version) { AppPermissions.load(context) }
    RefreshOnResume { version++ }
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
        }
        permissions.forEach { PermissionRow(it) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionRow(permission: AppPermission) {
    val isDark = LocalDeckColors.current?.isDark == true
    val tile = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.07f else 0.04f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(tile, RoundedCornerShape(14.dp))
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(permission.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            permission.owner?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PermissionAction(permission)
        }
        StatusPill(granted = permission.granted)
    }
}

@Composable
private fun PermissionAction(permission: AppPermission) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when {
        permission.kind == PermissionKind.InstallTime -> Note("Always granted at install")

        permission.kind == PermissionKind.Unavailable -> Note("Not on this Android version")

        permission.kind == PermissionKind.Special ->
            ActionPill(text = "Open settings", onClick = { AppPermissions.openSettings(context, permission) }, compact = true)

        !permission.granted && activity != null ->
            ActionPill(text = "Request", onClick = { AppPermissions.request(activity, permission) }, compact = true)

        AppPermissions.canRevoke(permission) && activity != null -> RevokePill(onRevoke = {
            if (AppPermissions.revoke(activity, permission)) {
                Toast.makeText(
                    context,
                    "${permission.displayName} is removed when the app closes. Reopen it to continue.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        })

        permission.granted -> Note("Revoke needs Android 13+ or App settings", muted)
    }
}

@Composable
private fun RevokePill(onRevoke: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(confirming) {
        if (confirming) {
            delay(CONFIRM_TIMEOUT_MILLIS)
            confirming = false
        }
    }
    ActionPill(
        text = if (confirming) "Tap again to revoke" else "Revoke",
        onClick = {
            if (confirming) {
                confirming = false
                onRevoke()
            } else {
                confirming = true
            }
        },
        color = MaterialTheme.colorScheme.error,
        compact = true,
    )
}

@Composable
private fun Note(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = color)
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(
            text = if (granted) "Granted" else "Denied",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun RefreshOnResume(onResume: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        val owner = view.findViewTreeLifecycleOwner()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) onResume() }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
}

private const val CONFIRM_TIMEOUT_MILLIS = 3_000L
