package com.leaderboardkit.sampleretro.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.leaderboardkit.sampleretro.ui.theme.RetroCyan
import com.leaderboardkit.sampleretro.ui.theme.RetroMonoFont
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
 * Weekly-tab-only "NEXT LEVEL IN..." countdown — same UTC-Monday epoch-day math as
 * `:sample`'s `ResetCountdown` (see its KDoc for why this isn't part of the
 * library itself: the window-bucket reset math is an internal data-layer detail,
 * not public API).
 */
@Composable
fun RetroResetCountdown(modifier: Modifier = Modifier) {
    var remaining by remember { mutableStateOf(timeUntilNextWeeklyResetUtc()) }

    LaunchedEffect(Unit) {
        while (true) {
            remaining = timeUntilNextWeeklyResetUtc()
            delay(1_000)
        }
    }

    Text(
        text = "NEXT LEVEL IN ${formatCountdown(remaining)}",
        style = TextStyle(fontFamily = RetroMonoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetroCyan),
        modifier = modifier,
    )
}

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
