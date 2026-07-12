@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.presentation

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry

/**
 * The result of a side effect (a use-case call), fed into [reduceLeaderboardState].
 *
 * [LeaderboardIntent] and [LeaderboardChange] are deliberately separate types:
 * an intent like [LeaderboardIntent.Refresh] triggers a suspend use-case call
 * (a side effect, which cannot be pure), and *that call's outcome* is what the
 * pure reducer consumes. This is what "reduction is a pure function; side effects
 * never live in the reducer" means in practice — [LeaderboardViewModel] owns the
 * side effects, [reduceLeaderboardState] only ever does data transformation.
 */
internal sealed interface LeaderboardChange {
    data object LoadingStarted : LeaderboardChange
    data class EntriesLoaded(val entries: List<LeaderboardEntry>) : LeaderboardChange
    data class CurrentUserEntryResolved(val entry: LeaderboardEntry?) : LeaderboardChange
    data object LoadMoreStarted : LeaderboardChange
    data class LoadMoreFinished(val hasMore: Boolean) : LeaderboardChange
    data class ConfigChanged(val config: LeaderboardConfig) : LeaderboardChange
    data class Failed(val error: LeaderboardError) : LeaderboardChange
}

/**
 * `(State, Change) -> State`. Pure: no suspension, no I/O, no reference to use
 * cases or the current time — every branch is a deterministic data transform, which
 * is what makes it testable without a backend (see `LeaderboardReducerTest`).
 */
internal fun reduceLeaderboardState(state: LeaderboardState, change: LeaderboardChange): LeaderboardState =
    when (change) {
        LeaderboardChange.LoadingStarted ->
            state.copy(isLoading = true, error = null)

        is LeaderboardChange.EntriesLoaded ->
            state.copy(entries = change.entries, isLoading = false, error = null)

        is LeaderboardChange.CurrentUserEntryResolved ->
            state.copy(currentUserEntry = change.entry)

        LeaderboardChange.LoadMoreStarted ->
            state.copy(isLoadingMore = true)

        is LeaderboardChange.LoadMoreFinished ->
            state.copy(isLoadingMore = false, canLoadMore = change.hasMore)

        is LeaderboardChange.ConfigChanged ->
            state.copy(
                config = change.config,
                entries = emptyList(),
                currentUserEntry = null,
                isLoading = true,
                isLoadingMore = false,
                canLoadMore = true,
                error = null,
            )

        is LeaderboardChange.Failed ->
            state.copy(isLoading = false, isLoadingMore = false, error = change.error)
    }
