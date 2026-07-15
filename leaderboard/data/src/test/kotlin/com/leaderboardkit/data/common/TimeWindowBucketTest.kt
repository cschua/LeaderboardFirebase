package com.leaderboardkit.data.common

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.model.TimeWindow
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Instant

class TimeWindowBucketTest {

    // 2026-07-09T12:00:00Z is a Thursday.
    private val thursdayNoonUtc = Instant.parse("2026-07-09T12:00:00Z")

    @Test
    fun `all-time bucket is constant`() {
        assertThat(TimeWindowBucket.currentBucketId(TimeWindow.AllTime, thursdayNoonUtc)).isEqualTo("all")
    }

    @Test
    fun `daily bucket is the calendar date in the reset zone`() {
        val bucket = TimeWindowBucket.currentBucketId(TimeWindow.Daily(TimeZone.UTC), thursdayNoonUtc)

        assertThat(bucket).isEqualTo("2026-07-09")
    }

    @Test
    fun `weekly bucket is the same for every day in that iso week`() {
        val monday = TimeWindowBucket.currentBucketId(TimeWindow.Weekly(TimeZone.UTC), Instant.parse("2026-07-06T00:00:00Z"))
        val thursday = TimeWindowBucket.currentBucketId(TimeWindow.Weekly(TimeZone.UTC), thursdayNoonUtc)
        val sunday = TimeWindowBucket.currentBucketId(TimeWindow.Weekly(TimeZone.UTC), Instant.parse("2026-07-12T23:00:00Z"))

        assertThat(monday).isEqualTo("wk-2026-07-06")
        assertThat(thursday).isEqualTo("wk-2026-07-06")
        assertThat(sunday).isEqualTo("wk-2026-07-06")
    }

    @Test
    fun `weekly bucket differs across the week boundary`() {
        val thisWeek = TimeWindowBucket.currentBucketId(TimeWindow.Weekly(TimeZone.UTC), Instant.parse("2026-07-12T23:59:00Z"))
        val nextWeek = TimeWindowBucket.currentBucketId(TimeWindow.Weekly(TimeZone.UTC), Instant.parse("2026-07-13T00:01:00Z"))

        assertThat(thisWeek).isNotEqualTo(nextWeek)
    }

    @Test
    fun `monthly bucket is year-month in the reset zone`() {
        val bucket = TimeWindowBucket.currentBucketId(TimeWindow.Monthly(TimeZone.UTC), thursdayNoonUtc)

        assertThat(bucket).isEqualTo("2026-07")
    }

    @Test
    fun `season bucket is derived from the season id, not the clock`() {
        val season = TimeWindow.Season(
            seasonId = "s1",
            range = Instant.parse("2026-01-01T00:00:00Z")..Instant.parse("2026-03-01T00:00:00Z"),
        )

        assertThat(TimeWindowBucket.currentBucketId(season, thursdayNoonUtc)).isEqualTo("season-s1")
    }

    @Test
    fun `custom bucket is derived from the range boundaries`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val end = Instant.parse("2026-03-01T00:00:00Z")
        val custom = TimeWindow.Custom(range = start..end)

        val bucket = TimeWindowBucket.currentBucketId(custom, thursdayNoonUtc)

        assertThat(bucket).isEqualTo("custom-${start.epochSeconds}-${end.epochSeconds}")
    }
}
