package com.leaderboardkit.data.firestore

import com.leaderboardkit.data.common.TimeWindowBucket
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardScope

/**
 * Resolves a [LeaderboardConfig] to the Firestore collection holding its entries.
 *
 * This is a config-time extension point, not a hardcoded path: multi-tenant hosts
 * (e.g. a game with multiple titles, or a white-labeled app) supply their own
 * implementation — typically [DefaultFirestorePathStrategy] wrapped with a
 * tenant/app-id prefix — and bind it in place of the default via Hilt.
 */
@InternalLeaderboardKitApi
fun interface FirestorePathStrategy {
    fun collectionPath(config: LeaderboardConfig): String
}

/**
 * `leaderboards/{boardId}/windows/{windowBucket}/entries`, with an additional
 * `/friendsOf/{userId}` segment for [LeaderboardScope.Friends] so that per-user
 * friend boards (which are never queried across users) don't collide.
 *
 * The window bucket segment (see [TimeWindowBucket]) gives every time window its
 * own collection, which is what makes window expiration a no-op rather than a
 * purge: old buckets are simply never addressed again.
 */
@InternalLeaderboardKitApi
class DefaultFirestorePathStrategy : FirestorePathStrategy {
    override fun collectionPath(config: LeaderboardConfig): String {
        val windowBucket = TimeWindowBucket.currentBucketId(config.timeWindow)
        val scopeSegment = when (val scope = config.scope) {
            is LeaderboardScope.Global -> null
            is LeaderboardScope.Friends -> "friendsOf/${scope.currentUserId}"
            is LeaderboardScope.Category -> "category/${scope.categoryId}"
            is LeaderboardScope.Custom -> "custom/${scope.filterId}"
        }
        val segments = listOfNotNull(
            "leaderboards",
            config.boardId,
            "windows",
            windowBucket,
            scopeSegment,
            "entries",
        )
        return segments.joinToString("/")
    }
}
