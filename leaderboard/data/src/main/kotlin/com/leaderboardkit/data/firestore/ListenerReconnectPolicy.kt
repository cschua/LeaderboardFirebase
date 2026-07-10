package com.leaderboardkit.data.firestore

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Firestore's offline-persistence layer can serve a snapshot listener's initial
 * event from the local cache when reconnecting, which is fine for short gaps but
 * risks the client acting on data that's meaningfully stale once the gap has been
 * long enough. Google's own guidance is to not assume incremental/cache-consistent
 * behavior past roughly a 30 minute disconnect.
 *
 * This tracks, per board key, when a [RealtimeListener][com.leaderboardkit.domain.model.RefreshStrategy.RealtimeListener]
 * was last known active, so [FirestoreLeaderboardRepository] can force a
 * server-sourced read on (re)attach after a long gap instead of trusting whatever
 * the cache hands back first.
 */
class ListenerReconnectPolicy(private val staleAfter: kotlin.time.Duration = 30.minutes) {

    private val lastActiveAt = mutableMapOf<String, Instant>()

    fun shouldForceServerRead(boardKey: String, now: Instant = Clock.System.now()): Boolean {
        val last = lastActiveAt[boardKey] ?: return false
        return now - last > staleAfter
    }

    fun markActive(boardKey: String, now: Instant = Clock.System.now()) {
        lastActiveAt[boardKey] = now
    }

    fun markInactive(boardKey: String, now: Instant = Clock.System.now()) {
        lastActiveAt[boardKey] = now
    }
}
