@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.data.fake.FakeLeaderboardRepository
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardScope
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.domain.repository.LeaderboardRepository
import com.leaderboardkit.domain.usecase.GetNearbyRanksUseCase
import com.leaderboardkit.domain.usecase.LoadMoreUseCase
import com.leaderboardkit.domain.usecase.ObserveLeaderboardUseCase
import com.leaderboardkit.domain.usecase.SubmitScoreUseCase
import com.leaderboardkit.presentation.LeaderboardDependencies
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Exercises [LeaderboardClient]'s own thin wrapper methods directly — the ViewModel
 * plumbing they delegate to is already covered by [LeaderboardDependencies]/use-case
 * tests, so this stays focused on what [LeaderboardClient] itself is responsible for:
 * pre-filling [LeaderboardKitConfig.defaultScope], sourcing the current user id from
 * [LeaderboardKitConfig.currentUserId] rather than a caller-supplied one, and
 * delegating [LeaderboardClient.timeUntilNextReset] to the config's time window.
 */
class LeaderboardClientTest {

    private fun client(currentUserId: String = "me", repository: LeaderboardRepository = FakeLeaderboardRepository()) = LeaderboardClient(
        config = LeaderboardKitConfig(currentUserId = { currentUserId }, defaultScope = LeaderboardScope.Category("boss_rush")),
        dependencies = LeaderboardDependencies(
            observeLeaderboard = ObserveLeaderboardUseCase(repository),
            loadMore = LoadMoreUseCase(repository),
            submitScore = SubmitScoreUseCase(repository),
            getNearbyRanks = GetNearbyRanksUseCase(repository),
        ),
    )

    @Test
    fun `buildConfig pre-fills the default scope`() {
        val config = client().buildConfig("weekly_coins")

        assertThat(config.boardId).isEqualTo("weekly_coins")
        assertThat(config.scope).isEqualTo(LeaderboardScope.Category("boss_rush"))
    }

    @Test
    fun `buildConfig lets the block override the default scope`() {
        val config = client().buildConfig("weekly_coins") { scope = LeaderboardScope.Global }

        assertThat(config.scope).isEqualTo(LeaderboardScope.Global)
    }

    @Test
    fun `submitScore uses the client's own current user id, not a caller-supplied one`() = runTest {
        val repository = FakeLeaderboardRepository()
        val leaderboardClient = client(currentUserId = "me", repository = repository)
        val config = leaderboardClient.buildConfig("board")

        leaderboardClient.submitScore(config, 500L)

        assertThat(repository.getUserRank("me", config).getOrNull()).isEqualTo(1)
    }

    @Test
    fun `timeUntilNextReset delegates to the config's time window`() {
        val leaderboardClient = client()
        val config = leaderboardClient.buildConfig("board") { timeWindow = TimeWindow.Weekly(TimeZone.UTC) }
        // Wednesday 2026-07-08 12:00 UTC -> next reset is Monday 2026-07-13 00:00 UTC.
        val now = Instant.parse("2026-07-08T12:00:00Z")

        val remaining = leaderboardClient.timeUntilNextReset(config, now)

        assertThat(remaining).isEqualTo(4.days + 12.hours)
    }

    @Test
    fun `timeUntilNextReset is null for boards that never reset`() {
        val leaderboardClient = client()
        val config = leaderboardClient.buildConfig("board") { timeWindow = TimeWindow.AllTime }

        assertThat(leaderboardClient.timeUntilNextReset(config)).isNull()
    }

    @Test
    fun `formatResetCountdown pads hours minutes and seconds to two digits`() {
        assertThat(formatResetCountdown(4.days + 2.hours + 5.minutes + 9.seconds)).isEqualTo("4d 02h 05m 09s")
    }
}
