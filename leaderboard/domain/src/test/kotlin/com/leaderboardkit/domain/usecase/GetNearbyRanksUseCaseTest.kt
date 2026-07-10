package com.leaderboardkit.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.RecordingLeaderboardRepository
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.testConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetNearbyRanksUseCaseTest {

    private val nearby = listOf(
        LeaderboardEntry("u0", "A", "avatar_01", 200, 4),
        LeaderboardEntry("u1", "B", "avatar_02", 150, 5),
        LeaderboardEntry("u2", "C", "avatar_03", 100, 6),
    )

    @Test
    fun `forwards radius and returns surrounding entries`() = runTest {
        val repository = RecordingLeaderboardRepository(nearbyEntries = Result.success(nearby))
        val useCase = GetNearbyRanksUseCase(repository)

        val result = useCase("u1", radius = 1, testConfig())

        assertThat(result.getOrNull()).isEqualTo(nearby)
        assertThat(repository.lastNearbyQuery?.second).isEqualTo(1)
    }

    @Test
    fun `rejects non-positive radius without calling repository`() = runTest {
        val repository = RecordingLeaderboardRepository()
        val useCase = GetNearbyRanksUseCase(repository)

        try {
            useCase("u1", radius = 0, testConfig())
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
        assertThat(repository.lastNearbyQuery).isNull()
    }
}
