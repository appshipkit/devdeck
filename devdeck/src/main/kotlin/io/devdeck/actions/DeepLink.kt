package io.devdeck.actions

import androidx.compose.runtime.Immutable

/**
 * A deep link testers can open by name, without typing or even seeing the URI.
 * Links with the same [group] are listed together; [description] shows under the name.
 *
 * ```
 * DeepLink("Product page", "myapp://product/123", group = "Shop")
 * ```
 */
@Immutable
public class DeepLink(
    public val name: String,
    public val uri: String,
    public val group: String? = null,
    public val description: String? = null,
) {
    internal fun matches(query: String): Boolean =
        listOfNotNull(name, uri, group, description).any { it.contains(query, ignoreCase = true) }
}
