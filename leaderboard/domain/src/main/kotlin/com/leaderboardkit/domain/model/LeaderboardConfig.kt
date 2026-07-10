package com.leaderboardkit.domain.model

/**
 * Fully describes one leaderboard "shape". Every axis of configurability the
 * library supports is a field here — new board shapes are expressed by
 * constructing a different [LeaderboardConfig], never by subclassing repositories
 * or use cases.
 *
 * @param boardId Stable logical identifier for this board (e.g. "global_alltime",
 *   "weekly_coins", "raid_boss_42"). Used by the data layer to key collection/path
 *   strategy lookups and per-board pagination cursor state, so two configs that
 *   should be treated as the same underlying board must share a [boardId].
 * @param pageSize Number of entries fetched per page / per [RefreshStrategy.Polling] tick.
 * @param prefetchDistance How many entries from the end of the currently loaded
 *   window a UI layer should trigger `loadMore()` at. Purely advisory to consumers
 *   of this config; the data layer does not act on it directly.
 */
data class LeaderboardConfig(
    val boardId: String,
    val scope: LeaderboardScope,
    val timeWindow: TimeWindow,
    val sortDirection: SortDirection,
    val tieBreak: TieBreak,
    val pageSize: Int,
    val prefetchDistance: Int,
    val refreshStrategy: RefreshStrategy,
) {
    init {
        require(boardId.isNotBlank()) { "boardId must not be blank" }
        require(pageSize > 0) { "pageSize must be > 0, was $pageSize" }
        require(prefetchDistance >= 0) { "prefetchDistance must be >= 0, was $prefetchDistance" }
    }
}
