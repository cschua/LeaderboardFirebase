package com.leaderboardkit.presentation

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry

/** [top] plus, if the current user isn't already in it, their own [pinnedEntry] row — see [selectWidgetEntries]. */
@InternalLeaderboardKitApi
data class WidgetEntrySelection(
    val top: List<LeaderboardEntry>,
    val pinnedEntry: LeaderboardEntry?,
)

/**
 * The top [topCount] entries plus, if [currentUserId] isn't already among them, a
 * separate pinned row for their own entry — the compact "top N with your own rank
 * pinned below" shape `LeaderboardWidget` renders. Pure function of [LeaderboardState]
 * so it's testable without a Composable/ViewModel in the loop.
 */
@InternalLeaderboardKitApi
fun LeaderboardState.selectWidgetEntries(currentUserId: String, topCount: Int): WidgetEntrySelection {
    val top = entries.take(topCount)
    val pinnedEntry = currentUserEntry?.takeIf { entry -> top.none { it.userId == entry.userId } }
    return WidgetEntrySelection(top, pinnedEntry)
}
