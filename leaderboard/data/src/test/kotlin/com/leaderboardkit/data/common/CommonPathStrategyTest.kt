@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.data.common

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardScope
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.domain.model.leaderboardConfig
import org.junit.Test

class CommonPathStrategyTest {

    @Test
    fun `global all-time board path`() {
        val config = leaderboardConfig("global_alltime") { scope = LeaderboardScope.Global }
        assertThat(CommonPathStrategy.resolvePath(config)).isEqualTo("leaderboards/global_alltime/windows/all/entries")
    }

    @Test
    fun `friends board path is scoped per user`() {
        val configForAlice = leaderboardConfig("weekly_coins") {
            scope = LeaderboardScope.Friends("alice", emptyList())
            timeWindow = TimeWindow.AllTime
        }
        val configForBob = leaderboardConfig("weekly_coins") {
            scope = LeaderboardScope.Friends("bob", emptyList())
            timeWindow = TimeWindow.AllTime
        }

        assertThat(CommonPathStrategy.resolvePath(configForAlice)).isEqualTo("leaderboards/weekly_coins/windows/all/friendsOf/alice/entries")
        assertThat(CommonPathStrategy.resolvePath(configForBob)).isEqualTo("leaderboards/weekly_coins/windows/all/friendsOf/bob/entries")
    }

    @Test
    fun `category board path includes the category id`() {
        val config = leaderboardConfig("event_board") { scope = LeaderboardScope.Category("boss_rush") }
        assertThat(CommonPathStrategy.resolvePath(config)).contains("category/boss_rush")
    }
}
