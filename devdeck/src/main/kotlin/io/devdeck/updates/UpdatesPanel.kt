package io.devdeck.updates

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.devdeck.FOCUS_SCROLL_DELAY_MILLIS
import io.devdeck.LocalDeckColors
import io.devdeck.LocalDeckState
import io.devdeck.components.ActionPill
import io.devdeck.components.ActionPillRow
import io.devdeck.components.DropdownItem
import io.devdeck.components.HorizontalPadding
import io.devdeck.components.TitleAndSubtitle
import io.devdeck.device.appVersionInfo
import io.devdeck.internal.DeckIcons
import io.devdeck.plugin.UPDATES_PLUGIN_ID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
public fun UpdatesPanel(modifier: Modifier = Modifier, title: String = "Test build updates") {
    val source = UpdateChecker.source ?: return
    val status = UpdateChecker.status
    val update = UpdateChecker.availableUpdate
    val success = updateAvailableColor(LocalDeckColors.current?.isDark == true)
    val context = LocalContext.current
    val deckState = LocalDeckState.current
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(deckState?.pendingFocus) {
        if (deckState?.pendingFocus == UPDATES_PLUGIN_ID) {
            // Wait for the section to finish expanding so the scroll lands on the final position.
            delay(FOCUS_SCROLL_DELAY_MILLIS)
            bringIntoView.bringIntoView()
            deckState.pendingFocus = null
        }
    }
    Column(modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView)) {
        Row(
            Modifier.fillMaxWidth().padding(start = HorizontalPadding, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TitleAndSubtitle(title, statusText(status, source), enabled = true, modifier = Modifier.weight(1f))
            if (status == UpdateStatus.Checking) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = UpdateChecker::check, enabled = status !is UpdateStatus.Installing) {
                    Icon(DeckIcons.Refresh, contentDescription = "Check for updates", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        AnimatedVisibility(status is UpdateStatus.Installing) {
            val progress = (status as? UpdateStatus.Installing)?.progress
            if (progress == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = HorizontalPadding))
            } else {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = HorizontalPadding))
            }
        }
        AnimatedVisibility(update != null && !update.releaseNotes.isNullOrBlank()) {
            Text(
                text = update?.releaseNotes.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HorizontalPadding, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
            )
        }
        var chosen by remember(UpdateChecker.builds) { mutableStateOf<AvailableUpdate?>(null) }
        if (UpdateChecker.builds.isNotEmpty()) {
            BuildPicker(UpdateChecker.builds, chosen, onChosen = { chosen = it }, enabled = status !is UpdateStatus.Installing)
        }
        if (update != null || chosen != null || source.requiresSignIn || source.browseBuildsUrl != null) {
            ActionPillRow {
                chosen?.let { build ->
                    ActionPill(
                        text = "Install ${build.label}",
                        onClick = { UpdateChecker.install(build) },
                        enabled = status !is UpdateStatus.Installing,
                    )
                }
                if (update != null) {
                    ActionPill(
                        text = "Install latest",
                        onClick = UpdateChecker::install,
                        color = success,
                        enabled = status !is UpdateStatus.Installing,
                    )
                }
                source.browseBuildsUrl?.let { url ->
                    ActionPill(text = "All builds", onClick = { openUrl(context, url) })
                }
                if (source.requiresSignIn) {
                    if (UpdateChecker.isSignedIn) {
                        ActionPill(text = "Sign out", onClick = UpdateChecker::signOut, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ActionPill(text = "Sign in as tester", onClick = UpdateChecker::signIn)
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildPicker(builds: List<AvailableUpdate>, chosen: AvailableUpdate?, onChosen: (AvailableUpdate) -> Unit, enabled: Boolean) {
    val context = LocalContext.current
    val installedCode = remember(context) { appVersionInfo(context)?.versionCode }
    val isDowngrade = chosen?.versionCode?.let { code -> installedCode != null && code < installedCode } == true
    DropdownItem(
        title = "Builds",
        subtitle = when {
            chosen == null -> "Pick a specific version"
            isDowngrade -> "Older than this app: uninstall it first, which clears its data"
            else -> chosen.releaseNotes?.lineSequence()?.firstOrNull()
        },
        options = builds,
        selected = chosen,
        onSelected = onChosen,
        placeholder = "Choose",
        optionLabel = { it.label },
        enabled = enabled,
    )
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    try {
        context.startActivity(if (context is Activity) intent else intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

internal fun updateAvailableColor(isDark: Boolean): Color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)

private fun statusText(status: UpdateStatus, source: UpdateSource): String = when (status) {
    UpdateStatus.Idle -> if (source.requiresSignIn && !UpdateChecker.isSignedIn) "Sign in to check ${source.name}" else "Not checked yet"

    UpdateStatus.Checking -> "Checking ${source.name}…"

    UpdateStatus.UpToDate -> "You're on the latest version · checked ${UpdateChecker.lastCheckedAtMillis?.let(::time).orEmpty()}"

    is UpdateStatus.Available -> "${status.update.label} is available"

    is UpdateStatus.Installing -> status.progress?.let { "Downloading ${status.update.label} · ${(it * 100).toInt()}%" }
        ?: "Preparing ${status.update.label}…"

    is UpdateStatus.Failed -> "Check failed: ${status.message}"
}

private fun time(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
