package com.leaderboardkit.domain.usecase

import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.repository.LeaderboardRepository
import javax.inject.Inject

class SubmitScoreUseCase @Inject constructor(
    private val repository: LeaderboardRepository,
) {
    suspend operator fun invoke(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any> = emptyMap(),
    ): Result<Unit> {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return repository.submitScore(userId, score, config, metadata)
    }
}
