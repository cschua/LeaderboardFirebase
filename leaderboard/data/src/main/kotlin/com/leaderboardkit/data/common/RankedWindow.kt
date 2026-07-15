package com.leaderboardkit.data.common

import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.SortDirection

/**
 * [items] is already in display order (best rank first);
 * [startRank] is the rank of its first element.
 */
data class RankedWindow<T>(val items: List<T>, val startRank: Int)

/**
 * Stamps consecutive ranks onto [entries] (already in display order), starting at [startRank].
 * Shared by every [com.leaderboardkit.domain.repository.LeaderboardRepository] implementation.
 */
fun assignRanks(entries: List<LeaderboardEntry>, startRank: Int): List<LeaderboardEntry> =
    entries.mapIndexed { index, entry -> entry.copy(rank = startRank + index) }

/**
 * Rank of the element at [index] in a list stored ascending by score, once [size]
 * and [sortDirection] are known — [SortDirection.Ascending] boards rank best-first
 * from the start of that list, [SortDirection.Descending] boards from the end.
 * Used where the backend has no server-side rank aggregate and must derive it from
 * a full ordered fetch (see `RealtimeDbLeaderboardRepository.getUserRank`).
 */
fun rankFromAscendingIndex(index: Int, size: Int, sortDirection: SortDirection): Int = when (sortDirection) {
    SortDirection.Ascending -> index + 1
    SortDirection.Descending -> size - index
}

/**
 * Given [ascending] (a full list stored ascending by score) and the storage-order
 * [anchorIndex] of some entry within it, returns up to [radius] neighbors on each
 * side — reordered for display (best rank first) and clamped to the list's bounds
 * — plus the rank of the window's first element, ready for [assignRanks].
 *
 * [sortDirection] affects two things independently: which end of [ascending] is
 * "best" (so display order may need reversing), and therefore which storage index
 * the anchor's *display* position actually is — getting either one out of sync
 * with the other silently returns the wrong neighbors, which is exactly the class
 * of bug this function exists to pin down with tests.
 */
fun <T> surroundingWindow(ascending: List<T>, anchorIndex: Int, radius: Int, sortDirection: SortDirection): RankedWindow<T> {
    val display = if (sortDirection == SortDirection.Descending) ascending.asReversed() else ascending
    val displayIndex = if (sortDirection == SortDirection.Descending) ascending.size - 1 - anchorIndex else anchorIndex
    val from = (displayIndex - radius).coerceAtLeast(0)
    val to = (displayIndex + radius).coerceAtMost(display.size - 1)
    return RankedWindow(display.subList(from, to + 1), startRank = from + 1)
}

/**
 * Rank of the first element in a "better-than-anchor" window, given the anchor's own [anchorRank]
 * and how many entries beat it ([betterCount]). Never below rank 1.
 */
fun aboveWindowStartRank(anchorRank: Int, betterCount: Int): Int = (anchorRank - betterCount).coerceAtLeast(1)
