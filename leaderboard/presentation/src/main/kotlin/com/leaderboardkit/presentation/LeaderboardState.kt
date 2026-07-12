package com.leaderboardkit.presentation

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry

/**
 * Single immutable snapshot the UI layer renders from. There is exactly one of
 * these live per [LeaderboardViewModel] instance, held in a `MutableStateFlow`.
 *
 * [currentUserEntry] is populated independently of [entries]: if the current user
 * is within the loaded window it mirrors that entry, but if they're off-screen
 * (e.g. rank 4,502 on a board that only loads the top 25) it's resolved via a
 * separate surrounding-ranks lookup — see [LeaderboardViewModel] KDoc. It stays
 * `null` only while that resolution is genuinely unknown/pending, not merely
 * because the user is off-screen.
 */
@InternalLeaderboardKitApi
data class LeaderboardState(
    val entries: List<LeaderboardEntry> = emptyList(),
    val currentUserEntry: LeaderboardEntry? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: LeaderboardError? = null,
    val config: LeaderboardConfig,
)
