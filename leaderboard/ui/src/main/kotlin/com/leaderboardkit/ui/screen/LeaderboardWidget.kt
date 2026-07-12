@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.leaderboardConfig
import com.leaderboardkit.presentation.LeaderboardDependencies
import com.leaderboardkit.presentation.LeaderboardState
import com.leaderboardkit.presentation.LeaderboardViewModel
import com.leaderboardkit.ui.avatar.AvatarResolver
import com.leaderboardkit.ui.avatar.DefaultAvatarResolver
import com.leaderboardkit.ui.component.LeaderboardRow
import com.leaderboardkit.ui.theme.LeaderboardTheme
import com.leaderboardkit.ui.theme.rememberLeaderboardTheme

/**
 * Compact, embeddable board preview (e.g. a "top 5" card on a home/dashboard
 * screen) driven by the exact same [LeaderboardState] machinery as
 * [LeaderboardScreen] — see that composable's KDoc for the ViewModel-sharing key
 * scheme. Not scrollable/paginated by design: it's a static top-N snapshot with an
 * optional pinned current-user row and a "see all" affordance, not a second
 * full list.
 */
@Composable
fun LeaderboardWidget(
    config: LeaderboardConfig,
    currentUserId: String,
    dependencies: LeaderboardDependencies,
    theme: LeaderboardTheme = rememberLeaderboardTheme(),
    modifier: Modifier = Modifier,
    topCount: Int = 5,
    avatarResolver: AvatarResolver = DefaultAvatarResolver,
    onSeeAllClick: (() -> Unit)? = null,
) {
    val viewModel: LeaderboardViewModel = viewModel(
        key = config.boardId,
        factory = remember(config, currentUserId, dependencies) {
            LeaderboardViewModel.factory(config, currentUserId, dependencies)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LeaderboardWidgetContent(
        state = state,
        currentUserId = currentUserId,
        theme = theme,
        modifier = modifier,
        topCount = topCount,
        avatarResolver = avatarResolver,
        onSeeAllClick = onSeeAllClick,
    )
}

/** State-driven core of [LeaderboardWidget] — see [LeaderboardContent] for why this split exists. */
@Composable
fun LeaderboardWidgetContent(
    state: LeaderboardState,
    currentUserId: String,
    theme: LeaderboardTheme,
    modifier: Modifier = Modifier,
    topCount: Int = 5,
    avatarResolver: AvatarResolver = DefaultAvatarResolver,
    onSeeAllClick: (() -> Unit)? = null,
) {
    val top = state.entries.take(topCount)
    val pinnedEntry = state.currentUserEntry?.takeIf { top.none { entry -> entry.userId == currentUserId } }

    Card(modifier = modifier.fillMaxWidth()) {
        Column {
            if (top.isEmpty()) {
                Text(
                    text = "No scores yet",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            top.forEach { entry ->
                LeaderboardRow(
                    entry = entry,
                    isCurrentUser = entry.userId == currentUserId,
                    theme = theme,
                    avatarResolver = avatarResolver,
                )
            }
            if (pinnedEntry != null) {
                HorizontalDivider()
                LeaderboardRow(entry = pinnedEntry, isCurrentUser = true, theme = theme, avatarResolver = avatarResolver)
            }
            if (onSeeAllClick != null) {
                HorizontalDivider()
                TextButton(onClick = onSeeAllClick, modifier = Modifier.fillMaxWidth()) {
                    Text("See all")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardWidgetContentPreview() {
    val entries = (1..5).map {
        LeaderboardEntry("u$it", "Player $it", "avatar_0$it", (1000 - it * 50).toLong(), rank = it)
    }
    LeaderboardWidgetContent(
        state = LeaderboardState(
            entries = entries,
            config = leaderboardConfig("preview") {},
        ),
        currentUserId = "u1",
        theme = rememberLeaderboardTheme(),
        onSeeAllClick = {},
    )
}
