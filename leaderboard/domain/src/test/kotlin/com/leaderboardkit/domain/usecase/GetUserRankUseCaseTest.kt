package com.leaderboardkit.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.RecordingLeaderboardRepository
import com.leaderboardkit.domain.testConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetUserRankUseCaseTest {

    @Test
    fun `returns rank from repository`() = runTest {
        val repository = RecordingLeaderboardRepository(userRank = Result.success(42))
        val useCase = GetUserRankUseCase(repository)

        val result = useCase("u1", testConfig())

        assertThat(result.getOrNull()).isEqualTo(42)
        assertThat(repository.lastRankQuery?.first).isEqualTo("u1")
    }

    @Test
    fun `returns null when user has no entry`() = runTest {
        val repository = RecordingLeaderboardRepository(userRank = Result.success(null))
        val useCase = GetUserRankUseCase(repository)

        val result = useCase("missing", testConfig())

        assertThat(result.getOrNull()).isNull()
        assertThat(result.isSuccess).isTrue()
    }
}
