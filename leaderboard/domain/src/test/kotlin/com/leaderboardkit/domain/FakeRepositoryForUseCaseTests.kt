package com.leaderboardkit.domain

import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Minimal hand-written [LeaderboardRepository] double for use-case tests. A
 * hand-written fake (rather than a mocking library) keeps `:leaderboard:domain`'s
 * test dependencies as small as its main dependencies.
 */
class RecordingLeaderboardRepository(
    private val entries: List<LeaderboardEntry> = emptyList(),
    private val userRank: Result<Int?> = Result.success(null),
    private val nearbyEntries: Result<List<LeaderboardEntry>> = Result.success(emptyList()),
    private val submitResult: Result<Unit> = Result.success(Unit),
    private val loadMoreResult: Result<Boolean> = Result.success(false),
) : LeaderboardRepository {

    var lastObservedConfig: LeaderboardConfig? = null
        private set
    var lastSubmit: Triple<String, Long, Map<String, Any>>? = null
        private set
    var lastRankQuery: Pair<String, LeaderboardConfig>? = null
        private set
    var lastNearbyQuery: Triple<String, Int, LeaderboardConfig>? = null
        private set

    override fun observeEntries(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> {
        lastObservedConfig = config
        return flowOf(entries)
    }

    override suspend fun loadMore(config: LeaderboardConfig): Result<Boolean> = loadMoreResult

    override suspend fun submitScore(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any>,
    ): Result<Unit> {
        lastSubmit = Triple(userId, score, metadata)
        return submitResult
    }

    override suspend fun getUserRank(userId: String, config: LeaderboardConfig): Result<Int?> {
        lastRankQuery = userId to config
        return userRank
    }

    override suspend fun getSurroundingEntries(
        userId: String,
        radius: Int,
        config: LeaderboardConfig,
    ): Result<List<LeaderboardEntry>> {
        lastNearbyQuery = Triple(userId, radius, config)
        return nearbyEntries
    }
}

fun testConfig(boardId: String = "test_board") = com.leaderboardkit.domain.model.leaderboardConfig(boardId) {}
