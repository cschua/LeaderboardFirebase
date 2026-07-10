package com.leaderboardkit.presentation

import com.leaderboardkit.domain.usecase.GetNearbyRanksUseCase
import com.leaderboardkit.domain.usecase.LoadMoreUseCase
import com.leaderboardkit.domain.usecase.ObserveLeaderboardUseCase
import com.leaderboardkit.domain.usecase.SubmitScoreUseCase

/**
 * The Stage-1 use cases [LeaderboardViewModel] needs, bundled so callers don't
 * have to pass four constructor parameters individually.
 *
 * This stands in for proper DI wiring: `:leaderboard:ui`'s facade for obtaining a
 * fully Hilt-injected [LeaderboardViewModel] (so a host app never constructs this
 * by hand) is out of scope for this stage and lands with the public-api facade.
 * Until then, callers (including previews) build one directly from use cases
 * backed by whichever [com.leaderboardkit.domain.repository.LeaderboardRepository]
 * they've wired up.
 */
data class LeaderboardDependencies(
    val observeLeaderboard: ObserveLeaderboardUseCase,
    val loadMore: LoadMoreUseCase,
    val submitScore: SubmitScoreUseCase,
    val getNearbyRanks: GetNearbyRanksUseCase,
)
