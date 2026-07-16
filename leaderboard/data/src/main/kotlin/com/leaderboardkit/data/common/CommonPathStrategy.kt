package com.leaderboardkit.data.common

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardScope

@InternalLeaderboardKitApi
object CommonPathStrategy {
    fun resolvePath(config: LeaderboardConfig): String {
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
