package com.leaderboardkit.sample

import android.app.Application
import com.leaderboardkit.LeaderboardKit
import com.leaderboardkit.LeaderboardKitConfig

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
        LeaderboardKit.initialize(
            context = this,
            config = LeaderboardKitConfig(
                currentUserId = { SampleUser.ID },
            ),
        )
    }
}
