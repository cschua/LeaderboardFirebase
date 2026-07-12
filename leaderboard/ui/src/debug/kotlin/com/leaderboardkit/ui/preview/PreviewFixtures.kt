@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.ui.preview

import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.data.fake.FakeLeaderboardRepository
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.leaderboardConfig
import com.leaderboardkit.presentation.LeaderboardError
import com.leaderboardkit.presentation.LeaderboardState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Debug-only (see `:leaderboard:ui`'s `build.gradle.kts`: `FakeLeaderboardRepository`
 * is a `debugImplementation` dependency, never shipped to a host app's release
 * build) sample data for `@Preview`s, built by actually driving Stage 1's
 * [FakeLeaderboardRepository] rather than hand-assembling [LeaderboardState]s —
 * so a preview and a real (fake-backed) [com.leaderboardkit.presentation.LeaderboardViewModel]
 * run through the identical repository contract.
 */
internal object PreviewFixtures {

    val previewConfig: LeaderboardConfig = leaderboardConfig("preview_board") { pageSize = 10 }

    fun sampleEntries(count: Int, scoreFor: (Int) -> Long = { (count - it).toLong() * 137 }): List<LeaderboardEntry> =
        (0 until count).map { index ->
            LeaderboardEntry(
                userId = "user_$index",
                displayName = "Player ${index + 1}",
                avatarId = AvatarDefaults.PLACEHOLDER_AVATAR_IDS[index % AvatarDefaults.PLACEHOLDER_AVATAR_IDS.size],
                score = scoreFor(index),
                rank = null,
            )
        }

    /** Empty board, nothing loaded yet, no error. */
    fun emptyState(): LeaderboardState = LeaderboardState(config = previewConfig)

    /** First fetch in flight. */
    fun loadingState(): LeaderboardState = LeaderboardState(config = previewConfig, isLoading = true)

    /** First fetch failed, nothing loaded yet. */
    fun errorState(): LeaderboardState =
        LeaderboardState(config = previewConfig, error = LeaderboardError.Unknown("Couldn't reach the server. Check your connection."))

    /** Top-10 board where the current user (`user_3`) is already in the loaded window. */
    fun populatedWithCurrentUserOnScreen(): LeaderboardState {
        val repository = FakeLeaderboardRepository(sampleEntries(10))
        val entries = runBlocking { repository.observeEntries(previewConfig).first() }
        return LeaderboardState(
            entries = entries,
            currentUserEntry = entries.firstOrNull { it.userId == "user_3" },
            config = previewConfig,
        )
    }

    /**
     * Large board where the current user (`user_150`) is far outside the loaded
     * top-10 window — exercises the same surrounding-ranks resolution path
     * [com.leaderboardkit.presentation.LeaderboardViewModel] uses, via
     * [FakeLeaderboardRepository.getSurroundingEntries], so the pinned
     * "off-screen" row in `LeaderboardContent`/previews reflects real repository
     * behavior rather than a hand-faked entry.
     */
    fun populatedWithCurrentUserOffscreen(): LeaderboardState {
        val repository = FakeLeaderboardRepository(sampleEntries(300))
        val currentUserId = "user_150"
        val entries = runBlocking { repository.observeEntries(previewConfig).first() }
        val currentUserEntry = runBlocking {
            repository.getSurroundingEntries(currentUserId, radius = 1, previewConfig)
                .getOrNull()
                ?.firstOrNull { it.userId == currentUserId }
        }
        return LeaderboardState(entries = entries, currentUserEntry = currentUserEntry, config = previewConfig)
    }
}
