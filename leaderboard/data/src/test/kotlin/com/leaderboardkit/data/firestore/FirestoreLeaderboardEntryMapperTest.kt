@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.data.firestore

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry
import org.junit.Test

class FirestoreLeaderboardEntryMapperTest {

    private val mapper = FirestoreLeaderboardEntryMapper()

    @Test
    fun `fromDocument maps all fields`() {
        val data = mapOf(
            "userId" to "u1",
            "displayName" to "Alice",
            "avatarId" to "avatar_07",
            "score" to 1500L,
            "rank" to 3L,
            "metadata" to mapOf("combo" to 7L),
        )

        val entry = mapper.fromDocument(documentId = "u1", data = data)

        assertThat(entry).isEqualTo(
            LeaderboardEntry(
                userId = "u1",
                displayName = "Alice",
                avatarId = "avatar_07",
                score = 1500L,
                rank = 3,
                metadata = mapOf("combo" to 7L),
            ),
        )
    }

    @Test
    fun `fromDocument falls back to document id and defaults for missing fields`() {
        val entry = mapper.fromDocument(documentId = "u2", data = emptyMap())

        assertThat(entry.userId).isEqualTo("u2")
        assertThat(entry.displayName).isEmpty()
        assertThat(entry.avatarId).isEqualTo(AvatarDefaults.DEFAULT_AVATAR_ID)
        assertThat(entry.score).isEqualTo(0L)
        assertThat(entry.rank).isNull()
        assertThat(entry.metadata).isEmpty()
    }

    @Test
    fun `toDocument round-trips through fromDocument`() {
        val original = LeaderboardEntry(
            userId = "u3",
            displayName = "Carol",
            avatarId = "avatar_03",
            score = 42L,
            rank = 1,
            metadata = mapOf("streak" to 5L),
        )

        val roundTripped = mapper.fromDocument(original.userId, mapper.toDocument(original))

        assertThat(roundTripped).isEqualTo(original)
    }
}
