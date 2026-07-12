package com.leaderboardkit.data.common

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi

/**
 * Avatars are a fixed set of locally-bundled drawable resources, resolved by
 * [com.leaderboardkit.domain.model.LeaderboardEntry.avatarId] — never a remote
 * image. This is the data layer's canonical placeholder set, used when
 * constructing entries that have no avatar selection yet (e.g. a first-ever
 * score submission with no `avatarId` in its metadata).
 *
 * The actual drawable resources these ids map to are a UI-layer concern for a
 * later stage; this module only ever produces/consumes the id string.
 */
@InternalLeaderboardKitApi
object AvatarDefaults {
    val PLACEHOLDER_AVATAR_IDS: List<String> = (1..12).map { "avatar_%02d".format(it) }
    val DEFAULT_AVATAR_ID: String = PLACEHOLDER_AVATAR_IDS.first()
}
