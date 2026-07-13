@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.leaderboardkit.data.firestore.DefaultFirestorePathStrategy
import com.leaderboardkit.data.firestore.DirectWriteScoreSubmitter
import com.leaderboardkit.data.firestore.FirestoreLeaderboardEntryMapper
import com.leaderboardkit.data.firestore.FirestoreLeaderboardRepository
import com.leaderboardkit.data.ratelimit.ClientRateLimiter
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardConfigBuilder
import com.leaderboardkit.domain.model.leaderboardConfig
import com.leaderboardkit.domain.repository.LeaderboardRepository
import com.leaderboardkit.domain.usecase.GetNearbyRanksUseCase
import com.leaderboardkit.domain.usecase.LoadMoreUseCase
import com.leaderboardkit.domain.usecase.ObserveLeaderboardUseCase
import com.leaderboardkit.domain.usecase.SubmitScoreUseCase
import com.leaderboardkit.presentation.LeaderboardDependencies
import kotlin.time.Duration.Companion.seconds

/**
 * A scoped handle to one [LeaderboardKitConfig]'s worth of wiring — the
 * `:leaderboard:data` repository and Stage-1 use cases [createLeaderboardClient]
 * builds from it, bundled as [LeaderboardDependencies]. Unlike the old
 * `LeaderboardKit` singleton this replaces, nothing here is global: hold as many
 * of these as you need (multiple Firebase apps, tests, previews) without them
 * clobbering each other, and thread one through the composition with
 * [ProvideLeaderboardClient] wherever [LeaderboardScreen]/[LeaderboardWidget] need it.
 *
 * Construct via [createLeaderboardClient] — not directly — so every instance is
 * fully wired.
 */
class LeaderboardClient internal constructor(
    internal val config: LeaderboardKitConfig,
    internal val dependencies: LeaderboardDependencies,
) {

    /** [leaderboardConfig] pre-seeded with [LeaderboardKitConfig.defaultScope] so most boards only need a [boardId]. */
    fun buildConfig(boardId: String, block: LeaderboardConfigBuilder.() -> Unit = {}): LeaderboardConfig =
        leaderboardConfig(boardId) {
            scope = config.defaultScope
            block()
        }

    /**
     * Submits [score] for the current user on [config]'s board. Deliberately a
     * plain suspend function rather than something reachable only from inside
     * [LeaderboardScreen]'s composition: score submission usually happens on a different
     * screen than the leaderboard itself (e.g. a "run complete" handler), so it
     * needs to be callable from a ViewModel/use case, not just a Compose callback.
     */
    suspend fun submitScore(
        config: LeaderboardConfig,
        score: Long,
        metadata: Map<String, Any> = emptyMap(),
    ): Result<Unit> = dependencies.submitScore(this.config.currentUserId(), score, config, metadata)
}

/**
 * Builds a [LeaderboardClient] for [config] — selects the Firebase app
 * (default, unless [LeaderboardKitConfig.firebaseAppName] names a secondary
 * one), constructs the direct-write Firestore repository, and wires the four
 * use cases behind it. Replaces the old `LeaderboardKit.initialize`; call once
 * (typically `Application.onCreate()`) and hold onto the result — usually to
 * hand to [ProvideLeaderboardClient] further down the composition.
 */
fun createLeaderboardClient(context: Context, config: LeaderboardKitConfig): LeaderboardClient {
    FirebaseApp.initializeApp(context.applicationContext)
    val firebaseApp = config.firebaseAppName?.let { FirebaseApp.getInstance(it) } ?: FirebaseApp.getInstance()
    val firestore = FirebaseFirestore.getInstance(firebaseApp)

    val pathStrategy = DefaultFirestorePathStrategy()
    val mapper = FirestoreLeaderboardEntryMapper()
    val rateLimiter = ClientRateLimiter(minInterval = 1.seconds)
    val scoreSubmitter = DirectWriteScoreSubmitter(firestore, pathStrategy, mapper, rateLimiter)
    val repository: LeaderboardRepository = FirestoreLeaderboardRepository(firestore, pathStrategy, mapper, scoreSubmitter)

    val dependencies = LeaderboardDependencies(
        observeLeaderboard = ObserveLeaderboardUseCase(repository),
        loadMore = LoadMoreUseCase(repository),
        submitScore = SubmitScoreUseCase(repository),
        getNearbyRanks = GetNearbyRanksUseCase(repository),
    )
    return LeaderboardClient(config, dependencies)
}
