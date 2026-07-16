package com.leaderboardkit.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.RecordingLeaderboardRepository
import com.leaderboardkit.domain.testConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LoadMoreUseCaseTest {

    @Test
    fun `returns true when a page was appended`() = runTest {
        val repository = RecordingLeaderboardRepository(loadMoreResult = Result.success(true))
        val useCase = LoadMoreUseCase(repository)

        val result = useCase(testConfig())

        assertThat(result.getOrNull()).isTrue()
    }

    @Test
    fun `returns false once the board is exhausted`() = runTest {
        val repository = RecordingLeaderboardRepository(loadMoreResult = Result.success(false))
        val useCase = LoadMoreUseCase(repository)

        val result = useCase(testConfig())

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val failure = RuntimeException("boom")
        val repository = RecordingLeaderboardRepository(loadMoreResult = Result.failure(failure))
        val useCase = LoadMoreUseCase(repository)

        val result = useCase(testConfig())

        assertThat(result.exceptionOrNull()).isEqualTo(failure)
    }
}
