package com.leaderboardkit.data.common

import com.leaderboardkit.domain.model.TimeWindow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

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
 *
 * Not part of this module's public surface: host apps customizing path strategy
 * are expected to delegate to [com.leaderboardkit.data.firestore.DefaultFirestorePathStrategy]/
 * [com.leaderboardkit.data.realtimedb.DefaultRealtimeDbPathStrategy] (e.g. wrapping
 * one with a tenant prefix) rather than reimplement bucket computation themselves.
 */
internal object TimeWindowBucket {

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

    /** Visible within `:leaderboard:data` (not `private`) so [timeUntilNextReset] can reuse it without re-deriving the same one-liner. */
    fun dateIn(zone: TimeZone, now: Instant): LocalDate = now.toLocalDateTime(zone).date

    /**
     * Deliberately avoids `LocalDate.dayOfWeek` — on Android that property returns
     * a `java.time.DayOfWeek`, whose members (`.value` included) require API 26
     * and aren't available on this library's minSdk 24 without core library
     * desugaring (which this module doesn't enable). Epoch-day arithmetic is pure
     * kotlinx-datetime, no `java.time` involved, and works down to API 24.
     * 1970-01-01 (epoch day 0) was a Thursday, i.e. 3 days after Monday.
     */
    private fun startOfWeek(zone: TimeZone, now: Instant): LocalDate {
        val epochDay = dateIn(zone, now).toEpochDays()
        val daysSinceMonday = (((epochDay + 3) % 7) + 7) % 7
        return LocalDate.fromEpochDays(epochDay - daysSinceMonday)
    }
}
