@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.presentation

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.leaderboardConfig
import org.junit.Test

class WidgetEntrySelectionTest {

    private val config = leaderboardConfig("board") {}

    private fun entry(userId: String, score: Long, rank: Int? = null) =
        LeaderboardEntry(userId, displayName = userId, avatarId = "avatar_01", score = score, rank = rank)

    private fun state(entries: List<LeaderboardEntry>, currentUserEntry: LeaderboardEntry? = null) =
        LeaderboardState(entries = entries, currentUserEntry = currentUserEntry, config = config)

    @Test
    fun `takes only the first topCount entries`() {
        val entries = (1..10).map { entry("u$it", it.toLong()) }

        val selection = state(entries).selectWidgetEntries(currentUserId = "u1", topCount = 5)

        assertThat(selection.top.map { it.userId }).containsExactly("u1", "u2", "u3", "u4", "u5").inOrder()
    }

    @Test
    fun `no pinned entry when the current user is already within the top slice`() {
        val entries = (1..10).map { entry("u$it", it.toLong()) }

        val selection = state(entries, currentUserEntry = entry("u3", 3)).selectWidgetEntries(currentUserId = "u3", topCount = 5)

        assertThat(selection.pinnedEntry).isNull()
    }

    @Test
    fun `current user off the top slice is pinned separately`() {
        val entries = (1..10).map { entry("u$it", it.toLong()) }
        val offScreen = entry("u9", 9)

        val selection = state(entries, currentUserEntry = offScreen).selectWidgetEntries(currentUserId = "u9", topCount = 5)

        assertThat(selection.top.map { it.userId }).doesNotContain("u9")
        assertThat(selection.pinnedEntry).isEqualTo(offScreen)
    }

    @Test
    fun `no pinned entry when currentUserEntry hasn't resolved yet`() {
        val entries = (1..10).map { entry("u$it", it.toLong()) }

        val selection = state(entries, currentUserEntry = null).selectWidgetEntries(currentUserId = "u9", topCount = 5)

        assertThat(selection.pinnedEntry).isNull()
    }

    @Test
    fun `topCount larger than the entry list just returns everything`() {
        val entries = (1..3).map { entry("u$it", it.toLong()) }

        val selection = state(entries).selectWidgetEntries(currentUserId = "u1", topCount = 5)

        assertThat(selection.top).hasSize(3)
    }
}
