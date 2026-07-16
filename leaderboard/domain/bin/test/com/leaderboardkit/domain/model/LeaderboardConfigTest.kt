package com.leaderboardkit.domain.model

import org.junit.Test

class LeaderboardConfigTest {

    private fun baseBuilder() = leaderboardConfig("board") {}

    @Test(expected = IllegalArgumentException::class)
    fun `rejects blank boardId`() {
        leaderboardConfig("   ") {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non-positive pageSize`() {
        leaderboardConfig("board") { pageSize = 0 }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects negative prefetchDistance`() {
        leaderboardConfig("board") { prefetchDistance = -1 }
    }

    @Test
    fun `accepts a valid config`() {
        baseBuilder()
    }
}
