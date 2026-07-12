package com.leaderboardkit.data.realtimedb

import com.leaderboardkit.data.common.TimeWindowBucket
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardScope

/** Realtime Database analogue of `FirestorePathStrategy` — resolves a config to a node path. */
@InternalLeaderboardKitApi
fun interface RealtimeDbPathStrategy {
    fun nodePath(config: LeaderboardConfig): String
}

/** `leaderboards/{boardId}/windows/{windowBucket}[/friendsOf/{userId}|/category/{categoryId}|/custom/{filterId}]/entries` */
@InternalLeaderboardKitApi
class DefaultRealtimeDbPathStrategy : RealtimeDbPathStrategy {
    override fun nodePath(config: LeaderboardConfig): String {
        val windowBucket = TimeWindowBucket.currentBucketId(config.timeWindow)
        val scopeSegment = when (val scope = config.scope) {
            is LeaderboardScope.Global -> null
            is LeaderboardScope.Friends -> "friendsOf/${scope.currentUserId}"
            is LeaderboardScope.Category -> "category/${scope.categoryId}"
            is LeaderboardScope.Custom -> "custom/${scope.filterId}"
        }
        return listOfNotNull("leaderboards", config.boardId, "windows", windowBucket, scopeSegment, "entries")
            .joinToString("/")
    }
}
