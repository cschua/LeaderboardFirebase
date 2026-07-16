package com.leaderboardkit.domain.model

/**
 * How to order entries that have an identical [LeaderboardEntry.score].
 *
 * The achievement timestamp used by [EarliestAchievedFirst]/[LatestAchievedFirst]
 * is not a first-class [LeaderboardEntry] field — it is read from
 * [LeaderboardEntry.metadata] by the data layer using [metadataKey], since not
 * every board tracks achievement time. If the key is absent for an entry, that
 * entry sorts after all entries that have it.
 */
sealed interface TieBreak {

    /** Ties are left in whatever order the backend returns them. */
    data object None : TieBreak

    data class EarliestAchievedFirst(val metadataKey: String = "achievedAt") : TieBreak

    data class LatestAchievedFirst(val metadataKey: String = "achievedAt") : TieBreak
}
