@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.data.realtimedb

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry
import org.junit.Test

class RealtimeDbLeaderboardEntryMapperTest {

    private val mapper = RealtimeDbLeaderboardEntryMapper()

    @Test
    fun `fromNode maps all fields and always leaves rank null`() {
        val value = mapOf(
            "displayName" to "Bob",
            "avatarId" to "avatar_09",
            "score" to 900L,
            "metadata" to mapOf("level" to 4L),
        )

        val entry = mapper.fromNode(userId = "u1", value = value)

        assertThat(entry).isEqualTo(
            LeaderboardEntry(
                userId = "u1",
                displayName = "Bob",
                avatarId = "avatar_09",
                score = 900L,
                rank = null,
                metadata = mapOf("level" to 4L),
            ),
        )
    }

    @Test
    fun `fromNode defaults missing fields`() {
        val entry = mapper.fromNode(userId = "u2", value = emptyMap())

        assertThat(entry.displayName).isEmpty()
        assertThat(entry.avatarId).isEqualTo(AvatarDefaults.DEFAULT_AVATAR_ID)
        assertThat(entry.score).isEqualTo(0L)
        assertThat(entry.metadata).isEmpty()
    }

    @Test
    fun `toNode does not include the userId (it is the node key, not a field)`() {
        val entry = LeaderboardEntry("u3", "Carol", "avatar_02", 10L, rank = 2, metadata = emptyMap())

        val node = mapper.toNode(entry)

        assertThat(node).doesNotContainKey("userId")
        assertThat(node["score"]).isEqualTo(10L)
    }
}
