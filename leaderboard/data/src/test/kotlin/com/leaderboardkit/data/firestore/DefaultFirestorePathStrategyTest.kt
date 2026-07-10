package com.leaderboardkit.data.firestore

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.model.LeaderboardScope
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.domain.model.leaderboardConfig
import org.junit.Test

class DefaultFirestorePathStrategyTest {

    private val strategy = DefaultFirestorePathStrategy()

    @Test
    fun `global all-time board has an odd (valid) segment count and no scope segment`() {
        val config = leaderboardConfig("global_alltime") { scope = LeaderboardScope.Global }

        val path = strategy.collectionPath(config)

        assertThat(path).isEqualTo("leaderboards/global_alltime/windows/all/entries")
        assertThat(path.split("/")).hasSize(5)
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

        assertThat(strategy.collectionPath(configForAlice)).isNotEqualTo(strategy.collectionPath(configForBob))
        assertThat(strategy.collectionPath(configForAlice).split("/")).hasSize(7)
    }

    @Test
    fun `category board includes the category id`() {
        val config = leaderboardConfig("event_board") { scope = LeaderboardScope.Category("boss_rush") }

        assertThat(strategy.collectionPath(config)).contains("category/boss_rush")
    }
}
