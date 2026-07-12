package com.leaderboardkit.sampleretro

import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/** Stand-in for this sample's own auth/profile layer — mirrors `:sample`'s `SampleUser`. */
object RetroUser {
    /** The signed-in Firebase Auth uid ([RetroApplication.ensureSignedIn] runs before this is ever read). */
    val ID: String
        get() = requireNotNull(Firebase.auth.currentUser?.uid) {
            "RetroUser.ID read before RetroApplication signed in"
        }

    const val DISPLAY_NAME = "PLAYER 1"
    const val AVATAR_ID = "avatar_09"

    /** See `DirectWriteScoreSubmitter` KDoc — a submission that omits these leaves a blank entry. */
    val PROFILE_METADATA: Map<String, Any> = mapOf(
        "displayName" to DISPLAY_NAME,
        "avatarId" to AVATAR_ID,
    )
}
