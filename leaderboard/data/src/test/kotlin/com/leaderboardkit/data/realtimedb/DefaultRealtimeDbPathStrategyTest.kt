@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.data.realtimedb

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardScope
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.domain.model.leaderboardConfig
import org.junit.Test

class DefaultRealtimeDbPathStrategyTest {

    private val strategy = DefaultRealtimeDbPathStrategy()

    @Test
    fun `global all-time board has no scope segment`() {
        val config = leaderboardConfig("global_alltime") { scope = LeaderboardScope.Global }

        assertThat(strategy.nodePath(config)).isEqualTo("leaderboards/global_alltime/windows/all/entries")
    }

    @Test
    fun `friends board is scoped per user so different users never collide`() {
        val configForAlice = leaderboardConfig("weekly_coins") {
            scope = LeaderboardScope.Friends("alice", emptyList())
            timeWindow = TimeWindow.AllTime
        }
        val configForBob = leaderboardConfig("weekly_coins") {
            scope = LeaderboardScope.Friends("bob", emptyList())
            timeWindow = TimeWindow.AllTime
        }

        assertThat(strategy.nodePath(configForAlice)).isNotEqualTo(strategy.nodePath(configForBob))
        assertThat(strategy.nodePath(configForAlice)).contains("friendsOf/alice")
    }

    @Test
    fun `category board includes the category id`() {
        val config = leaderboardConfig("event_board") { scope = LeaderboardScope.Category("boss_rush") }

        assertThat(strategy.nodePath(config)).contains("category/boss_rush")
    }
}
