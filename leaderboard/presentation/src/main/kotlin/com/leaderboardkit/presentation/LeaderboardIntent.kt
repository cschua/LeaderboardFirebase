package com.leaderboardkit.presentation

import com.leaderboardkit.domain.model.LeaderboardScope
import com.leaderboardkit.domain.model.TimeWindow

/** User/UI-originated actions. Sent via [LeaderboardViewModel.onIntent] — never handled directly by composables. */
sealed interface LeaderboardIntent {
    data object Refresh : LeaderboardIntent
    data object LoadMore : LeaderboardIntent
    data class ChangeTimeWindow(val window: TimeWindow) : LeaderboardIntent
    data class ChangeScope(val scope: LeaderboardScope) : LeaderboardIntent
    data class SubmitScore(val score: Long) : LeaderboardIntent
}
