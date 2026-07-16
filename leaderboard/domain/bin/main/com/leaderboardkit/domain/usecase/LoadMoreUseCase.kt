package com.leaderboardkit.domain.usecase

import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.repository.LeaderboardRepository

/** @return `true` if a new page was appended, `false` if the board's end was reached. */
class LoadMoreUseCase(
    private val repository: LeaderboardRepository,
) {
    suspend operator fun invoke(config: LeaderboardConfig): Result<Boolean> = repository.loadMore(config)
}
