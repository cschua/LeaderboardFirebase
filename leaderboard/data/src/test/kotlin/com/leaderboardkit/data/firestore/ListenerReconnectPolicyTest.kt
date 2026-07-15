package com.leaderboardkit.data.firestore

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ListenerReconnectPolicyTest {

    private val policy = ListenerReconnectPolicy(staleAfter = 30.minutes)
    private val t0 = Instant.parse("2026-07-09T12:00:00Z")

    @Test
    fun `never-active board does not force a server read`() {
        assertThat(policy.shouldForceServerRead("board1", now = t0)).isFalse()
    }

    @Test
    fun `short gap since last active does not force a server read`() {
        policy.markActive("board1", now = t0)
        policy.markInactive("board1", now = t0)

        assertThat(policy.shouldForceServerRead("board1", now = t0 + 10.minutes)).isFalse()
    }

    @Test
    fun `gap past the stale threshold forces a server read`() {
        policy.markActive("board1", now = t0)
        policy.markInactive("board1", now = t0)

        assertThat(policy.shouldForceServerRead("board1", now = t0 + 31.minutes)).isTrue()
    }

    @Test
    fun `boards are tracked independently`() {
        policy.markActive("board1", now = t0)
        policy.markInactive("board1", now = t0)

        assertThat(policy.shouldForceServerRead("board2", now = t0 + 31.minutes)).isFalse()
    }

    @Test
    fun `gap exactly at the stale threshold does not force a server read`() {
        policy.markActive("board1", now = t0)
        policy.markInactive("board1", now = t0)

        assertThat(policy.shouldForceServerRead("board1", now = t0 + 30.minutes)).isFalse()
    }

    @Test
    fun `becoming active again resets the gap even after going stale`() {
        policy.markActive("board1", now = t0)
        policy.markInactive("board1", now = t0)
        assertThat(policy.shouldForceServerRead("board1", now = t0 + 31.minutes)).isTrue()

        policy.markActive("board1", now = t0 + 31.minutes)

        assertThat(policy.shouldForceServerRead("board1", now = t0 + 35.minutes)).isFalse()
    }

    @Test
    fun `defaults to the real clock when now is not supplied`() {
        val livePolicy = ListenerReconnectPolicy()

        livePolicy.markActive("board1")
        livePolicy.markInactive("board1")

        assertThat(livePolicy.shouldForceServerRead("board1")).isFalse()
    }
}
