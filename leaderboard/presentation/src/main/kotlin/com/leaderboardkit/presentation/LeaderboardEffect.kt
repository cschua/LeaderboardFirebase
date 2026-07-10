package com.leaderboardkit.presentation

/** One-off UI events that don't belong in persistent [LeaderboardState] (snackbars, scroll requests, ...). */
sealed interface LeaderboardEffect {
    data class ShowError(val message: String) : LeaderboardEffect
    data object ScrollToUserRank : LeaderboardEffect
}
