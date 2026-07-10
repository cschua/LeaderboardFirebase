package com.leaderboardkit.sample.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Ticking "resets in ..." label for a [kotlinx.datetime.TimeZone.UTC]-anchored
 * weekly board. `:leaderboard:data`'s own window-bucket math (which this mirrors)
 * is an internal implementation detail, not part of the public API — a host app
 * that wants a reset countdown computes it itself, exactly like this.
 */
@Composable
fun ResetCountdown(modifier: Modifier = Modifier) {
    var remaining by remember { mutableStateOf(timeUntilNextWeeklyResetUtc()) }

    LaunchedEffect(Unit) {
        while (true) {
            remaining = timeUntilNextWeeklyResetUtc()
            delay(1_000)
        }
    }

    Text(text = "Resets in ${formatCountdown(remaining)}", style = MaterialTheme.typography.labelLarge, modifier = modifier)
}

/**
 * Epoch-day arithmetic rather than `LocalDate.dayOfWeek` — on Android that
 * property returns a `java.time.DayOfWeek`, which requires API 26 and isn't
 * available on this sample's minSdk 24 without core library desugaring.
 * 1970-01-01 (epoch day 0) was a Thursday, i.e. 3 days after Monday.
 */
private fun timeUntilNextWeeklyResetUtc(): Duration {
    val now = Clock.System.now()
    val today = now.toLocalDateTime(TimeZone.UTC).date
    val epochDay = today.toEpochDays()
    val daysSinceMonday = (((epochDay + 3) % 7) + 7) % 7
    val daysUntilNextMonday = if (daysSinceMonday == 0L) 7L else 7L - daysSinceMonday
    val nextMonday = LocalDate.fromEpochDays(epochDay + daysUntilNextMonday)
    val resetInstant = nextMonday.atStartOfDayIn(TimeZone.UTC)
    return (resetInstant - now).coerceAtLeast(ZERO)
}

private fun formatCountdown(duration: Duration): String {
    val days = duration.inWholeDays
    val hours = (duration - days.days).inWholeHours
    val minutes = (duration - days.days - hours.hours).inWholeMinutes
    val seconds = (duration - days.days - hours.hours - minutes.minutes).inWholeSeconds
    return "%dd %02dh %02dm %02ds".format(days, hours, minutes, seconds)
}
