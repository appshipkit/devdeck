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
import androidx.compose.runtime.mutableStateMapOf
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
import io.devdeck.LocalDeckState
import io.devdeck.components.ButtonItem
import io.devdeck.components.SectionTitle
import io.devdeck.components.TextInputItem
import io.devdeck.components.TextItem
import io.devdeck.internal.DeckIcons
import io.devdeck.internal.DeckSettings

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
public fun DeepLinkLauncher(modifier: Modifier = Modifier, suggestions: List<String> = emptyList(), restrictToApp: Boolean = true) {
    DeepLinkLauncher(modifier, suggestions, restrictToApp, links = emptyList())
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
public fun DeepLinkLauncher(
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    restrictToApp: Boolean = true,
    links: List<DeepLink> = emptyList(),
) {
    DeepLinkLauncher(modifier, suggestions, restrictToApp, links, prefix = null)
}

/**
 * A field to open any deep link, plus the app's saved [links] by name (their URIs stay out of sight) and the last
 * few opened. Typing in the field also filters the saved links by name, group, description or URI.
 * Links without a scheme get [prefix] (`product/123` opens `myapp://product/123` with prefix `myapp://`), and saved
 * links with `{placeholders}` ask for their values first.
 */
@Composable
public fun DeepLinkLauncher(
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    restrictToApp: Boolean = true,
    links: List<DeepLink> = emptyList(),
    prefix: String? = null,
) {
    val context = LocalContext.current
    var uri by rememberSaveable { mutableStateOf("") }
    var recent by remember { mutableStateOf(DeepLinkHistory.load(context)) }
    val deckState = LocalDeckState.current
    var filling by remember { mutableStateOf<DeepLink?>(null) }
    val open: (String) -> Unit = { typed ->
        val link = DeepLinkUris.withPrefix(typed, prefix)
        if (launchDeepLink(context, link, restrictToApp)) recent = DeepLinkHistory.add(context, link)
    }
    // Inside the drawer the form gets its own page; elsewhere it opens above the list.
    val openSaved: (DeepLink) -> Unit = { link ->
        when {
            !link.needsInput -> open(link.uri)
            deckState != null -> deckState.navigateTo(link.name) { DeepLinkInputs(link, onOpen = open) }
            else -> filling = link
        }
    }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = uri,
            onValueChange = { uri = it },
            placeholder = {
                Text(
                    when {
                        links.isNotEmpty() -> "Deep link, or search saved links"
                        !prefix.isNullOrBlank() -> "product/123"
                        else -> "myapp://product/123"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
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
        filling?.let { link ->
            DeepLinkInputs(link, onOpen = {
                open(it)
                filling = null
            })
        }
        val query = uri.trim()
        val shown = remember(links, query) { if (query.isEmpty()) links else links.filter { it.matches(query) } }
        shown.groupBy { it.group ?: SAVED_TITLE }.forEach { (group, groupLinks) ->
            SectionTitle(group)
            groupLinks.forEach { link -> ButtonItem(title = link.name, subtitle = link.description, onClick = { openSaved(link) }) }
        }
        if (links.isNotEmpty() && query.isNotEmpty() && shown.isEmpty()) TextItem("No saved links match \"$query\"")
        if (suggestions.isNotEmpty()) {
            SectionTitle("Suggested")
            suggestions.forEach { link ->
                ButtonItem(title = link, onClick = {
                    uri = link
                    open(link)
                })
            }
        }
        if (recent.isNotEmpty()) {
            SectionTitle("Recent")
            recent.forEach { link ->
                // A saved link shows by its name here too.
                val saved = links.firstOrNull { it.uri == link }
                ButtonItem(title = saved?.name ?: link, onClick = {
                    uri = link
                    open(link)
                })
            }
        }
    }
}

/** Asks for a saved link's values, remembering the last ones used, and shows the link it will open. */
@Composable
private fun DeepLinkInputs(link: DeepLink, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val fields = remember(link) { link.fields }
    val values = remember(link) {
        mutableStateMapOf<String, String>().apply { fields.forEach { put(it.key, DeepLinkValues.load(context, link, it) ?: it.default) } }
    }
    val missing = fields.filter { it.required && values[it.key].isNullOrBlank() }
    Column(Modifier.fillMaxWidth()) {
        link.description?.let { TextItem(it) }
        fields.forEach { field ->
            TextInputItem(
                title = field.label,
                value = values[field.key].orEmpty(),
                commitOnEveryChange = true,
                subtitle = field.hint ?: if (field.required) null else "Optional",
                onValueCommit = { values[field.key] = it },
            )
        }
        val target = DeepLinkUris.fill(link.uri, values)
        TextItem("Opens $target")
        ButtonItem(
            title = "Open ${link.name}",
            subtitle = if (missing.isEmpty()) null else "Needs ${missing.joinToString { it.label }}",
            enabled = missing.isEmpty(),
            onClick = {
                fields.forEach { DeepLinkValues.save(context, link, it, values[it.key].orEmpty()) }
                onOpen(target)
            },
        )
    }
}

private object DeepLinkValues {
    private fun key(link: DeepLink, field: DeepLinkInput) = "deeplinks.value.${link.name}.${field.key}"

    fun load(context: Context, link: DeepLink, field: DeepLinkInput): String? = DeckSettings.of(context).getString(key(link, field), null)

    fun save(context: Context, link: DeepLink, field: DeepLinkInput, value: String) {
        DeckSettings.of(context).edit { putString(key(link, field), value) }
    }
}

private const val SAVED_TITLE = "Saved links"

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
