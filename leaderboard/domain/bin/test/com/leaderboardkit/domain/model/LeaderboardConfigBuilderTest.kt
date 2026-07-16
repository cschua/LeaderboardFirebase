package com.leaderboardkit.domain.model

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LeaderboardConfigBuilderTest {

    @Test
    fun `defaults to global all-time descending with recommended polling`() {
        val config = leaderboardConfig("global_alltime") {}

        assertThat(config.scope).isEqualTo(LeaderboardScope.Global)
        assertThat(config.timeWindow).isEqualTo(TimeWindow.AllTime)
        assertThat(config.sortDirection).isEqualTo(SortDirection.Descending)
        assertThat(config.tieBreak).isEqualTo(TieBreak.None)
        assertThat(config.pageSize).isEqualTo(25)
        assertThat(config.refreshStrategy).isEqualTo(RefreshStrategy.Polling(45.seconds))
    }

    @Test
    fun `friends scope defaults to realtime listener`() {
        val config = leaderboardConfig("friends_board") {
            scope = LeaderboardScope.Friends("me", listOf("f1", "f2"))
        }

        assertThat(config.refreshStrategy).isEqualTo(RefreshStrategy.RealtimeListener)
    }

    @Test
    fun `weekly global board defaults to slower polling than all-time`() {
        val config = leaderboardConfig("weekly_coins") {
            timeWindow = TimeWindow.Weekly(resetTimeZone = TimeZone.UTC)
        }

        assertThat(config.refreshStrategy).isEqualTo(RefreshStrategy.Polling(3.minutes))
    }

    @Test
    fun `explicit refresh strategy overrides the recommended default`() {
        val config = leaderboardConfig("friends_board") {
            scope = LeaderboardScope.Friends("me", emptyList())
            refreshStrategy = RefreshStrategy.ManualOnly
        }

        assertThat(config.refreshStrategy).isEqualTo(RefreshStrategy.ManualOnly)
    }
}
