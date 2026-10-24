package io.devdeck.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.devdeck.components.BulletItem
import io.devdeck.components.CodeBlock
import io.devdeck.components.CodeLanguage
import io.devdeck.components.ExpandableItem
import io.devdeck.components.InfoGroup
import io.devdeck.components.Pill
import io.devdeck.components.SectionDivider
import io.devdeck.components.TextItem
import io.devdeck.internal.copyToClipboard
import io.devdeck.internal.shareFiles

@Composable
internal fun NetworkCallDetail(callId: Long, modifier: Modifier = Modifier, highlights: List<NetworkHighlight> = emptyList()) {
    val calls by NetworkLogger.calls.collectAsState()
    val call = remember(calls, callId) { calls.firstOrNull { it.id == callId } }
    if (call == null) {
        TextItem("This request is no longer in the log")
        return
    }
    val context = LocalContext.current
    val matched = remember(call, highlights) { highlights.filter { it.matches(call) } }

    Column(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(call.method, methodColor(call.method))
                Pill(NetworkFormat.status(call), statusColor(call))
                if (call.simulation != null) Pill("Simulated", SimulatedColor, filled = false)
                matched.forEach { Pill(it.label, it.color, filled = false) }
                Text(NetworkFormat.duration(call.durationMillis), style = MaterialTheme.typography.bodySmall)
            }
            Text(call.url, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                TextButton(onClick = { context.copyToClipboard("URL", call.url) }) { Text("Copy URL") }
                TextButton(onClick = { context.copyToClipboard("cURL", NetworkFormat.curl(call)) }) { Text("Copy cURL") }
                TextButton(onClick = { context.shareFiles(emptyList(), text = NetworkFormat.summary(call), subject = call.displayName) }) {
                    Text("Share")
                }
            }
        }
        SectionDivider()

        ExpandableItem("Overview", initiallyExpanded = true) {
            InfoGroup {
                call.operationName?.let { BulletItem("Operation", it) }
                call.simulation?.let { BulletItem("Simulated by DevDeck", it) }
                BulletItem("Method", call.method)
                BulletItem(
                    "Status",
                    listOfNotNull(
                        NetworkFormat.status(call),
                        call.responseMessage?.takeIf {
                            it.isNotBlank()
                        },
                    ).joinToString(" "),
                )
                BulletItem("Host", call.host)
                BulletItem("Path", call.path)
                call.protocol?.let { BulletItem("Protocol", it) }
                BulletItem("Started", NetworkFormat.dateTime(call.startedAtMillis))
                BulletItem("Duration", NetworkFormat.duration(call.durationMillis))
                BulletItem("Request size", NetworkFormat.size(call.requestSize))
                BulletItem("Response size", NetworkFormat.size(call.responseSize))
            }
        }
        call.error?.let { error ->
            ExpandableItem("Error", initiallyExpanded = true) { CodeBlock(error, label = "Error") }
        }

        val params = call.queryParameters
        if (params.isNotEmpty()) {
            ExpandableItem("Query parameters", subtitle = "${params.size}") {
                params.forEach { (name, value) -> BulletItem(name, value) }
            }
        }
        HeadersSection("Request headers", call.requestHeaders)
        BodySection("Request body", call.requestBody, call.requestContentType)

        SectionDivider()
        HeadersSection("Response headers", call.responseHeaders)
        BodySection("Response body", call.responseBody, call.responseContentType, initiallyExpanded = true)
    }
}

@Composable
private fun HeadersSection(title: String, headers: List<Pair<String, String>>) {
    ExpandableItem(title, subtitle = if (headers.isEmpty()) "None" else "${headers.size}") {
        headers.forEach { (name, value) -> BulletItem(name, value) }
    }
}

@Composable
private fun BodySection(title: String, body: String?, contentType: String?, initiallyExpanded: Boolean = false) {
    val isJson = remember(body, contentType) { NetworkFormat.isJson(contentType, body) }
    ExpandableItem(
        title = title,
        subtitle = when {
            body == null -> "None"
            else -> listOfNotNull(contentType?.substringBefore(';'), "${body.length} chars").joinToString(" · ")
        },
        initiallyExpanded = initiallyExpanded && body != null,
    ) {
        if (body != null) {
            CodeBlock(
                text = body,
                language = if (isJson) CodeLanguage.Json else CodeLanguage.Plain,
                label = title,
                collapsedLines = 30,
            )
        }
    }
}
