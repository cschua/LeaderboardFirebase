package com.leaderboardkit.data.common

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry

@InternalLeaderboardKitApi
object EntryFields {
    const val USER_ID = "userId"
    const val DISPLAY_NAME = "displayName"
    const val AVATAR_ID = "avatarId"
    const val SCORE = "score"
    const val RANK = "rank"
    const val METADATA = "metadata"
}

/**
 * Shared mapping logic for Firebase-like map structures.
 */
@InternalLeaderboardKitApi
object EntryMapper {

    @Suppress("UNCHECKED_CAST")
    fun fromMap(id: String, data: Map<String, Any?>): LeaderboardEntry = LeaderboardEntry(
        userId = data[EntryFields.USER_ID] as? String ?: id,
        displayName = data[EntryFields.DISPLAY_NAME] as? String ?: "",
        avatarId = data[EntryFields.AVATAR_ID] as? String ?: AvatarDefaults.DEFAULT_AVATAR_ID,
        score = (data[EntryFields.SCORE] as? Number)?.toLong() ?: 0L,
        rank = (data[EntryFields.RANK] as? Number)?.toInt(),
        metadata = (data[EntryFields.METADATA] as? Map<String, Any>) ?: emptyMap(),
    )

    fun toMap(entry: LeaderboardEntry, includeUserId: Boolean = true, includeRank: Boolean = true): Map<String, Any?> = buildMap {
        if (includeUserId) put(EntryFields.USER_ID, entry.userId)
        put(EntryFields.DISPLAY_NAME, entry.displayName)
        put(EntryFields.AVATAR_ID, entry.avatarId)
        put(EntryFields.SCORE, entry.score)
        if (includeRank) put(EntryFields.RANK, entry.rank)
        put(EntryFields.METADATA, entry.metadata)
    }
}
