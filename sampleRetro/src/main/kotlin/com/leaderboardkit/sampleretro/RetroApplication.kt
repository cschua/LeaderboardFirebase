@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.sampleretro

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.leaderboardkit.data.firestore.DefaultFirestorePathStrategy
import com.leaderboardkit.data.firestore.DirectWriteScoreSubmitter
import com.leaderboardkit.data.firestore.FirestoreLeaderboardEntryMapper
import com.leaderboardkit.data.firestore.FirestoreLeaderboardRepository
import com.leaderboardkit.data.ratelimit.ClientRateLimiter
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.repository.LeaderboardRepository
import com.leaderboardkit.domain.usecase.GetNearbyRanksUseCase
import com.leaderboardkit.domain.usecase.LoadMoreUseCase
import com.leaderboardkit.domain.usecase.ObserveLeaderboardUseCase
import com.leaderboardkit.domain.usecase.SubmitScoreUseCase
import com.leaderboardkit.presentation.LeaderboardDependencies
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.milliseconds

/**
 * This sample's composition root — where [SampleApplication][com.leaderboardkit.sample.SampleApplication]
 * makes one `LeaderboardKit.initialize` call and lets the `:leaderboard:public-api`
 * facade wire everything behind it, this app builds the same
 * `:leaderboard:data` → `:leaderboard:domain` use-case → [LeaderboardDependencies]
 * chain itself. That's the whole point of `sampleRetro`: demonstrate the Clean
 * Architecture module boundaries and the MVI contract
 * ([com.leaderboardkit.presentation.LeaderboardViewModel]) directly rather than
 * through the convenience layer — see `RetroLeaderboardScreen` for where
 * [leaderboardDependencies] ends up.
 *
 * [rateLimiter]'s cooldown is much shorter than the facade's default 1 second:
 * submitting a score here fans out to all three time-window buckets for the same
 * board (see `RetroLeaderboardScreen.submitScoreToAllWindows`), and
 * [ClientRateLimiter] keys its cooldown by `(userId, boardId)` alone, not by
 * window — a 1-second cooldown would make that three-write fan-out block on
 * itself.
 */
class RetroApplication : Application() {

    lateinit var leaderboardDependencies: LeaderboardDependencies
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        ensureSignedIn()

        val firestore = FirebaseFirestore.getInstance()
        val pathStrategy = DefaultFirestorePathStrategy()
        val mapper = FirestoreLeaderboardEntryMapper()
        val rateLimiter = ClientRateLimiter(minInterval = 200.milliseconds)
        val scoreSubmitter = DirectWriteScoreSubmitter(firestore, pathStrategy, mapper, rateLimiter)
        val repository: LeaderboardRepository = FirestoreLeaderboardRepository(firestore, pathStrategy, mapper, scoreSubmitter)

        leaderboardDependencies = LeaderboardDependencies(
            observeLeaderboard = ObserveLeaderboardUseCase(repository),
            loadMore = LoadMoreUseCase(repository),
            submitScore = SubmitScoreUseCase(repository),
            getNearbyRanks = GetNearbyRanksUseCase(repository),
        )
    }

    /** Same anonymous-sign-in stand-in as `:sample`'s `SampleApplication` — see its KDoc. */
    private fun ensureSignedIn() = runBlocking {
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously().await()
        }
    }
}
