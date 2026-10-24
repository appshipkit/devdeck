package io.devdeck.bugreport

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.devdeck.LocalDeckState
import io.devdeck.components.ButtonItem

@Composable
public fun BugReportButton(
    modifier: Modifier = Modifier,
    title: String = "Generate a bug report",
    buildInfo: () -> Map<String, String> = { emptyMap() },
) {
    val drawerState = LocalDeckState.current
    ButtonItem(
        title = title,
        modifier = modifier,
        onClick = { drawerState?.navigateTo("Bug report") { BugReportScreen(buildInfo = buildInfo) } },
    )
}
