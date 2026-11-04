package io.devdeck

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.devdeck.internal.DeckIcons

/**
 * How the drawer looks. [titleIcon] is drawn in the tile next to the title, tinted for that tile (use `Icon` so it
 * picks up the colour); pass `null` to show the title without an icon.
 */
@Immutable
public data class DeckStyle(
    val title: String = "Debug menu",
    val subtitle: String? = null,
    val maxWidth: Dp = 360.dp,
    val widthFraction: Float = 0.85f,
    val scrimColor: Color = Color.Black.copy(alpha = 0.4f),
    val containerColor: Color = Color.Unspecified,
    val cardColor: Color = Color.Unspecified,
    val lightColorScheme: ColorScheme? = null,
    val darkColorScheme: ColorScheme? = null,
    val animationDurationMillis: Int = 250,
    val collapsibleSections: Boolean = true,
    val oneSectionAtATime: Boolean = false,
    val titleIcon: (@Composable () -> Unit)? = DefaultTitleIcon,
) {

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        title: String = "Debug menu",
        subtitle: String? = null,
        maxWidth: Dp = 360.dp,
        widthFraction: Float = 0.85f,
        scrimColor: Color = Color.Black.copy(alpha = 0.4f),
        containerColor: Color = Color.Unspecified,
        cardColor: Color = Color.Unspecified,
        lightColorScheme: ColorScheme? = null,
        darkColorScheme: ColorScheme? = null,
        animationDurationMillis: Int = 250,
        collapsibleSections: Boolean = true,
        oneSectionAtATime: Boolean = false,
    ) : this(
        title, subtitle, maxWidth, widthFraction, scrimColor, containerColor, cardColor, lightColorScheme, darkColorScheme,
        animationDurationMillis, collapsibleSections, oneSectionAtATime, DefaultTitleIcon,
    )

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        title: String = this.title,
        subtitle: String? = this.subtitle,
        maxWidth: Dp = this.maxWidth,
        widthFraction: Float = this.widthFraction,
        scrimColor: Color = this.scrimColor,
        containerColor: Color = this.containerColor,
        cardColor: Color = this.cardColor,
        lightColorScheme: ColorScheme? = this.lightColorScheme,
        darkColorScheme: ColorScheme? = this.darkColorScheme,
        animationDurationMillis: Int = this.animationDurationMillis,
        collapsibleSections: Boolean = this.collapsibleSections,
        oneSectionAtATime: Boolean = this.oneSectionAtATime,
    ): DeckStyle = copy(
        title = title,
        subtitle = subtitle,
        maxWidth = maxWidth,
        widthFraction = widthFraction,
        scrimColor = scrimColor,
        containerColor = containerColor,
        cardColor = cardColor,
        lightColorScheme = lightColorScheme,
        darkColorScheme = darkColorScheme,
        animationDurationMillis = animationDurationMillis,
        collapsibleSections = collapsibleSections,
        oneSectionAtATime = oneSectionAtATime,
        titleIcon = titleIcon,
    )
}

private val DefaultTitleIcon: @Composable () -> Unit = { Icon(DeckIcons.BugReport, contentDescription = null) }
