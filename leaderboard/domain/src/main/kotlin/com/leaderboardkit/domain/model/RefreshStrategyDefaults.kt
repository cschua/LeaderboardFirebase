package com.leaderboardkit.domain.model

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Implements the recommended-defaults table documented on [RefreshStrategy]: when a
 * [LeaderboardConfigBuilder] is not given an explicit [RefreshStrategy], this is what
 * gets applied based on [LeaderboardScope] and [TimeWindow] alone.
 */
object RefreshStrategyDefaults {

    fun recommendedFor(scope: LeaderboardScope, timeWindow: TimeWindow): RefreshStrategy = when (scope) {
        is LeaderboardScope.Friends -> RefreshStrategy.RealtimeListener

        is LeaderboardScope.Global -> when (timeWindow) {
            is TimeWindow.AllTime -> RefreshStrategy.Polling(45.seconds)
            else -> RefreshStrategy.Polling(3.minutes)
        }

        is LeaderboardScope.Category, is LeaderboardScope.Custom -> RefreshStrategy.Polling(3.minutes)
    }
}
