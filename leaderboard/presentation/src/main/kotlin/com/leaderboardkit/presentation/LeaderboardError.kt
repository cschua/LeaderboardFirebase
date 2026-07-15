package com.leaderboardkit.presentation

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardException
import kotlin.time.Duration

/**
 * UI-facing failure classification. The reducer and composables only ever see
 * this type — raw [Throwable]s/`LeaderboardException`s from the data layer are
 * mapped to it once, at the point they're caught (see `Throwable.toLeaderboardError()`),
 * and never leak further up.
 */
@InternalLeaderboardKitApi
sealed interface LeaderboardError {
    data class RateLimited(val retryAfter: Duration) : LeaderboardError
    data object UserNotFound : LeaderboardError
    data class Unknown(val message: String) : LeaderboardError
}

/** Short, human-readable text for [LeaderboardEffect.ShowError] / default error state content. */
@InternalLeaderboardKitApi
fun LeaderboardError.toDisplayMessage(): String = when (this) {
    is LeaderboardError.RateLimited -> "You're submitting scores too quickly. Try again in ${retryAfter.inWholeSeconds}s."
    is LeaderboardError.UserNotFound -> "No leaderboard entry found."
    is LeaderboardError.Unknown -> message.ifBlank { "Something went wrong." }
}

@OptIn(InternalLeaderboardKitApi::class)
internal fun Throwable.toLeaderboardError(): LeaderboardError = when (this) {
    is LeaderboardException.RateLimitExceeded -> LeaderboardError.RateLimited(retryAfter)
    is LeaderboardException.UserNotFound -> LeaderboardError.UserNotFound
    else -> LeaderboardError.Unknown(message ?: this::class.simpleName ?: "Unknown error")
}
