@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.data.fake

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.LeaderboardException
import com.leaderboardkit.domain.model.leaderboardConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeLeaderboardRepositoryTest {

    /** Deterministically cycles through the fixed avatar placeholder set — see [AvatarDefaults]. */
    private fun entry(userId: String, score: Long) = LeaderboardEntry(
        userId = userId,
        displayName = userId,
        avatarId = AvatarDefaults.PLACEHOLDER_AVATAR_IDS[(score % AvatarDefaults.PLACEHOLDER_AVATAR_IDS.size).toInt()],
        score = score,
        rank = null,
    )

    @Test
    fun `observeEntries sorts descending and assigns ranks`() = runTest {
        val repository = FakeLeaderboardRepository(
            listOf(entry("low", 10), entry("high", 100), entry("mid", 50)),
        )
        val config = leaderboardConfig("board") { pageSize = 10 }

        repository.observeEntries(config).test {
            val page = awaitItem()
            assertThat(page.map { it.userId }).containsExactly("high", "mid", "low").inOrder()
            assertThat(page.map { it.rank }).containsExactly(1, 2, 3).inOrder()
            page.forEach { assertThat(AvatarDefaults.PLACEHOLDER_AVATAR_IDS).contains(it.avatarId) }
        }
    }

    @Test
    fun `loadMore grows the emitted window until exhausted`() = runTest {
        val repository = FakeLeaderboardRepository((1..5).map { entry("u$it", it.toLong()) })
        val config = leaderboardConfig("board") { pageSize = 2 }

        repository.observeEntries(config).test { assertThat(awaitItem()).hasSize(2) }

        assertThat(repository.loadMore(config).getOrNull()).isTrue()
        repository.observeEntries(config).test { assertThat(awaitItem()).hasSize(4) }

        assertThat(repository.loadMore(config).getOrNull()).isTrue()
        repository.observeEntries(config).test { assertThat(awaitItem()).hasSize(5) }

        // All 5 entries loaded -> no more pages left.
        assertThat(repository.loadMore(config).getOrNull()).isFalse()
    }

    @Test
    fun `overrideUserRank forces getUserRank regardless of natural sort`() = runTest {
        val repository = FakeLeaderboardRepository(listOf(entry("u1", 5)))
        repository.overrideUserRank("u1", 9999)

        val result = repository.getUserRank("u1", leaderboardConfig("board") {})

        assertThat(result.getOrNull()).isEqualTo(9999)
    }

    @Test
    fun `getSurroundingEntries returns a radius window centered on the user`() = runTest {
        val repository = FakeLeaderboardRepository((1..10).map { entry("u$it", it.toLong()) })
        val config = leaderboardConfig("board") { }

        // u10 has the highest score -> rank 1 under descending sort.
        val result = repository.getSurroundingEntries("u7", radius = 2, config).getOrNull()

        assertThat(result?.map { it.userId }).containsExactly("u9", "u8", "u7", "u6", "u5").inOrder()
    }

    @Test
    fun `getSurroundingEntries returns UserNotFound when the anchor user is missing`() = runTest {
        val repository = FakeLeaderboardRepository(listOf(entry("u1", 10)))
        val config = leaderboardConfig("board") { }

        val result = repository.getSurroundingEntries("missing", radius = 1, config)

        assertThat(result.exceptionOrNull()).isInstanceOf(LeaderboardException.UserNotFound::class.java)
    }

    @Test
    fun `submitScore upserts and re-sorts`() = runTest {
        val repository = FakeLeaderboardRepository(listOf(entry("u1", 10)))
        val config = leaderboardConfig("board") { }

        repository.submitScore("u2", 999L, config)

        val rank = repository.getUserRank("u2", config).getOrNull()
        assertThat(rank).isEqualTo(1)
    }
}
