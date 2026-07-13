package com.leaderboardkit.data.common

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.TimeWindow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Time remaining until this [TimeWindow]'s next periodic reset, evaluated at
 * [now] and anchored to the window's own `resetTimeZone`. `null` for
 * [TimeWindow.AllTime]/[TimeWindow.Season]/[TimeWindow.Custom], which don't
 * reset on a periodic boundary the way [TimeWindow.Daily]/[TimeWindow.Weekly]/
 * [TimeWindow.Monthly] do.
 *
 * Mirrors [TimeWindowBucket]'s own bucket-boundary math — both belong in the
 * data layer per [TimeWindow]'s KDoc ("computed... by the data layer, not the
 * domain layer"). `:leaderboard:public-api` re-exposes this (see its
 * `timeUntilNextReset`) for host apps that only depend on it; callers already
 * on `:leaderboard:data` (advanced/internal-API usage, e.g. `:sampleRetro`)
 * can call this directly.
 */
@InternalLeaderboardKitApi
fun TimeWindow.timeUntilNextReset(now: Instant = Clock.System.now()): Duration? = when (this) {
    is TimeWindow.AllTime, is TimeWindow.Season, is TimeWindow.Custom -> null
    is TimeWindow.Daily -> durationUntilStartOf(startOfNextDay(resetTimeZone, now), resetTimeZone, now)
    is TimeWindow.Weekly -> durationUntilStartOf(startOfNextWeek(resetTimeZone, now), resetTimeZone, now)
    is TimeWindow.Monthly -> durationUntilStartOf(startOfNextMonth(resetTimeZone, now), resetTimeZone, now)
}

/** Formats [duration] as `"1d 02h 03m 04s"` for a ticking countdown label. */
@InternalLeaderboardKitApi
fun formatCountdown(duration: Duration): String {
    val days = duration.inWholeDays
    val hours = (duration - days.days).inWholeHours
    val minutes = (duration - days.days - hours.hours).inWholeMinutes
    val seconds = (duration - days.days - hours.hours - minutes.minutes).inWholeSeconds
    return "%dd %02dh %02dm %02ds".format(days, hours, minutes, seconds)
}

private fun durationUntilStartOf(date: LocalDate, zone: TimeZone, now: Instant): Duration =
    (date.atStartOfDayIn(zone) - now).coerceAtLeast(ZERO)

private fun startOfNextDay(zone: TimeZone, now: Instant): LocalDate =
    LocalDate.fromEpochDays(TimeWindowBucket.dateIn(zone, now).toEpochDays() + 1)

/** Same "today is Monday -> next reset is a week away, not today" rule as [TimeWindowBucket]'s own week-bucket math. */
private fun startOfNextWeek(zone: TimeZone, now: Instant): LocalDate {
    val epochDay = TimeWindowBucket.dateIn(zone, now).toEpochDays()
    val daysSinceMonday = (((epochDay + 3) % 7) + 7) % 7
    val daysUntilNextMonday = if (daysSinceMonday == 0L) 7L else 7L - daysSinceMonday
    return LocalDate.fromEpochDays(epochDay + daysUntilNextMonday)
}

private fun startOfNextMonth(zone: TimeZone, now: Instant): LocalDate {
    val date = TimeWindowBucket.dateIn(zone, now)
    return if (date.monthNumber == 12) LocalDate(date.year + 1, 1, 1) else LocalDate(date.year, date.monthNumber + 1, 1)
}
