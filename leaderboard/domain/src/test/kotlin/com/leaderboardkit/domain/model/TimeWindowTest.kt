package com.leaderboardkit.domain.model

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Instant

class TimeWindowTest {

    private val range = Instant.parse("2026-01-01T00:00:00Z")..Instant.parse("2026-03-01T00:00:00Z")

    @Test
    fun `resetTimeZone is null for AllTime`() {
        assertThat(TimeWindow.AllTime.resetTimeZone).isNull()
    }

    @Test
    fun `resetTimeZone is null for Season`() {
        val season = TimeWindow.Season("s1", range)
        assertThat(season.resetTimeZone).isNull()
        assertThat(season.seasonId).isEqualTo("s1")
        assertThat(season.range).isEqualTo(range)
    }

    @Test
    fun `resetTimeZone is null for Custom`() {
        val custom = TimeWindow.Custom(range)
        assertThat(custom.resetTimeZone).isNull()
        assertThat(custom.range).isEqualTo(range)
    }

    @Test
    fun `resetTimeZone is preserved for Daily`() {
        val zone = TimeZone.of("America/New_York")
        assertThat(TimeWindow.Daily(zone).resetTimeZone).isEqualTo(zone)
    }

    @Test
    fun `resetTimeZone is preserved for Weekly`() {
        val zone = TimeZone.of("Europe/London")
        assertThat(TimeWindow.Weekly(zone).resetTimeZone).isEqualTo(zone)
    }

    @Test
    fun `resetTimeZone is preserved for Monthly`() {
        val zone = TimeZone.of("Asia/Tokyo")
        assertThat(TimeWindow.Monthly(zone).resetTimeZone).isEqualTo(zone)
    }
}
