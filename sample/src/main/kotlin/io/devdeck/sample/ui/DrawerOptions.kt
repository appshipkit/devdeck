package io.devdeck.sample.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import io.devdeck.DeckGestures
import io.devdeck.DeckShortcut
import io.devdeck.DeckSide
import io.devdeck.DeckStyle
import io.devdeck.DeckTrigger
import io.devdeck.sample.R

@Stable
class DrawerOptions {
    var enabled by mutableStateOf(true)
    var twoFingerSwipe by mutableStateOf(true)
    var keyboardShortcut by mutableStateOf(true)
    var floatingButton by mutableStateOf(true)
    var customIcon by mutableStateOf(false)
    var titleIcon by mutableStateOf(TitleIcon.Default)
    var side by mutableStateOf(DeckSide.End)
    var oneSectionAtATime by mutableStateOf(false)

    val gestures: DeckGestures
        get() = DeckGestures(
            twoFingerSwipe = twoFingerSwipe,
            keyboardShortcut = if (keyboardShortcut) DeckShortcut() else null,
        )

    val trigger: DeckTrigger
        get() = when {
            !floatingButton -> DeckTrigger.None
            customIcon -> customIconTrigger
            else -> DeckTrigger.FloatingButton()
        }
}

enum class TitleIcon { Default, Custom, None }

/** The drawer style with the sample's title icon choice applied. */
fun DrawerOptions.style(base: DeckStyle): DeckStyle = base.copy(
    oneSectionAtATime = oneSectionAtATime,
    titleIcon = when (titleIcon) {
        TitleIcon.Default -> base.titleIcon
        TitleIcon.Custom -> wrenchIcon
        TitleIcon.None -> null
    },
)

private val wrenchIcon: @Composable () -> Unit = { Icon(painterResource(R.drawable.ic_wrench), contentDescription = null) }

// Any composable works as the icon, here a drawable from the app's own resources.
private val customIconTrigger = DeckTrigger.FloatingButton(
    icon = { Icon(painterResource(R.drawable.ic_wrench), contentDescription = "Open debug menu") },
)

@Composable
fun rememberDrawerOptions(): DrawerOptions = remember { DrawerOptions() }
