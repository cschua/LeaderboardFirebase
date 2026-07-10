package com.leaderboardkit.data.ratelimit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * A minimal client-side guard against submit-score spam, keyed by (userId, boardId).
 *
 * This is a UX/cost safeguard, not an anti-cheat mechanism — a modified client can
 * trivially bypass it. Any score that must be trusted (competitive leaderboards,
 * anything with real-world stakes) needs server-side validation, which is exactly
 * what [com.leaderboardkit.data.firestore.CloudFunctionScoreSubmitter] is for; see
 * its KDoc for the direct-write-vs-Cloud-Function tradeoff.
 */
class ClientRateLimiter(private val minInterval: Duration) {

    private val mutex = Mutex()
    private val lastSubmissionAt = mutableMapOf<String, TimeSource.Monotonic.ValueTimeMark>()
    private val timeSource = TimeSource.Monotonic

    /**
     * Returns `null` if a submission for [userId]/[boardId] is currently allowed
     * (and records this attempt as the new "last submission"), or the remaining
     * cooldown [Duration] if the caller should back off.
     */
    suspend fun tryAcquire(userId: String, boardId: String): Duration? = mutex.withLock {
        val key = "$userId:$boardId"
        val now = timeSource.markNow()
        val last = lastSubmissionAt[key]
        if (last != null) {
            val elapsed = now - last
            if (elapsed < minInterval) {
                return@withLock minInterval - elapsed
            }
        }
        lastSubmissionAt[key] = now
        null
    }
}
