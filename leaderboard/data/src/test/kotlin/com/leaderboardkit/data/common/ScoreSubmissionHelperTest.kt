@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.data.common

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.SortDirection
import com.leaderboardkit.domain.model.leaderboardConfig
import org.junit.Test

class ScoreSubmissionHelperTest {

    @Test
    fun `isBetter - descending`() {
        val config = leaderboardConfig("board") { sortDirection = SortDirection.Descending }
        assertThat(ScoreSubmissionHelper.isBetter(100, 50, config)).isTrue()
        assertThat(ScoreSubmissionHelper.isBetter(50, 100, config)).isFalse()
        assertThat(ScoreSubmissionHelper.isBetter(100, 100, config)).isFalse()
    }

    @Test
    fun `isBetter - ascending`() {
        val config = leaderboardConfig("board") { sortDirection = SortDirection.Ascending }
        assertThat(ScoreSubmissionHelper.isBetter(50, 100, config)).isTrue()
        assertThat(ScoreSubmissionHelper.isBetter(100, 50, config)).isFalse()
        assertThat(ScoreSubmissionHelper.isBetter(100, 100, config)).isFalse()
    }

    @Test
    fun `createSubmissionEntry - use metadata when present`() {
        val metadata = mapOf("displayName" to "Alice", "avatarId" to "av1", "other" to "val")
        val entry = ScoreSubmissionHelper.createSubmissionEntry(
            userId = "u1",
            score = 100,
            metadata = metadata,
            existingDisplayName = "Old",
            existingAvatarId = "avOld"
        )

        assertThat(entry.displayName).isEqualTo("Alice")
        assertThat(entry.avatarId).isEqualTo("av1")
        assertThat(entry.metadata).containsExactly("other", "val")
    }

    @Test
    fun `createSubmissionEntry - fallback to existing when metadata missing`() {
        val entry = ScoreSubmissionHelper.createSubmissionEntry(
            userId = "u1",
            score = 100,
            metadata = emptyMap(),
            existingDisplayName = "Old",
            existingAvatarId = "avOld"
        )

        assertThat(entry.displayName).isEqualTo("Old")
        assertThat(entry.avatarId).isEqualTo("avOld")
    }

    @Test
    fun `createSubmissionEntry - fallback to defaults when everything missing`() {
        val entry = ScoreSubmissionHelper.createSubmissionEntry(
            userId = "u1",
            score = 100,
            metadata = emptyMap(),
            existingDisplayName = null,
            existingAvatarId = null
        )

        assertThat(entry.displayName).isEmpty()
        assertThat(entry.avatarId).isEqualTo(AvatarDefaults.DEFAULT_AVATAR_ID)
    }
}
