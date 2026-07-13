package com.leaderboardkit.domain.usecase

import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.Flow

class ObserveLeaderboardUseCase(
    private val repository: LeaderboardRepository,
) {
    operator fun invoke(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> =
        repository.observeEntries(config)
}
