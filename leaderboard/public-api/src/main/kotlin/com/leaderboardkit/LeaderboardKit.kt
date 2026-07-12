@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.leaderboardConfig
import com.leaderboardkit.domain.repository.LeaderboardRepository
import com.leaderboardkit.domain.usecase.GetNearbyRanksUseCase
import com.leaderboardkit.domain.usecase.LoadMoreUseCase
import com.leaderboardkit.domain.usecase.ObserveLeaderboardUseCase
import com.leaderboardkit.domain.usecase.SubmitScoreUseCase
import com.leaderboardkit.presentation.LeaderboardDependencies
import com.leaderboardkit.ui.screen.LeaderboardScreen
import com.leaderboardkit.ui.screen.LeaderboardWidget
import com.leaderboardkit.ui.theme.LeaderboardTheme
import com.leaderboardkit.ui.theme.rememberLeaderboardTheme
import kotlin.time.Duration.Companion.seconds

/**
 * The library's facade — for most host apps, [initialize] once and [screen]
 * everywhere else is the entire integration. Everything this object wires up
 * internally (`FirestoreLeaderboardRepository`, the four Stage-1 use cases, a
 * [LeaderboardDependencies] bundle) stays out of this file's public signatures on
 * purpose: swapping the backend, customizing path strategy, or reaching for the
 * MVI contract directly are still possible by depending on `:leaderboard:data` /
 * `:leaderboard:presentation` / `:leaderboard:ui` yourself instead of this module
 * — see the README's "advanced usage" section — but that's a deliberate opt-out,
 * not something [LeaderboardKit] hands you by default.
 *
 * Firestore is the only backend this facade wires up (see README non-goals);
 * Realtime Database remains available as a reference adapter for hosts that skip
 * this module and wire `:leaderboard:data` themselves.
 */
object LeaderboardKit {

    private const val TAG = "LeaderboardKit"

    private data class State(val kitConfig: LeaderboardKitConfig, val dependencies: LeaderboardDependencies)

    @Volatile
    private var state: State? = null

    /**
     * One-time setup — call from `Application.onCreate()` (or equivalent) before
     * any [screen]/[widget]/[submitScore] call. Selects the Firebase app (default,
     * unless [LeaderboardKitConfig.firebaseAppName] names a secondary one),
     * constructs the direct-write Firestore repository, and wires the four use
     * cases behind it.
     *
     * Safe to call again to swap the active [LeaderboardKitConfig] (e.g. account
     * switch, tests) — re-initialization replaces the previous state atomically
     * and logs a warning rather than failing, since in-flight work built against
     * the old repository/use cases is left to complete on its own.
     */
    fun initialize(context: Context, config: LeaderboardKitConfig) {
        synchronized(this) {
            if (state != null) {
                Log.w(TAG, "LeaderboardKit.initialize() called again — replacing the existing configuration.")
            }

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
            state = State(config, dependencies)
        }
    }

    /** [leaderboardConfig] pre-seeded with [LeaderboardKitConfig.defaultScope] so most boards only need a [boardId]. */
    fun buildConfig(boardId: String, block: LeaderboardConfigBuilder.() -> Unit = {}): LeaderboardConfig =
        leaderboardConfig(boardId) {
            scope = requireConfig().defaultScope
            block()
        }

    /** The library's main entry composable — a full-screen, scrollable, paginated board. */
    @Composable
    fun screen(
        config: LeaderboardConfig,
        theme: LeaderboardTheme = requireConfig().defaultTheme ?: rememberLeaderboardTheme(),
        modifier: Modifier = Modifier,
        rowContent: (@Composable (entry: LeaderboardEntry, isCurrentUser: Boolean) -> Unit)? = null,
        onShowError: (String) -> Unit = {},
    ) {
        val (cfg, deps) = requireState()
        LeaderboardScreen(
            config = config,
            currentUserId = cfg.currentUserId(),
            dependencies = deps,
            theme = theme,
            modifier = modifier,
            rowContent = rowContent,
            avatarResolver = cfg.avatarResolver,
            onShowError = onShowError,
        )
    }

    /** A compact, embeddable top-N card for dashboards/home screens — see `LeaderboardWidget` KDoc for the ViewModel-sharing rules with [screen]. */
    @Composable
    fun widget(
        config: LeaderboardConfig,
        theme: LeaderboardTheme = requireConfig().defaultTheme ?: rememberLeaderboardTheme(),
        modifier: Modifier = Modifier,
        topCount: Int = 5,
        onSeeAllClick: (() -> Unit)? = null,
    ) {
        val (cfg, deps) = requireState()
        LeaderboardWidget(
            config = config,
            currentUserId = cfg.currentUserId(),
            dependencies = deps,
            theme = theme,
            modifier = modifier,
            topCount = topCount,
            avatarResolver = cfg.avatarResolver,
            onSeeAllClick = onSeeAllClick,
        )
    }

    /**
     * Submits [score] for the current user on [config]'s board. Deliberately a
     * plain suspend function rather than something reachable only from inside
     * [screen]'s composition: score submission usually happens on a different
     * screen than the leaderboard itself (e.g. a "run complete" handler), so it
     * needs to be callable from a ViewModel/use case, not just a Compose callback.
     */
    suspend fun submitScore(
        config: LeaderboardConfig,
        score: Long,
        metadata: Map<String, Any> = emptyMap(),
    ): Result<Unit> {
        val (cfg, deps) = requireState()
        return deps.submitScore(cfg.currentUserId(), score, config, metadata)
    }

    private fun requireState(): State = checkNotNull(state) {
        "LeaderboardKit.initialize(context, config) must be called before use — typically in Application.onCreate()."
    }

    private fun requireConfig(): LeaderboardKitConfig = requireState().kitConfig

    private fun requireDependencies(): LeaderboardDependencies = requireState().dependencies
}
