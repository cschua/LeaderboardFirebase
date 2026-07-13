package com.leaderboardkit.domain.usecase

import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.repository.LeaderboardRepository

class GetUserRankUseCase(
    private val repository: LeaderboardRepository,
) {
    suspend operator fun invoke(userId: String, config: LeaderboardConfig): Result<Int?> =
        repository.getUserRank(userId, config)
}
