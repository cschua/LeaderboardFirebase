package com.leaderboardkit.sample

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.leaderboardkit.LeaderboardKit
import com.leaderboardkit.LeaderboardKitConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

/**
 * The entire library setup, per the README's "under 20 lines" claim: one
 * [LeaderboardKit.initialize] call in [onCreate], nothing else. Every demo
 * screen after this point only ever calls [LeaderboardKit.screen]/[LeaderboardKit.submitScore].
 *
 * A real app also needs a `google-services.json` in this module for
 * [LeaderboardKit.initialize]'s Firebase calls to reach an actual project —
 * see the README's setup section. Without one, the demos still build and
 * render (loading/error states), they just won't reach a live board.
 */
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureSignedIn()
        LeaderboardKit.initialize(
            context = this,
            config = LeaderboardKitConfig(
                currentUserId = { SampleUser.ID },
            ),
        )
    }

    /**
     * The README's Firestore rules require `request.auth != null` to read a
     * board at all, and `request.auth.uid == userId` to write one — so
     * something has to sign in before the first Firestore call goes out.
     * The demo has no sign-in screen, so this blocks startup on anonymous
     * auth instead (fast, and [SampleUser.ID] depends on it existing).
     */
    private fun ensureSignedIn() = runBlocking {
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously().await()
        }
    }
}
