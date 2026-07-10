package com.leaderboardkit.domain.model

/**
 * A single row on a leaderboard.
 *
 * [score] is a [Long] rather than a floating point type so that ordering and
 * tie-breaking are exact. Boards where a smaller raw measurement wins (e.g. race
 * times) should encode the measurement as an integer unit (milliseconds, centimeters,
 * etc.) and pair it with [com.leaderboardkit.domain.model.SortDirection.Ascending]
 * rather than switching to a floating point score.
 *
 * [rank] is nullable: it is unset for entries that haven't been through a
 * ranking pass yet (e.g. a raw page fetch before client-side rank assignment,
 * or a backend that hasn't denormalized rank fields).
 *
 * [avatarId] is a key into a fixed set of locally-bundled avatar drawables
 * (e.g. "avatar_01") — not a URL. Avatars are never fetched over the network;
 * resolving an [avatarId] to an actual drawable resource is a UI-layer concern.
 */
data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val avatarId: String,
    val score: Long,
    val rank: Int?,
    val metadata: Map<String, Any> = emptyMap(),
)
