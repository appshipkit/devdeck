package io.devdeck.network

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public class NetworkHighlight(public val label: String, public val color: Color, public val matches: (NetworkCall) -> Boolean) {
    public companion object {
        public fun operation(label: String, color: Color, vararg operationNames: String): NetworkHighlight =
            NetworkHighlight(label, color) { call -> call.operationName != null && call.operationName in operationNames }

        public fun urlContains(label: String, color: Color, fragment: String): NetworkHighlight =
            NetworkHighlight(label, color) { call -> call.url.contains(fragment, ignoreCase = true) }

        public fun graphQl(label: String = "GraphQL", color: Color = Color(0xFFE535AB)): NetworkHighlight =
            NetworkHighlight(label, color) { call -> call.operationName != null }
    }
}
