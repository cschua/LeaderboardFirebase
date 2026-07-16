package com.leaderboardkit.data.common

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry
import kotlinx.coroutines.sync.Mutex

/**
 * Common state tracked by repositories per-board to support pagination and
 * concurrent-safe [com.leaderboardkit.domain.repository.LeaderboardRepository.loadMore] calls.
 *
 * @param T The backend-specific cursor type (e.g. `DocumentSnapshot` for Firestore).
 */
@InternalLeaderboardKitApi
class BaseBoardState<T> {
    val mutex = Mutex()
    var loadedEntries: List<LeaderboardEntry> = emptyList()
    var cursor: T? = null
    var endReached: Boolean = false

    fun update(newEntries: List<LeaderboardEntry>, nextCursor: T?, isEnd: Boolean, append: Boolean = false) {
        loadedEntries = if (append) loadedEntries + newEntries else newEntries
        cursor = nextCursor
        endReached = isEnd
    }
}
