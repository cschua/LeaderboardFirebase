package com.leaderboardkit.domain.usecase

import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.repository.LeaderboardRepository

class GetNearbyRanksUseCase(
    private val repository: LeaderboardRepository,
) {
    suspend operator fun invoke(
        userId: String,
        radius: Int,
        config: LeaderboardConfig,
    ): Result<List<LeaderboardEntry>> {
        require(radius > 0) { "radius must be > 0, was $radius" }
        return repository.getSurroundingEntries(userId, radius, config)
    }
}
