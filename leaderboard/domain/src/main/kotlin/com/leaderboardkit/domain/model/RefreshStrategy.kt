package com.leaderboardkit.domain.model

import kotlin.time.Duration

/**
 * Controls how a board's data is kept up to date, and is the primary cost-control
 * knob for Firestore-backed boards: a real-time snapshot listener bills a read for
 * every added/updated/removed document in the observed result set, so an always-on
 * listener over a large or frequently-changing result set is the single biggest
 * cost lever in this library.
 *
 * Recommended per-scope defaults (overridable — see `LeaderboardConfigBuilder`):
 *
 * | Board type                          | Default                          | Rationale |
 * |--------------------------------------|-----------------------------------|-----------|
 * | Global / all-time                    | Polling(45-60s)                  | Large, high-churn result set; sub-minute rank precision rarely matters |
 * | Friends                              | RealtimeListener                 | Small, bounded result set, so live cost stays low regardless of churn |
 * | Weekly / seasonal / category         | Polling(2-5min) + hard recompute at window expiry | Matches the board lifecycle: continuous during the window, one-time reset |
 * | Current-user rank + surrounding ±N   | RealtimeListener                 | Small, bounded read cost where responsiveness matters most |
 */
sealed interface RefreshStrategy {

    /**
     * Live Firestore snapshot listener. Use only for small, bounded result sets
     * (e.g. a friends list, or a narrow ±N surrounding-rank window around the
     * current user) where read volume stays low regardless of churn.
     */
    data object RealtimeListener : RefreshStrategy

    /**
     * One-shot fetch on a timer (or on demand). Default choice for global/all-time
     * and category boards — feels live to the player without paying for a live
     * listener over a large, frequently-churning result set.
     */
    data class Polling(val interval: Duration) : RefreshStrategy

    /**
     * Fetch once on screen entry / explicit pull-to-refresh only. No background
     * cost between refreshes. Best for large seasonal boards where the meaningful
     * event is the window reset, not incremental rank movement.
     */
    data object ManualOnly : RefreshStrategy
}
