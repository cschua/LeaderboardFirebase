package com.leaderboardkit.data.ratelimit

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ClientRateLimiterTest {

    @Test
    fun `first submission for a user and board is always allowed`() = runTest {
        val limiter = ClientRateLimiter(minInterval = 10.seconds)

        assertThat(limiter.tryAcquire("u1", "board1")).isNull()
    }

    @Test
    fun `second immediate submission for the same user and board is rejected`() = runTest {
        val limiter = ClientRateLimiter(minInterval = 10.seconds)
        limiter.tryAcquire("u1", "board1")

        val cooldown = limiter.tryAcquire("u1", "board1")

        assertThat(cooldown).isNotNull()
    }

    @Test
    fun `different boards for the same user are independent`() = runTest {
        val limiter = ClientRateLimiter(minInterval = 10.seconds)
        limiter.tryAcquire("u1", "board1")

        assertThat(limiter.tryAcquire("u1", "board2")).isNull()
    }

    @Test
    fun `different users on the same board are independent`() = runTest {
        val limiter = ClientRateLimiter(minInterval = 10.seconds)
        limiter.tryAcquire("u1", "board1")

        assertThat(limiter.tryAcquire("u2", "board1")).isNull()
    }
}
