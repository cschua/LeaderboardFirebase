package com.leaderboardkit.domain.model

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * The time range a board's scores are drawn from, and when it resets.
 *
 * Reset boundaries for [Daily]/[Weekly]/[Monthly] are computed relative to
 * [resetTimeZone] by the data layer (e.g. midnight local time in that zone),
 * not by the domain layer — the domain layer only carries the declared window
 * and its reset zone.
 */
sealed interface TimeWindow {

    val resetTimeZone: TimeZone?

    data object AllTime : TimeWindow {
        override val resetTimeZone: TimeZone? = null
    }

    data class Daily(override val resetTimeZone: TimeZone) : TimeWindow

    data class Weekly(override val resetTimeZone: TimeZone) : TimeWindow

    data class Monthly(override val resetTimeZone: TimeZone) : TimeWindow

    /** A named, fixed-duration event such as a competitive season. */
    data class Season(
        val seasonId: String,
        val range: ClosedRange<Instant>,
    ) : TimeWindow {
        override val resetTimeZone: TimeZone? = null
    }

    /** An arbitrary, caller-supplied [Instant] range. */
    data class Custom(val range: ClosedRange<Instant>) : TimeWindow {
        override val resetTimeZone: TimeZone? = null
    }
}
