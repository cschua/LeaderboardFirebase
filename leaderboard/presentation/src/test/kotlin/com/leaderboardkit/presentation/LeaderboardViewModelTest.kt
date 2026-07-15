@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.data.fake.FakeLeaderboardRepository
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.LeaderboardScope
import com.leaderboardkit.domain.model.leaderboardConfig
import com.leaderboardkit.domain.repository.LeaderboardRepository
import com.leaderboardkit.domain.usecase.GetNearbyRanksUseCase
import com.leaderboardkit.domain.usecase.LoadMoreUseCase
import com.leaderboardkit.domain.usecase.ObserveLeaderboardUseCase
import com.leaderboardkit.domain.usecase.SubmitScoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Orchestration-level coverage on top of [LeaderboardReducerTest]'s pure-function
 * cases: wires a real [FakeLeaderboardRepository] through the actual use cases so
 * these exercise [LeaderboardViewModel]'s side-effect plumbing, still with zero
 * network/Firebase involvement.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun entry(userId: String, score: Long) =
        LeaderboardEntry(userId, displayName = userId, avatarId = "avatar_01", score = score, rank = null)

    /** Delegates everything to [delegate] except [observeEntries], which throws for any config whose [LeaderboardConfig.scope] is [failingScope]. */
    private class FlakyRepository(
        private val delegate: LeaderboardRepository,
        private val failingScope: LeaderboardScope,
    ) : LeaderboardRepository by delegate {
        override fun observeEntries(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> =
            if (config.scope == failingScope) flow { throw IllegalStateException("boom") } else delegate.observeEntries(config)
    }

    /** Delegates everything to [delegate] except [submitScore], which fails for [failingBoardId]. */
    private class SubmitFailingRepository(
        private val delegate: LeaderboardRepository,
        private val failingBoardId: String,
    ) : LeaderboardRepository by delegate {
        override suspend fun submitScore(userId: String, score: Long, config: LeaderboardConfig, metadata: Map<String, Any>): Result<Unit> =
            if (config.boardId == failingBoardId) Result.failure(RuntimeException("boom")) else delegate.submitScore(userId, score, config, metadata)
    }

    private fun viewModel(repository: LeaderboardRepository, currentUserId: String = "me") = LeaderboardViewModel(
        initialConfig = leaderboardConfig("board") { pageSize = 5 },
        currentUserId = currentUserId,
        dependencies = LeaderboardDependencies(
            observeLeaderboard = ObserveLeaderboardUseCase(repository),
            loadMore = LoadMoreUseCase(repository),
            submitScore = SubmitScoreUseCase(repository),
            getNearbyRanks = GetNearbyRanksUseCase(repository),
        ),
    )

    @Test
    fun `initial state loads entries and resolves an in-window current user`() = runTest {
        val repository = FakeLeaderboardRepository(listOf(entry("me", 100), entry("other", 50)))
        val vm = viewModel(repository, currentUserId = "me")

        vm.state.test {
            var latest = awaitItem()
            assertThat(latest.isLoading).isTrue()
            while (latest.entries.isEmpty()) latest = awaitItem()
            assertThat(latest.isLoading).isFalse()
            assertThat(latest.entries.map { it.userId }).containsExactly("me", "other").inOrder()
            assertThat(latest.currentUserEntry?.userId).isEqualTo("me")
        }
    }

    @Test
    fun `current user off-screen is resolved via getNearbyRanks`() = runTest {
        val entries = (1..20).map { entry("u$it", (100 - it).toLong()) } + entry("me", 1)
        val repository = FakeLeaderboardRepository(entries)
        val vm = viewModel(repository, currentUserId = "me")

        vm.state.test {
            var latest = awaitItem()
            while (latest.currentUserEntry == null) latest = awaitItem()
            assertThat(latest.currentUserEntry.userId).isEqualTo("me")
            assertThat(latest.entries.map { it.userId }).doesNotContain("me")
        }
    }

    @Test
    fun `Refresh flips isLoading and reloads even when the config is unchanged`() = runTest {
        val repository = FakeLeaderboardRepository(listOf(entry("me", 10)))
        val vm = viewModel(repository, currentUserId = "me")

        vm.state.test {
            var latest = awaitItem()
            while (latest.entries.isEmpty()) latest = awaitItem()
            assertThat(latest.isLoading).isFalse()

            vm.onIntent(LeaderboardIntent.Refresh)

            latest = awaitItem()
            assertThat(latest.isLoading).isTrue()

            while (latest.isLoading) latest = awaitItem()
            assertThat(latest.entries.map { it.userId }).containsExactly("me")
        }
    }

    @Test
    fun `LoadMore grows entries and flips canLoadMore once exhausted`() = runTest {
        val repository = FakeLeaderboardRepository((1..7).map { entry("u$it", it.toLong()) })
        val vm = viewModel(repository, currentUserId = "u1")

        vm.state.test {
            var latest = awaitItem()
            while (latest.entries.size < 5) latest = awaitItem()

            vm.onIntent(LeaderboardIntent.LoadMore)
            while (latest.entries.size < 7) latest = awaitItem()
            assertThat(latest.canLoadMore).isTrue()

            vm.onIntent(LeaderboardIntent.LoadMore)
            latest = awaitItem()
            while (latest.isLoadingMore) latest = awaitItem()
            assertThat(latest.canLoadMore).isFalse()
        }
    }

    @Test
    fun `SubmitScore success emits ScrollToUserRank`() = runTest {
        val repository = FakeLeaderboardRepository(listOf(entry("me", 10)))
        val vm = viewModel(repository, currentUserId = "me")

        vm.effects.test {
            vm.onIntent(LeaderboardIntent.SubmitScore(999L))
            assertThat(awaitItem()).isEqualTo(LeaderboardEffect.ScrollToUserRank)
        }
    }

    @Test
    fun `SubmitScoreToWindows success submits to every config and emits ScrollToUserRank`() = runTest {
        val repository = FakeLeaderboardRepository(listOf(entry("me", 10)))
        val vm = viewModel(repository, currentUserId = "me")
        val configs = listOf(
            leaderboardConfig("board") { pageSize = 5 },
            leaderboardConfig("board") { pageSize = 10 },
        )

        vm.effects.test {
            vm.onIntent(LeaderboardIntent.SubmitScoreToWindows(999L, configs))
            assertThat(awaitItem()).isEqualTo(LeaderboardEffect.ScrollToUserRank)
        }
    }

    @Test
    fun `SubmitScoreToWindows failure dispatches ShowError instead of ScrollToUserRank`() = runTest {
        val repository = SubmitFailingRepository(FakeLeaderboardRepository(listOf(entry("me", 10))), failingBoardId = "bad_board")
        val vm = viewModel(repository, currentUserId = "me")
        val configs = listOf(leaderboardConfig("bad_board") {})

        vm.effects.test {
            vm.onIntent(LeaderboardIntent.SubmitScoreToWindows(999L, configs))
            assertThat(awaitItem()).isInstanceOf(LeaderboardEffect.ShowError::class.java)
        }
    }

    @Test
    fun `ChangeScope resets the board and reloads under the new config`() = runTest {
        val repository = FakeLeaderboardRepository(listOf(entry("me", 10)))
        val vm = viewModel(repository, currentUserId = "me")

        vm.state.test {
            var latest = awaitItem()
            while (latest.entries.isEmpty()) latest = awaitItem()

            vm.onIntent(LeaderboardIntent.ChangeScope(LeaderboardScope.Category("boss_rush")))

            latest = awaitItem()
            assertThat(latest.config.scope).isEqualTo(LeaderboardScope.Category("boss_rush"))
            assertThat(latest.isLoading).isTrue()
        }
    }

    @Test
    fun `an error on one config does not block entries loading after a later config change`() = runTest {
        val failingScope = LeaderboardScope.Category("boom")
        val repository = FlakyRepository(FakeLeaderboardRepository(listOf(entry("me", 10))), failingScope)
        val vm = viewModel(repository, currentUserId = "me")

        vm.state.test {
            var latest = awaitItem()
            while (latest.entries.isEmpty()) latest = awaitItem()

            vm.onIntent(LeaderboardIntent.ChangeScope(failingScope))
            latest = awaitItem()
            while (latest.error == null) latest = awaitItem()
            assertThat(latest.isLoading).isFalse()

            // The board this errored on isn't the only board this ViewModel will ever
            // show — a live app keeps running after a transient read failure, and the
            // *next* config change must still be able to load entries. If `.catch` ever
            // regresses to wrapping the whole `flatMapLatest` chain again (see
            // `entriesFlow` KDoc), this config change would spin on `isLoading` forever.
            vm.onIntent(LeaderboardIntent.ChangeScope(LeaderboardScope.Global))
            latest = awaitItem()
            assertThat(latest.isLoading).isTrue()
            while (latest.entries.isEmpty()) latest = awaitItem()
            assertThat(latest.entries.map { it.userId }).containsExactly("me")
        }
    }
}
