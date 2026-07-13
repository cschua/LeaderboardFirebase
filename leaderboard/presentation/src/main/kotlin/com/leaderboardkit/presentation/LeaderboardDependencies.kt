package com.leaderboardkit.presentation

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.usecase.GetNearbyRanksUseCase
import com.leaderboardkit.domain.usecase.LoadMoreUseCase
import com.leaderboardkit.domain.usecase.ObserveLeaderboardUseCase
import com.leaderboardkit.domain.usecase.SubmitScoreToWindowsUseCase
import com.leaderboardkit.domain.usecase.SubmitScoreUseCase

/**
 * The Stage-1 use cases [LeaderboardViewModel] needs, bundled so callers don't
 * have to pass four constructor parameters individually.
 *
 * Most host apps never construct this directly: `:leaderboard:public-api`'s
 * `createLeaderboardClient` builds one from a `LeaderboardKitConfig` and holds it
 * behind its `LeaderboardClient`. Callers that skip that facade — advanced
 * integrations, previews, tests — build one directly from use cases backed by
 * whichever [com.leaderboardkit.domain.repository.LeaderboardRepository] they've
 * wired up.
 */
@InternalLeaderboardKitApi
data class LeaderboardDependencies(
    val observeLeaderboard: ObserveLeaderboardUseCase,
    val loadMore: LoadMoreUseCase,
    val submitScore: SubmitScoreUseCase,
    val getNearbyRanks: GetNearbyRanksUseCase,
    val submitScoreToWindows: SubmitScoreToWindowsUseCase = SubmitScoreToWindowsUseCase(submitScore),
)
