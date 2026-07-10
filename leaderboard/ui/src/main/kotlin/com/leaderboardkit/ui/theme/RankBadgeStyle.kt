package com.leaderboardkit.ui.theme

import androidx.compose.runtime.Composable

/** How [com.leaderboardkit.ui.component.RankBadge] renders an entry's rank. */
sealed interface RankBadgeStyle {
    /** Plain rank number (or "-" if unranked). Always the fallback for ranks below 4th under [MedalIcon]. */
    data object Numeric : RankBadgeStyle

    /** Gold/silver/bronze medal for ranks 1-3, numeric for everything else. */
    data object MedalIcon : RankBadgeStyle

    /** Full override — receives the raw rank (`null` if unranked) and renders whatever the host app wants. */
    data class Custom(val content: @Composable (rank: Int?) -> Unit) : RankBadgeStyle
}
