package com.leaderboardkit.presentation

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi

/** One-off UI events that don't belong in persistent [LeaderboardState] (snackbars, scroll requests, ...). */
@InternalLeaderboardKitApi
sealed interface LeaderboardEffect {
    data class ShowError(val message: String) : LeaderboardEffect
    data object ScrollToUserRank : LeaderboardEffect
}
