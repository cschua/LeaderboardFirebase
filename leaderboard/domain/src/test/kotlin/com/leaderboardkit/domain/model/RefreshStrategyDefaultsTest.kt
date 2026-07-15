package com.leaderboardkit.domain.model

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Verifies [RefreshStrategyDefaults.recommendedFor] against the recommended-defaults
 * table documented on [RefreshStrategy] directly, rather than relying on incidental
 * coverage from [LeaderboardConfigBuilder]'s fallback wiring.
 */
class RefreshStrategyDefaultsTest {

    @Test
    fun `global all-time boards poll every 45 seconds`() {
        val strategy = RefreshStrategyDefaults.recommendedFor(LeaderboardScope.Global, TimeWindow.AllTime)

        assertThat(strategy).isEqualTo(RefreshStrategy.Polling(45.seconds))
    }

    @Test
    fun `global boards with any other time window poll every 3 minutes`() {
        val weekly = RefreshStrategyDefaults.recommendedFor(LeaderboardScope.Global, TimeWindow.Weekly(TimeZone.UTC))
        val daily = RefreshStrategyDefaults.recommendedFor(LeaderboardScope.Global, TimeWindow.Daily(TimeZone.UTC))
        val monthly = RefreshStrategyDefaults.recommendedFor(LeaderboardScope.Global, TimeWindow.Monthly(TimeZone.UTC))
        val season = RefreshStrategyDefaults.recommendedFor(
            LeaderboardScope.Global,
            TimeWindow.Season("s1", Instant.parse("2026-01-01T00:00:00Z")..Instant.parse("2026-03-01T00:00:00Z"))
        )
        val custom = RefreshStrategyDefaults.recommendedFor(
            LeaderboardScope.Global,
            TimeWindow.Custom(Instant.parse("2026-01-01T00:00:00Z")..Instant.parse("2026-03-01T00:00:00Z"))
        )

        assertThat(weekly).isEqualTo(RefreshStrategy.Polling(3.minutes))
        assertThat(daily).isEqualTo(RefreshStrategy.Polling(3.minutes))
        assertThat(monthly).isEqualTo(RefreshStrategy.Polling(3.minutes))
        assertThat(season).isEqualTo(RefreshStrategy.Polling(3.minutes))
        assertThat(custom).isEqualTo(RefreshStrategy.Polling(3.minutes))
    }

    @Test
    fun `friends boards always use a realtime listener regardless of time window`() {
        val friends = LeaderboardScope.Friends(currentUserId = "u1", friendUserIds = listOf("u2", "u3"))

        val allTime = RefreshStrategyDefaults.recommendedFor(friends, TimeWindow.AllTime)
        val weekly = RefreshStrategyDefaults.recommendedFor(friends, TimeWindow.Weekly(TimeZone.UTC))

        assertThat(allTime).isEqualTo(RefreshStrategy.RealtimeListener)
        assertThat(weekly).isEqualTo(RefreshStrategy.RealtimeListener)
    }

    @Test
    fun `category boards poll every 3 minutes regardless of time window`() {
        val strategy = RefreshStrategyDefaults.recommendedFor(LeaderboardScope.Category("boss_rush"), TimeWindow.AllTime)

        assertThat(strategy).isEqualTo(RefreshStrategy.Polling(3.minutes))
    }

    @Test
    fun `custom boards poll every 3 minutes regardless of time window`() {
        val strategy = RefreshStrategyDefaults.recommendedFor(LeaderboardScope.Custom("vip_only"), TimeWindow.AllTime)

        assertThat(strategy).isEqualTo(RefreshStrategy.Polling(3.minutes))
    }
}
