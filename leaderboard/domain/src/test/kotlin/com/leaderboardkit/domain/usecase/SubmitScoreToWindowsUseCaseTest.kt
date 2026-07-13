package com.leaderboardkit.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.leaderboardConfig
import com.leaderboardkit.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Test

/** Records every [submitScore] call, in order, and replays [results] one per call. */
private class SequencedSubmitRepository(private val results: List<Result<Unit>>) : LeaderboardRepository {

    val submittedConfigs = mutableListOf<LeaderboardConfig>()

    override fun observeEntries(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> = flowOf(emptyList())

    override suspend fun loadMore(config: LeaderboardConfig): Result<Boolean> = Result.success(false)

    override suspend fun submitScore(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any>,
    ): Result<Unit> {
        submittedConfigs += config
        return results[submittedConfigs.size - 1]
    }

    override suspend fun getUserRank(userId: String, config: LeaderboardConfig): Result<Int?> = Result.success(null)

    override suspend fun getSurroundingEntries(
        userId: String,
        radius: Int,
        config: LeaderboardConfig,
    ): Result<List<LeaderboardEntry>> = Result.success(emptyList())
}

private fun windowConfig(boardId: String) = leaderboardConfig(boardId) {}

class SubmitScoreToWindowsUseCaseTest {

    @Test
    fun `submits to every config in order`() = runTest {
        val repository = SequencedSubmitRepository(results = List(3) { Result.success(Unit) })
        val configs = listOf(windowConfig("weekly"), windowConfig("monthly"), windowConfig("all_time"))
        val useCase = SubmitScoreToWindowsUseCase(SubmitScoreUseCase(repository), staggerDelay = 300.milliseconds)

        val result = useCase("u1", 500L, configs)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.submittedConfigs).isEqualTo(configs)
    }

    @Test
    fun `waits the stagger delay between writes but not after the last`() = runTest {
        val repository = SequencedSubmitRepository(results = List(3) { Result.success(Unit) })
        val configs = listOf(windowConfig("weekly"), windowConfig("monthly"), windowConfig("all_time"))
        val useCase = SubmitScoreToWindowsUseCase(SubmitScoreUseCase(repository), staggerDelay = 300.milliseconds)

        useCase("u1", 500L, configs)

        assertThat(currentTime).isEqualTo(600L)
    }

    @Test
    fun `single config does not delay at all`() = runTest {
        val repository = SequencedSubmitRepository(results = listOf(Result.success(Unit)))
        val useCase = SubmitScoreToWindowsUseCase(SubmitScoreUseCase(repository), staggerDelay = 300.milliseconds)

        useCase("u1", 500L, listOf(windowConfig("weekly")))

        assertThat(currentTime).isEqualTo(0L)
    }

    @Test
    fun `stops at the first failure and does not submit remaining configs`() = runTest {
        val failure = RuntimeException("rate limited")
        val repository = SequencedSubmitRepository(
            results = listOf(Result.success(Unit), Result.failure(failure), Result.success(Unit)),
        )
        val configs = listOf(windowConfig("weekly"), windowConfig("monthly"), windowConfig("all_time"))
        val useCase = SubmitScoreToWindowsUseCase(SubmitScoreUseCase(repository), staggerDelay = 300.milliseconds)

        val result = useCase("u1", 500L, configs)

        assertThat(result.exceptionOrNull()).isEqualTo(failure)
        assertThat(repository.submittedConfigs).hasSize(2)
    }

    @Test
    fun `rejects blank userId without submitting to any config`() = runTest {
        val repository = SequencedSubmitRepository(results = List(3) { Result.success(Unit) })
        val configs = listOf(windowConfig("weekly"), windowConfig("monthly"), windowConfig("all_time"))
        val useCase = SubmitScoreToWindowsUseCase(SubmitScoreUseCase(repository), staggerDelay = 300.milliseconds)

        try {
            useCase("", 500L, configs)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
        assertThat(repository.submittedConfigs).isEmpty()
    }
}
