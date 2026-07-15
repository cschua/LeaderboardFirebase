@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.data.common

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.TimeWindow
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TimeWindowResetTest {

    @Test
    fun `all-time, season and custom windows never reset`() {
        val now = Instant.parse("2026-07-08T12:00:00Z")
        val season = TimeWindow.Season("s1", Instant.parse("2026-01-01T00:00:00Z")..Instant.parse("2026-03-01T00:00:00Z"))
        val custom = TimeWindow.Custom(Instant.parse("2026-01-01T00:00:00Z")..Instant.parse("2026-03-01T00:00:00Z"))

        assertThat(TimeWindow.AllTime.timeUntilNextReset(now)).isNull()
        assertThat(season.timeUntilNextReset(now)).isNull()
        assertThat(custom.timeUntilNextReset(now)).isNull()
    }

    @Test
    fun `daily resets at the next midnight in the reset zone`() {
        val now = Instant.parse("2026-07-08T18:30:00Z")

        val remaining = TimeWindow.Daily(TimeZone.UTC).timeUntilNextReset(now)

        assertThat(remaining).isEqualTo(5.hours + 30.minutes)
    }

    @Test
    fun `weekly midweek resolves to the following Monday 00_00 in the reset zone`() {
        // Wednesday 2026-07-08 12:00 UTC -> next reset is Monday 2026-07-13 00:00 UTC.
        val now = Instant.parse("2026-07-08T12:00:00Z")

        val remaining = TimeWindow.Weekly(TimeZone.UTC).timeUntilNextReset(now)

        assertThat(remaining).isEqualTo(4.days + 12.hours)
    }

    @Test
    fun `weekly exactly at the reset instant, the window just reset so a full week remains`() {
        val now = Instant.parse("2026-07-13T00:00:00Z")

        val remaining = TimeWindow.Weekly(TimeZone.UTC).timeUntilNextReset(now)

        assertThat(remaining).isEqualTo(7.days)
    }

    @Test
    fun `weekly one second after reset rolls over to next week, not negative`() {
        val now = Instant.parse("2026-07-13T00:00:01Z")

        val remaining = TimeWindow.Weekly(TimeZone.UTC).timeUntilNextReset(now)

        assertThat(remaining).isEqualTo(6.days + 23.hours + 59.minutes + 59.seconds)
    }

    @Test
    fun `weekly honors a non-UTC reset zone`() {
        // 2026-07-08T02:00:00Z is Wed 2026-07-07 22:00 in America/New_York (UTC-4 in July),
        // so next Monday 00:00 America/New_York is 2026-07-13T04:00:00Z.
        val now = Instant.parse("2026-07-08T02:00:00Z")
        val newYork = TimeZone.of("America/New_York")

        val remaining = TimeWindow.Weekly(newYork).timeUntilNextReset(now)

        assertThat(remaining).isEqualTo(Instant.parse("2026-07-13T04:00:00Z") - now)
    }

    @Test
    fun `monthly resolves to the first of the next month`() {
        val now = Instant.parse("2026-07-08T12:00:00Z")

        val remaining = TimeWindow.Monthly(TimeZone.UTC).timeUntilNextReset(now)

        assertThat(remaining).isEqualTo(Instant.parse("2026-08-01T00:00:00Z") - now)
    }

    @Test
    fun `monthly rolls over the year boundary in December`() {
        val now = Instant.parse("2026-12-20T00:00:00Z")

        val remaining = TimeWindow.Monthly(TimeZone.UTC).timeUntilNextReset(now)

        assertThat(remaining).isEqualTo(Instant.parse("2027-01-01T00:00:00Z") - now)
    }

    @Test
    fun `formatCountdown pads hours minutes and seconds to two digits`() {
        val formatted = formatCountdown(4.days + 2.hours + 5.minutes + 9.seconds)

        assertThat(formatted).isEqualTo("4d 02h 05m 09s")
    }

    @Test
    fun `formatCountdown handles a zero duration`() {
        val formatted = formatCountdown(Duration.ZERO)

        assertThat(formatted).isEqualTo("0d 00h 00m 00s")
    }
}
