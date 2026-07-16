package com.leaderboardkit.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.RecordingLeaderboardRepository
import com.leaderboardkit.domain.testConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SubmitScoreUseCaseTest {

    @Test
    fun `forwards score and metadata to repository`() = runTest {
        val repository = RecordingLeaderboardRepository()
        val useCase = SubmitScoreUseCase(repository)

        val result = useCase("u1", 500L, testConfig(), metadata = mapOf("combo" to 3))

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.lastSubmit).isEqualTo(Triple("u1", 500L, mapOf("combo" to 3)))
    }

    @Test
    fun `rejects blank userId without calling repository`() = runTest {
        val repository = RecordingLeaderboardRepository()
        val useCase = SubmitScoreUseCase(repository)

        try {
            useCase("", 500L, testConfig())
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
        assertThat(repository.lastSubmit).isNull()
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val failure = RuntimeException("boom")
        val repository = RecordingLeaderboardRepository(submitResult = Result.failure(failure))
        val useCase = SubmitScoreUseCase(repository)

        val result = useCase("u1", 500L, testConfig())

        assertThat(result.exceptionOrNull()).isEqualTo(failure)
    }
}
