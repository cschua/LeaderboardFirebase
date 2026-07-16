package com.leaderboardkit.data.realtimedb

import com.leaderboardkit.data.common.CommonPathStrategy
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig

/** Realtime Database analogue of `FirestorePathStrategy` — resolves a config to a node path. */
@InternalLeaderboardKitApi
fun interface RealtimeDbPathStrategy {
    fun nodePath(config: LeaderboardConfig): String
}

/** `leaderboards/{boardId}/windows/{windowBucket}[/friendsOf/{userId}|/category/{categoryId}|/custom/{filterId}]/entries` */
@InternalLeaderboardKitApi
class DefaultRealtimeDbPathStrategy : RealtimeDbPathStrategy {
    override fun nodePath(config: LeaderboardConfig): String = CommonPathStrategy.resolvePath(config)
}
