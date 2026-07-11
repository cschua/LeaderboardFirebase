package com.leaderboardkit.sample

import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Stand-in for the sample app's own auth layer — [com.leaderboardkit.LeaderboardKit]
 * has no concept of "who's signed in" itself, it only ever asks the host via
 * [com.leaderboardkit.LeaderboardKitConfig.currentUserId].
 */
object SampleUser {
    /**
     * The signed-in Firebase Auth uid ([SampleApplication.ensureSignedIn] runs
     * before this is ever read). Can't be a fixed string like `"demo_user"` —
     * the README's write rule checks `request.auth.uid == userId`, so this has
     * to match whatever [Firebase.auth] actually signed in as.
     */
    val ID: String
        get() = requireNotNull(Firebase.auth.currentUser?.uid) {
            "SampleUser.ID read before SampleApplication signed in"
        }

    const val DISPLAY_NAME = "You"
    const val AVATAR_ID = "avatar_04"

    /**
     * `DirectWriteScoreSubmitter` only sets `displayName`/`avatarId` from
     * `metadata` — a submission that omits them leaves a brand-new entry with
     * blank fields (there's no prior document to fall back to). Every demo's
     * `onSubmitRandomScore` passes this so submissions are never blank.
     */
    val PROFILE_METADATA: Map<String, Any> = mapOf(
        "displayName" to DISPLAY_NAME,
        "avatarId" to AVATAR_ID,
    )

    /** Friend-graph resolution is the host app's job (see `LeaderboardScope.Friends` KDoc) — hardcoded here for the demo. */
    val FRIEND_IDS = listOf("friend_01", "friend_02", "friend_03", "friend_04")
}
