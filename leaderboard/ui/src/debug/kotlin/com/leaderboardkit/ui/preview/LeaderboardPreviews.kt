@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.presentation.LeaderboardState
import com.leaderboardkit.ui.screen.LeaderboardContent
import com.leaderboardkit.ui.theme.rememberLeaderboardTheme

/**
 * Required Stage-2 preview set, all rendered through the same state-driven
 * [LeaderboardContent] a real [com.leaderboardkit.presentation.LeaderboardViewModel]
 * feeds — see [PreviewFixtures] for how each [LeaderboardState] snapshot is
 * produced from Stage 1's [com.leaderboardkit.data.fake.FakeLeaderboardRepository].
 */
@Composable
private fun PreviewScaffold(state: LeaderboardState, currentUserId: String = "user_3") {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LeaderboardContent(
            state = state,
            onIntent = {},
            currentUserId = currentUserId,
            theme = rememberLeaderboardTheme(),
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun LeaderboardEmptyStatePreview() {
    PreviewScaffold(state = PreviewFixtures.emptyState())
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun LeaderboardLoadingStatePreview() {
    PreviewScaffold(state = PreviewFixtures.loadingState())
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun LeaderboardErrorStatePreview() {
    PreviewScaffold(state = PreviewFixtures.errorState())
}

@Preview(name = "Populated - current user in top 10", showBackground = true, heightDp = 500)
@Composable
private fun LeaderboardPopulatedCurrentUserOnScreenPreview() {
    PreviewScaffold(state = PreviewFixtures.populatedWithCurrentUserOnScreen(), currentUserId = "user_3")
}

@Preview(name = "Populated - current user off-screen", showBackground = true, heightDp = 500)
@Composable
private fun LeaderboardPopulatedCurrentUserOffscreenPreview() {
    PreviewScaffold(state = PreviewFixtures.populatedWithCurrentUserOffscreen(), currentUserId = "user_150")
}
