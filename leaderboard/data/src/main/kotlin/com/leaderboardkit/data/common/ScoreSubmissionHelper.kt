package com.leaderboardkit.data.common

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.SortDirection

@InternalLeaderboardKitApi
object ScoreSubmissionHelper {

    fun isBetter(candidate: Long, existing: Long, config: LeaderboardConfig): Boolean =
        when (config.sortDirection) {
            SortDirection.Descending -> candidate >= existing
            SortDirection.Ascending -> candidate <= existing
        }

    fun createSubmissionEntry(
        userId: String,
        score: Long,
        metadata: Map<String, Any>,
        existingDisplayName: String?,
        existingAvatarId: String?,
    ): LeaderboardEntry = LeaderboardEntry(
        userId = userId,
        displayName = metadata[EntryFields.DISPLAY_NAME] as? String ?: existingDisplayName.orEmpty(),
        avatarId = metadata[EntryFields.AVATAR_ID] as? String
            ?: existingAvatarId
            ?: AvatarDefaults.DEFAULT_AVATAR_ID,
        score = score,
        rank = null,
        metadata = metadata - EntryFields.DISPLAY_NAME - EntryFields.AVATAR_ID,
    )
}
