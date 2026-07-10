package com.leaderboardkit.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** Colors used across leaderboard rows, on top of the passed-through Material3 [colorScheme]. */
data class LeaderboardColors(
    val colorScheme: ColorScheme,
    val topThreeHighlight: Color,
    val currentUserRowHighlight: Color,
)

data class LeaderboardTypography(
    val displayName: TextStyle,
    val score: TextStyle,
    val rank: TextStyle,
)

/**
 * Every visual knob [com.leaderboardkit.ui.screen.LeaderboardScreen] and its row
 * composables read from. Never construct this directly — use
 * [rememberLeaderboardTheme], which derives every default from the ambient
 * Material3 [ColorScheme]/[MaterialTheme.typography] so dark/light mode and any
 * app-level `MaterialTheme` customization are honored automatically; every field
 * is still individually overridable.
 */
data class LeaderboardTheme(
    val colors: LeaderboardColors,
    val typography: LeaderboardTypography,
    val rowHeight: Dp = 64.dp,
    val avatarShape: AvatarShape = AvatarShape.Circle,
    val rankBadgeStyle: RankBadgeStyle = RankBadgeStyle.Numeric,
    /** Placement spec for a row's animated position change when ranks shuffle (drives `Modifier.animateItem`). */
    val rankChangeAnimationSpec: FiniteAnimationSpec<IntOffset> = spring(),
)

@Composable
fun rememberLeaderboardTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    topThreeHighlight: Color = colorScheme.tertiaryContainer,
    currentUserRowHighlight: Color = colorScheme.primaryContainer,
    displayNameStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    scoreStyle: TextStyle = MaterialTheme.typography.titleMedium,
    rankStyle: TextStyle = MaterialTheme.typography.labelLarge,
    rowHeight: Dp = 64.dp,
    avatarShape: AvatarShape = AvatarShape.Circle,
    rankBadgeStyle: RankBadgeStyle = RankBadgeStyle.Numeric,
    rankChangeAnimationSpec: FiniteAnimationSpec<IntOffset> = spring(),
): LeaderboardTheme = remember(
    colorScheme,
    topThreeHighlight,
    currentUserRowHighlight,
    displayNameStyle,
    scoreStyle,
    rankStyle,
    rowHeight,
    avatarShape,
    rankBadgeStyle,
    rankChangeAnimationSpec,
) {
    LeaderboardTheme(
        colors = LeaderboardColors(
            colorScheme = colorScheme,
            topThreeHighlight = topThreeHighlight,
            currentUserRowHighlight = currentUserRowHighlight,
        ),
        typography = LeaderboardTypography(
            displayName = displayNameStyle,
            score = scoreStyle,
            rank = rankStyle,
        ),
        rowHeight = rowHeight,
        avatarShape = avatarShape,
        rankBadgeStyle = rankBadgeStyle,
        rankChangeAnimationSpec = rankChangeAnimationSpec,
    )
}
