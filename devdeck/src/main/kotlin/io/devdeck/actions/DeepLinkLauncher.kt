package io.devdeck.actions

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import io.devdeck.components.BulletItem
import io.devdeck.components.SectionTitle
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.DeckSettings

@Composable
public fun DeepLinkLauncher(modifier: Modifier = Modifier, suggestions: List<String> = emptyList(), restrictToApp: Boolean = true) {
    val context = LocalContext.current
    var uri by rememberSaveable { mutableStateOf("") }
    var recent by remember { mutableStateOf(DeepLinkHistory.load(context)) }
    val open: (String) -> Unit = { link ->
        if (launchDeepLink(context, link, restrictToApp)) recent = DeepLinkHistory.add(context, link)
    }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = uri,
            onValueChange = { uri = it },
            placeholder = { Text("myapp://product/123", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (uri.isNotBlank()) open(uri.trim()) }),
            trailingIcon = {
                IconButton(onClick = { open(uri.trim()) }, enabled = uri.isNotBlank()) {
                    Icon(
                        DeckIcons.Send,
                        contentDescription = "Open deep link",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = if (uri.isBlank()) 0.38f else 1f),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        )
        if (suggestions.isNotEmpty()) {
            SectionTitle("Suggested")
            suggestions.forEach { link ->
                BulletItem(link, onClick = {
                    uri = link
                    open(link)
                })
            }
        }
        if (recent.isNotEmpty()) {
            SectionTitle("Recent")
            recent.forEach { link ->
                BulletItem(link, onClick = {
                    uri = link
                    open(link)
                })
            }
        }
    }
}

private fun launchDeepLink(context: Context, link: String, restrictToApp: Boolean): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, link.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (restrictToApp) intent.setPackage(context.packageName)
    return runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "Nothing handles $link", Toast.LENGTH_SHORT).show() }
        .isSuccess
}

private object DeepLinkHistory {
    private const val KEY = "deeplinks.recent"
    private const val MAX = 10

    fun load(context: Context): List<String> = DeckSettings.of(context).getString(KEY, null)
        ?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    fun add(context: Context, link: String): List<String> {
        val updated = (listOf(link) + load(context).filterNot { it == link }).take(MAX)
        DeckSettings.of(context).edit { putString(KEY, updated.joinToString("\n")) }
        return updated
    }
}
