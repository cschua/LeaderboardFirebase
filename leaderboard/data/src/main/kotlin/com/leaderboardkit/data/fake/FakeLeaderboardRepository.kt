package com.leaderboardkit.data.fake

import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.SortDirection
import com.leaderboardkit.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Deterministic in-memory [LeaderboardRepository] for tests and Compose previews.
 * Ignores [LeaderboardConfig.refreshStrategy] entirely (no listeners, no polling,
 * no timers) — every read is a synchronous snapshot of [setEntries], which is what
 * makes it safe to use in previews and non-flaky in tests.
 *
 * One instance is meant to back a single board at a time; it does not partition
 * data by [LeaderboardConfig.boardId] the way the real backends do.
 */
class FakeLeaderboardRepository(initialEntries: List<LeaderboardEntry> = emptyList()) : LeaderboardRepository {

    private val allEntries = MutableStateFlow(initialEntries)
    private var loadedCount: Int? = null
    private val rankOverrides = mutableMapOf<String, Int>()
    private val mutex = Mutex()

    /** Replaces the full backing dataset and resets pagination back to one page. */
    fun setEntries(entries: List<LeaderboardEntry>) {
        allEntries.value = entries
        loadedCount = null
    }

    /**
     * Forces [getUserRank]/[getSurroundingEntries] to report [rank] for [userId]
     * regardless of natural sort order — useful for previewing "your rank" UI
     * states (e.g. a very large rank) without constructing thousands of fixture
     * entries. Pass `null` to remove the override.
     */
    fun overrideUserRank(userId: String, rank: Int?) {
        if (rank == null) rankOverrides.remove(userId) else rankOverrides[userId] = rank
    }

    private fun rankedEntries(config: LeaderboardConfig, source: List<LeaderboardEntry> = allEntries.value): List<LeaderboardEntry> {
        val comparator = when (config.sortDirection) {
            SortDirection.Descending -> compareByDescending<LeaderboardEntry> { it.score }
            SortDirection.Ascending -> compareBy { it.score }
        }
        return source.sortedWith(comparator).mapIndexed { index, entry ->
            entry.copy(rank = rankOverrides[entry.userId] ?: (index + 1))
        }
    }

    override fun observeEntries(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> =
        allEntries.map { current ->
            val window = loadedCount ?: config.pageSize.also { loadedCount = it }
            rankedEntries(config, current).take(window.coerceAtMost(current.size))
        }

    override suspend fun loadMore(config: LeaderboardConfig): Result<Boolean> = mutex.withLock {
        val total = allEntries.value.size
        val current = loadedCount ?: config.pageSize
        if (current >= total) return@withLock Result.success(false)
        loadedCount = (current + config.pageSize).coerceAtMost(total)
        Result.success(true)
    }

    override suspend fun submitScore(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any>,
    ): Result<Unit> {
        val current = allEntries.value
        val existingIndex = current.indexOfFirst { it.userId == userId }
        allEntries.value = if (existingIndex >= 0) {
            current.toMutableList().apply {
                this[existingIndex] = this[existingIndex].copy(score = score, metadata = metadata)
            }
        } else {
            current + LeaderboardEntry(
                userId = userId,
                displayName = metadata["displayName"] as? String ?: userId,
                avatarId = metadata["avatarId"] as? String ?: AvatarDefaults.DEFAULT_AVATAR_ID,
                score = score,
                rank = null,
                metadata = metadata,
            )
        }
        return Result.success(Unit)
    }

    override suspend fun getUserRank(userId: String, config: LeaderboardConfig): Result<Int?> {
        rankOverrides[userId]?.let { return Result.success(it) }
        val entry = rankedEntries(config).firstOrNull { it.userId == userId }
        return Result.success(entry?.rank)
    }

    override suspend fun getSurroundingEntries(
        userId: String,
        radius: Int,
        config: LeaderboardConfig,
    ): Result<List<LeaderboardEntry>> {
        val ranked = rankedEntries(config)
        val index = ranked.indexOfFirst { it.userId == userId }
        if (index < 0) return Result.success(emptyList())
        val from = (index - radius).coerceAtLeast(0)
        val to = (index + radius).coerceAtMost(ranked.size - 1)
        return Result.success(ranked.subList(from, to + 1))
    }
}
