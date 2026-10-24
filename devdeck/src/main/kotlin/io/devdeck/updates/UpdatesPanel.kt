package io.devdeck.updates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.devdeck.LocalDeckColors
import io.devdeck.components.ActionPill
import io.devdeck.components.ActionPillRow
import io.devdeck.components.HorizontalPadding
import io.devdeck.components.TitleAndSubtitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
public fun UpdatesPanel(modifier: Modifier = Modifier, title: String = "Test build updates") {
    val source = UpdateChecker.source ?: return
    val status = UpdateChecker.status
    val update = UpdateChecker.availableUpdate
    val success = updateAvailableColor(LocalDeckColors.current?.isDark == true)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = HorizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TitleAndSubtitle(title, statusText(status, source), enabled = true, modifier = Modifier.weight(1f))
            if (status == UpdateStatus.Checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
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
        ActionPillRow {
            if (update != null) {
                ActionPill(
                    text = "Install ${update.label}",
                    onClick = UpdateChecker::install,
                    color = success,
                    enabled = status !is UpdateStatus.Installing,
                )
            }
            ActionPill(
                text = "Check now",
                onClick = UpdateChecker::check,
                enabled = status != UpdateStatus.Checking && status !is UpdateStatus.Installing,
            )
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
