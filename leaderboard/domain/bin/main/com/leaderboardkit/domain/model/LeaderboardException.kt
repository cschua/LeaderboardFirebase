package com.leaderboardkit.domain.model

import kotlin.time.Duration

/** Marker for failures a UI layer may want to branch on specifically, as opposed
 * to a generic backend/network failure. Wrapped in [Result.failure] by repository
 * methods rather than thrown, so callers opt in to handling them.
 */
sealed class LeaderboardException(message: String) : Exception(message) {

    /** A client-side rate limit (see score-submission docs in `:leaderboard:data`) was hit. */
    class RateLimitExceeded(val retryAfter: Duration) :
        LeaderboardException("Score submission rate-limited, retry after $retryAfter")

    /** The requested user has no entry on this board. */
    class UserNotFound(val userId: String) :
        LeaderboardException("No leaderboard entry for user $userId")

    /** The backend request timed out. */
    class NetworkTimeout(message: String = "Request timed out") : LeaderboardException(message)
}
