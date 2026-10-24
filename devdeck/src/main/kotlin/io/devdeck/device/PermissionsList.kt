package io.devdeck.device

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.devdeck.components.BulletItem
import io.devdeck.components.TextItem

@Composable
public fun PermissionsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permissions = remember(context) { requestedPermissions(context) }
    Column(modifier.fillMaxWidth()) {
        if (permissions.isEmpty()) TextItem("No permissions requested")
        permissions.forEach { (name, granted) ->
            BulletItem(label = name.substringAfterLast('.'), value = if (granted) "Granted ✅" else "Denied ❌")
        }
    }
}

private fun requestedPermissions(context: Context): List<Pair<String, Boolean>> {
    val info: PackageInfo = runCatching {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
    }.getOrNull() ?: return emptyList()
    return info.requestedPermissions.orEmpty()
        .map { it to (ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED) }
        .sortedWith(compareBy({ it.second }, { it.first }))
}
