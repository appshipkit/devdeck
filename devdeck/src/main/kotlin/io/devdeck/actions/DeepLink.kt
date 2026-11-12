package io.devdeck.actions

import androidx.compose.runtime.Immutable
import java.net.URLEncoder

/**
 * A deep link testers can open by name, without typing or even seeing the URI.
 * Links with the same [group] are listed together; [description] shows under the name.
 *
 * Parts written as `{name}` in [uri] are asked for before the link opens, such as a product ID or a query value:
 *
 * ```
 * DeepLink("Product page", "myapp://product/{productId}?size={size}", group = "Shop",
 *     inputs = listOf(DeepLinkInput("productId", default = "123"), DeepLinkInput("size", required = false)))
 * ```
 *
 * [inputs] add a label, default or hint to those parts; a `{name}` without one is still asked for, as required.
 * A URI without a scheme (`product/123`) gets the launcher's prefix, such as `myapp://`.
 */
@Immutable
public class DeepLink(
    public val name: String,
    public val uri: String,
    public val group: String? = null,
    public val description: String? = null,
    public val inputs: List<DeepLinkInput> = emptyList(),
) {

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(name: String, uri: String, group: String? = null, description: String? = null) :
        this(name, uri, group, description, emptyList())

    /** Every value to ask for, in the order the URI uses them. */
    internal val fields: List<DeepLinkInput>
        get() = DeepLinkUris.placeholders(uri).map { key -> inputs.firstOrNull { it.key == key } ?: DeepLinkInput(key) }

    internal val needsInput: Boolean get() = DeepLinkUris.placeholders(uri).isNotEmpty()

    internal fun matches(query: String): Boolean = listOfNotNull(name, uri, group, description).any {
        it.contains(query, ignoreCase = true)
    }
}

/**
 * A value a [DeepLink] asks for before it opens, filling `{key}` in its URI. An empty optional value drops its
 * `key={key}` query parameter, so `?size={size}` disappears when no size is given.
 */
@Immutable
public class DeepLinkInput(
    public val key: String,
    public val label: String = key,
    public val default: String = "",
    public val hint: String? = null,
    public val required: Boolean = true,
)

/** Builds the URI a deep link opens: fills placeholders and adds the prefix to links without a scheme. */
internal object DeepLinkUris {

    private val PLACEHOLDER = Regex("\\{([A-Za-z0-9_]+)\\}")
    private val SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

    fun placeholders(uri: String): List<String> = PLACEHOLDER.findAll(uri).map { it.groupValues[1] }.distinct().toList()

    /** `product/1` with prefix `myapp://` becomes `myapp://product/1`; links that already have a scheme are kept. */
    fun withPrefix(link: String, prefix: String?): String {
        if (prefix.isNullOrBlank() || link.isBlank() || SCHEME.containsMatchIn(link)) return link
        val path = link.trimStart('/')
        return if (prefix.endsWith("/")) prefix + path else "$prefix/$path"
    }

    /** Fills each `{key}` with its URL-encoded value; empty values drop their `key={key}` query parameter. */
    fun fill(uri: String, values: Map<String, String>): String {
        var result = uri
        placeholders(uri).filter { values[it].isNullOrEmpty() }.forEach { key ->
            result = result.replace(Regex("[?&][^?&#=]*=\\{$key\\}"), "")
        }
        // Dropping the first parameter can leave `&` where `?` belongs.
        if ('?' in uri && '?' !in result) result = result.replaceFirst('&', '?')
        result = result.replace(PLACEHOLDER) { match -> encode(values[match.groupValues[1]].orEmpty()) }
        return result.removeSuffix("?")
    }

    // Slashes stay as they are, so a value can be a path such as `sneakers/nike`.
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20").replace("%2F", "/")
}
