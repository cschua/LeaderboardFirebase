package com.leaderboardkit.sample

/**
 * Stand-in for the sample app's own auth layer — [com.leaderboardkit.LeaderboardKit]
 * has no concept of "who's signed in" itself, it only ever asks the host via
 * [com.leaderboardkit.LeaderboardKitConfig.currentUserId].
 */
object SampleUser {
    const val ID = "demo_user"
    const val DISPLAY_NAME = "You"
    const val AVATAR_ID = "avatar_04"

    /** Friend-graph resolution is the host app's job (see `LeaderboardScope.Friends` KDoc) — hardcoded here for the demo. */
    val FRIEND_IDS = listOf("friend_01", "friend_02", "friend_03", "friend_04")
}
