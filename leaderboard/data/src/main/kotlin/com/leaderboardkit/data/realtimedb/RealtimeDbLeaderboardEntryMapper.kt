package com.leaderboardkit.data.realtimedb

import com.leaderboardkit.data.common.EntryMapper
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry

/**
 * Converts between [LeaderboardEntry] and the `Map<String, Any?>` shape of a
 * Realtime Database node value.
 */
@InternalLeaderboardKitApi
class RealtimeDbLeaderboardEntryMapper {

    fun fromNode(userId: String, value: Map<String, Any?>): LeaderboardEntry =
        EntryMapper.fromMap(userId, value).copy(rank = null)

    fun toNode(entry: LeaderboardEntry): Map<String, Any?> =
        EntryMapper.toMap(entry, includeUserId = false, includeRank = false)
}
