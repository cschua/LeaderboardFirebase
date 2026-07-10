package com.leaderboardkit.data.di

import com.leaderboardkit.data.firestore.DefaultFirestorePathStrategy
import com.leaderboardkit.data.firestore.DirectWriteScoreSubmitter
import com.leaderboardkit.data.firestore.FirestorePathStrategy
import com.leaderboardkit.data.firestore.ScoreSubmitter
import com.leaderboardkit.data.ratelimit.ClientRateLimiter
import com.leaderboardkit.data.realtimedb.DefaultRealtimeDbPathStrategy
import com.leaderboardkit.data.realtimedb.RealtimeDbPathStrategy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Default bindings for everything in `:leaderboard:data` *except* which
 * [com.leaderboardkit.domain.repository.LeaderboardRepository] implementation is
 * active — per the brief, that one binding is a host-app config-time decision,
 * not something this library picks for you, so it is deliberately **not** bound
 * here. Wire it up in your own `@Module` with one line:
 *
 * ```kotlin
 * @Module
 * @InstallIn(SingletonComponent::class)
 * abstract class AppLeaderboardBackendModule {
 *     @Binds
 *     abstract fun bindLeaderboardRepository(
 *         impl: FirestoreLeaderboardRepository, // or RealtimeDbLeaderboardRepository
 *     ): LeaderboardRepository
 * }
 * ```
 *
 * (`FakeLeaderboardRepository` is never Hilt-bound — construct it directly in
 * tests/previews.)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LeaderboardDataModule {

    @Binds
    abstract fun bindFirestorePathStrategy(impl: DefaultFirestorePathStrategy): FirestorePathStrategy

    @Binds
    abstract fun bindRealtimeDbPathStrategy(impl: DefaultRealtimeDbPathStrategy): RealtimeDbPathStrategy

    /** Default score-write path — see [ScoreSubmitter] KDoc for when to rebind to `CloudFunctionScoreSubmitter`. */
    @Binds
    abstract fun bindScoreSubmitter(impl: DirectWriteScoreSubmitter): ScoreSubmitter

    companion object {
        /** Minimum time between accepted score submissions for the same (user, board). */
        @Provides
        @Singleton
        fun provideClientRateLimiter(): ClientRateLimiter = ClientRateLimiter(minInterval = 1.seconds)

        @Provides
        @Named("leaderboardSubmitFunctionName")
        fun provideLeaderboardSubmitFunctionName(): String = "submitLeaderboardScore"
    }
}
