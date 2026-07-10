package com.leaderboardkit.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.RecordingLeaderboardRepository
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.testConfig
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test
import org.junit.Test

class ObserveLeaderboardUseCaseTest {

    private val entry = LeaderboardEntry(
        userId = "u1",
        displayName = "Alice",
        avatarId = "avatar_01",
        score = 100,
        rank = 1,
    )

    @Test
    fun `emits entries from repository`() = runTest {
        val repository = RecordingLeaderboardRepository(entries = listOf(entry))
        val useCase = ObserveLeaderboardUseCase(repository)
        val config = testConfig()

        useCase(config).test {
            assertThat(awaitItem()).containsExactly(entry)
            awaitComplete()
        }
        assertThat(repository.lastObservedConfig).isEqualTo(config)
    }
}
