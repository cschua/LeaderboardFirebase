@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.presentation

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.leaderboardConfig
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Exercises [reduceLeaderboardState] directly — no ViewModel, no coroutines, no
 * repository of any kind. Each case corresponds to the [LeaderboardChange] a real
 * [LeaderboardIntent] produces once its side effect (a use-case call) completes;
 * see [LeaderboardChange] KDoc for why the split exists.
 */
class LeaderboardReducerTest {

    private val config = leaderboardConfig("board") {}
    private val baseState = LeaderboardState(config = config)

    private fun entry(userId: String, score: Long, rank: Int? = null) =
        LeaderboardEntry(userId, displayName = userId, avatarId = "avatar_01", score = score, rank = rank)

    // -- Refresh --------------------------------------------------------

    @Test
    fun `LoadingStarted sets isLoading and clears any prior error`() {
        val dirty = baseState.copy(error = LeaderboardError.Unknown("boom"))

        val result = reduceLeaderboardState(dirty, LeaderboardChange.LoadingStarted)

        assertThat(result.isLoading).isTrue()
        assertThat(result.error).isNull()
    }

    @Test
    fun `EntriesLoaded replaces entries and clears loading and error`() {
        val loading = baseState.copy(isLoading = true, error = LeaderboardError.Unknown("boom"))
        val entries = listOf(entry("u1", 100, 1))

        val result = reduceLeaderboardState(loading, LeaderboardChange.EntriesLoaded(entries))

        assertThat(result.entries).isEqualTo(entries)
        assertThat(result.isLoading).isFalse()
        assertThat(result.error).isNull()
    }

    // -- currentUserEntry -------------------------------------------------

    @Test
    fun `CurrentUserEntryResolved sets currentUserEntry independently of entries`() {
        val me = entry("me", 42, 4502)

        val result = reduceLeaderboardState(baseState, LeaderboardChange.CurrentUserEntryResolved(me))

        assertThat(result.currentUserEntry).isEqualTo(me)
        assertThat(result.entries).isEmpty()
    }

    @Test
    fun `CurrentUserEntryResolved with null clears a stale entry`() {
        val withUser = baseState.copy(currentUserEntry = entry("me", 42, 4502))

        val result = reduceLeaderboardState(withUser, LeaderboardChange.CurrentUserEntryResolved(null))

        assertThat(result.currentUserEntry).isNull()
    }

    // -- LoadMore -----------------------------------------------------------

    @Test
    fun `LoadMoreStarted sets isLoadingMore without touching entries or isLoading`() {
        val result = reduceLeaderboardState(baseState, LeaderboardChange.LoadMoreStarted)

        assertThat(result.isLoadingMore).isTrue()
        assertThat(result.isLoading).isFalse()
    }

    @Test
    fun `LoadMoreFinished with hasMore true keeps canLoadMore true`() {
        val loadingMore = baseState.copy(isLoadingMore = true)

        val result = reduceLeaderboardState(loadingMore, LeaderboardChange.LoadMoreFinished(hasMore = true))

        assertThat(result.isLoadingMore).isFalse()
        assertThat(result.canLoadMore).isTrue()
    }

    @Test
    fun `LoadMoreFinished with hasMore false flips canLoadMore false`() {
        val loadingMore = baseState.copy(isLoadingMore = true, canLoadMore = true)

        val result = reduceLeaderboardState(loadingMore, LeaderboardChange.LoadMoreFinished(hasMore = false))

        assertThat(result.isLoadingMore).isFalse()
        assertThat(result.canLoadMore).isFalse()
    }

    // -- ChangeTimeWindow / ChangeScope (both produce ConfigChanged) --------

    @Test
    fun `ConfigChanged swaps config and resets the board to a fresh loading state`() {
        val populated = baseState.copy(
            entries = listOf(entry("u1", 100, 1)),
            currentUserEntry = entry("me", 42, 4502),
            canLoadMore = false,
            error = LeaderboardError.Unknown("boom"),
        )
        val newConfig = leaderboardConfig("board") { pageSize = 50 }

        val result = reduceLeaderboardState(populated, LeaderboardChange.ConfigChanged(newConfig))

        assertThat(result.config).isEqualTo(newConfig)
        assertThat(result.entries).isEmpty()
        assertThat(result.currentUserEntry).isNull()
        assertThat(result.isLoading).isTrue()
        assertThat(result.canLoadMore).isTrue()
        assertThat(result.error).isNull()
    }

    // -- Failures -------------------------------------------------------------

    @Test
    fun `Failed clears loading flags and sets the error`() {
        val busy = baseState.copy(isLoading = true, isLoadingMore = true)
        val error = LeaderboardError.RateLimited(5.seconds)

        val result = reduceLeaderboardState(busy, LeaderboardChange.Failed(error))

        assertThat(result.isLoading).isFalse()
        assertThat(result.isLoadingMore).isFalse()
        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `Failed does not clear already-loaded entries`() {
        val entries = listOf(entry("u1", 100, 1))
        val loaded = baseState.copy(entries = entries)

        val result = reduceLeaderboardState(loaded, LeaderboardChange.Failed(LeaderboardError.UserNotFound))

        assertThat(result.entries).isEqualTo(entries)
    }
}
