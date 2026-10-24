package io.devdeck.plugin

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.devdeck.components.PageLinkItem
import io.devdeck.console.ConsoleLevel
import io.devdeck.console.ConsoleView
import io.devdeck.console.ConsoleViewer
import io.devdeck.logcat.LogcatNoise
import io.devdeck.logcat.LogcatReader

public class LogcatPlugin(
    private val title: String = "Logcat",
    private val startOnLaunch: Boolean = true,
    private val initialView: ConsoleView = ConsoleView.All,
    private val ignoredTags: Set<String> = LogcatNoise.PLATFORM_TAGS,
    private val ignoredTagPrefixes: Set<String> = LogcatNoise.PLATFORM_TAG_PREFIXES,
    private val ignoredMessagePrefixes: Set<String> = LogcatNoise.PLATFORM_MESSAGE_PREFIXES,
    override val section: String? = "Logs 📚",
) : DeckPlugin {

    override val id: String = "logcat"

    override fun onAttach(context: Context) {
        LogcatReader.ignoredTags = ignoredTags
        LogcatReader.ignoredTagPrefixes = ignoredTagPrefixes
        LogcatReader.ignoredMessagePrefixes = ignoredMessagePrefixes
        if (startOnLaunch) LogcatReader.start()
    }

    @Composable
    override fun Content() {
        val lines by LogcatReader.buffer.lines.collectAsState()
        val errors = lines.count { it.level >= ConsoleLevel.Error }
        PageLinkItem(
            title = title,
            subtitle = if (lines.isEmpty()) "App process logs" else "${lines.size} lines · $errors errors",
        ) {
            LaunchedEffect(Unit) { LogcatReader.start() }
            ConsoleViewer(LogcatReader.buffer, title = title, initialView = initialView)
        }
    }
}
