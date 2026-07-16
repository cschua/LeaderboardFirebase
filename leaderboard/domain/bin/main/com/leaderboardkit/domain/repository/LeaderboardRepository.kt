package com.leaderboardkit.domain.repository

import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow

/**
 * Backend-agnostic access to leaderboard data. Implementations live in
 * `:leaderboard:data` (Firestore, Realtime Database, in-memory fake...) — nothing
 * in this interface, or anything upstream of it, may reference a specific backend
 * SDK type.
 *
 * ### Pagination
 * This library uses a cursor-based `loadMore()` rather than `Flow<PagingData>`:
 * Paging3 is AndroidX-only, which would force an Android dependency onto a design
 * meant to stay backend- and platform-swappable. [observeEntries] emits the
 * currently loaded window for [config]; [loadMore] fetches and appends the next
 * page. Concurrent [loadMore] calls for the same [LeaderboardConfig.boardId] are
 * serialized by the implementation (e.g. via `Mutex`), so callers do not need to
 * guard against double-invocation themselves (a fast double-tap on a "load more"
 * button is safe).
 *
 * Pagination is meant for [com.leaderboardkit.domain.model.RefreshStrategy.Polling]
 * / [com.leaderboardkit.domain.model.RefreshStrategy.ManualOnly] boards (large,
 * page-through boards like global/seasonal). Boards using
 * [com.leaderboardkit.domain.model.RefreshStrategy.RealtimeListener] are expected
 * to be small, already-bounded result sets (friends, a ±N surrounding-rank window)
 * fetched in a single page; calling [loadMore] on one of those is legal but
 * typically a no-op once the bounded set is exhausted.
 */
interface LeaderboardRepository {

    /**
     * The currently loaded window of entries for [config], re-emitting whenever the
     * underlying data changes (per [LeaderboardConfig.refreshStrategy]) or a new
     * page is appended via [loadMore]. Cold: (re)subscribing starts the strategy's
     * observation (listener/poll loop) and stops it when the last subscriber
     * disappears (`SharingStarted.WhileSubscribed` semantics in the implementation).
     */
    fun observeEntries(config: LeaderboardConfig): Flow<List<LeaderboardEntry>>

    /**
     * Fetches the next page for [config] and appends it to the window
     * [observeEntries] emits.
     *
     * @return `true` if a non-empty page was appended, `false` if the end of the
     *   result set was reached (nothing left to load).
     */
    suspend fun loadMore(config: LeaderboardConfig): Result<Boolean>

    /**
     * Submits [score] for [userId] on the board described by [config]. See the
     * data-layer docs for the two supported write paths (direct client write with
     * rate-limiting vs. Cloud Function) and how to choose between them.
     */
    suspend fun submitScore(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any> = emptyMap(),
    ): Result<Unit>

    /** The 1-based rank of [userId] on [config]'s board, or `null` if they have no entry. */
    suspend fun getUserRank(userId: String, config: LeaderboardConfig): Result<Int?>

    /**
     * Up to `2 * radius + 1` entries centered on [userId]'s rank on [config]'s
     * board (that many entries above and below, clipped at the ends of the board).
     * Intended to back a "your position" UI paired with
     * [com.leaderboardkit.domain.model.RefreshStrategy.RealtimeListener].
     */
    suspend fun getSurroundingEntries(
        userId: String,
        radius: Int,
        config: LeaderboardConfig,
    ): Result<List<LeaderboardEntry>>
}
