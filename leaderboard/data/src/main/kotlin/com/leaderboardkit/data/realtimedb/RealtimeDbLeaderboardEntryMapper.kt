package com.leaderboardkit.data.realtimedb

import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry

private const val FIELD_DISPLAY_NAME = "displayName"
private const val FIELD_AVATAR_ID = "avatarId"
private const val FIELD_SCORE = "score"
private const val FIELD_METADATA = "metadata"

/**
 * Converts between [LeaderboardEntry] and the `Map<String, Any?>` shape of a
 * Realtime Database node value. Takes the node key (the userId) and its value map
 * rather than a `DataSnapshot` directly, for the same unit-testability reason as
 * `FirestoreLeaderboardEntryMapper`.
 *
 * Realtime Database has no server-computed rank field equivalent in this design —
 * [LeaderboardEntry.rank] is always assigned client-side by the repository from
 * list position, never read from the stored value.
 */
@InternalLeaderboardKitApi
class RealtimeDbLeaderboardEntryMapper {

    @Suppress("UNCHECKED_CAST")
    fun fromNode(userId: String, value: Map<String, Any?>): LeaderboardEntry = LeaderboardEntry(
        userId = userId,
        displayName = value[FIELD_DISPLAY_NAME] as? String ?: "",
        avatarId = value[FIELD_AVATAR_ID] as? String ?: AvatarDefaults.DEFAULT_AVATAR_ID,
        score = (value[FIELD_SCORE] as? Number)?.toLong() ?: 0L,
        rank = null,
        metadata = (value[FIELD_METADATA] as? Map<String, Any>) ?: emptyMap(),
    )

    fun toNode(entry: LeaderboardEntry): Map<String, Any?> = mapOf(
        FIELD_DISPLAY_NAME to entry.displayName,
        FIELD_AVATAR_ID to entry.avatarId,
        FIELD_SCORE to entry.score,
        FIELD_METADATA to entry.metadata,
    )
}
