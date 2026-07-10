package com.leaderboardkit.data.common

import com.leaderboardkit.domain.model.TimeWindow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Computes a stable, filesystem/path-safe identifier for the *current* bucket of a
 * [TimeWindow], e.g. "2026-07-09" for a daily window evaluated today, or
 * "wk-2026-07-06" for the weekly window that day falls in.
 *
 * Path strategies use this to give each window its own Firestore
 * collection/Realtime Database node (`.../weekly_coins/wk-2026-07-06/entries`)
 * instead of one ever-growing collection that needs score purging. This directly
 * satisfies the "hard recompute at window expiration" behavior recommended for
 * weekly/seasonal boards: once a window ends, its bucket is simply never queried
 * again — no delete/reset pass is needed on the hot path (a host app may still want
 * a scheduled job to prune old buckets for storage cost, but that is decoupled
 * from correctness).
 */
object TimeWindowBucket {

    fun currentBucketId(timeWindow: TimeWindow, now: Instant = Clock.System.now()): String = when (timeWindow) {
        is TimeWindow.AllTime -> "all"
        is TimeWindow.Daily -> dateIn(timeWindow.resetTimeZone, now).toString()
        is TimeWindow.Weekly -> "wk-" + startOfWeek(timeWindow.resetTimeZone, now).toString()
        is TimeWindow.Monthly -> {
            val date = dateIn(timeWindow.resetTimeZone, now)
            "%04d-%02d".format(date.year, date.monthNumber)
        }
        is TimeWindow.Season -> "season-${timeWindow.seasonId}"
        is TimeWindow.Custom -> "custom-${timeWindow.range.start.epochSeconds}-${timeWindow.range.endInclusive.epochSeconds}"
    }

    private fun dateIn(zone: TimeZone, now: Instant): LocalDate = now.toLocalDateTime(zone).date

    private fun startOfWeek(zone: TimeZone, now: Instant): LocalDate {
        val today = dateIn(zone, now)
        val daysSinceMonday = today.dayOfWeek.value - 1 // java.time.DayOfWeek: MONDAY.value == 1
        return today.minus(DatePeriod(days = daysSinceMonday))
    }
}
