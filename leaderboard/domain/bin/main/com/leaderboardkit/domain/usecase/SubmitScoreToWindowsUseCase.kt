package com.leaderboardkit.domain.usecase

import com.leaderboardkit.domain.model.LeaderboardConfig
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fans one score out across several [LeaderboardConfig] views of what is
 * conceptually a single board — e.g. separate Weekly/Monthly/All-Time
 * time-window buckets that all read/write the same [LeaderboardConfig.boardId] —
 * so the submission lands in every window regardless of which one the caller
 * happens to be looking at afterward.
 *
 * [staggerDelay] is paced between writes (never after the last one) because
 * client-side rate limiting (`ClientRateLimiter` in `:leaderboard:data`) is
 * keyed by `(userId, boardId)`, not per time-window bucket — back-to-back
 * writes to the same board would otherwise trip the very next call's cooldown.
 *
 * Stops and returns on the first failure, leaving any remaining [LeaderboardConfig]s
 * unsubmitted.
 */
class SubmitScoreToWindowsUseCase(
    private val submitScore: SubmitScoreUseCase,
    private val staggerDelay: Duration = 300.milliseconds,
) {
    suspend operator fun invoke(
        userId: String,
        score: Long,
        configs: List<LeaderboardConfig>,
        metadata: Map<String, Any> = emptyMap(),
    ): Result<Unit> {
        configs.forEachIndexed { index, config ->
            val result = submitScore(userId, score, config, metadata)
            if (result.isFailure) return result
            if (index != configs.lastIndex) delay(staggerDelay)
        }
        return Result.success(Unit)
    }
}
