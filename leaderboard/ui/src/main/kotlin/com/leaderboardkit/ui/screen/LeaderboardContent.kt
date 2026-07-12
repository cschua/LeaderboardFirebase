@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.presentation.LeaderboardError
import com.leaderboardkit.presentation.LeaderboardIntent
import com.leaderboardkit.presentation.LeaderboardState
import com.leaderboardkit.ui.avatar.AvatarResolver
import com.leaderboardkit.ui.avatar.DefaultAvatarResolver
import com.leaderboardkit.ui.component.DefaultEmptyLeaderboardState
import com.leaderboardkit.ui.component.DefaultErrorLeaderboardState
import com.leaderboardkit.ui.component.DefaultLoadingLeaderboardState
import com.leaderboardkit.ui.component.LeaderboardRow
import com.leaderboardkit.ui.theme.LeaderboardTheme

/**
 * State-driven, ViewModel-free core of [LeaderboardScreen] — takes [state] and an
 * [onIntent] callback rather than owning a ViewModel itself. Exists as its own
 * composable so `@Preview`s (and any host that wants to hoist state differently)
 * can render every board shape from a plain [LeaderboardState] without needing a
 * live backend. [LeaderboardScreen] is a thin wrapper that supplies the ViewModel.
 */
@Composable
fun LeaderboardContent(
    state: LeaderboardState,
    onIntent: (LeaderboardIntent) -> Unit,
    currentUserId: String,
    theme: LeaderboardTheme,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    rowContent: (@Composable (entry: LeaderboardEntry, isCurrentUser: Boolean) -> Unit)? = null,
    emptyStateContent: @Composable () -> Unit = { DefaultEmptyLeaderboardState() },
    errorStateContent: @Composable (LeaderboardError, onRetry: () -> Unit) -> Unit =
        { error, onRetry -> DefaultErrorLeaderboardState(error, onRetry) },
    loadingStateContent: @Composable () -> Unit = { DefaultLoadingLeaderboardState() },
    avatarResolver: AvatarResolver = DefaultAvatarResolver,
) {
    val error = state.error
    when {
        state.isLoading && state.entries.isEmpty() -> loadingStateContent()
        // A background refresh failing while data is already on screen surfaces via
        // LeaderboardEffect.ShowError (a snackbar) instead of replacing the list —
        // this full-screen error state is only for "we have nothing to show at all".
        error != null && state.entries.isEmpty() -> errorStateContent(error) { onIntent(LeaderboardIntent.Refresh) }
        state.entries.isEmpty() -> emptyStateContent()
        else -> LeaderboardList(
            state = state,
            onIntent = onIntent,
            currentUserId = currentUserId,
            theme = theme,
            modifier = modifier,
            listState = listState,
            rowContent = rowContent,
            avatarResolver = avatarResolver,
        )
    }
}

@Composable
private fun LeaderboardList(
    state: LeaderboardState,
    onIntent: (LeaderboardIntent) -> Unit,
    currentUserId: String,
    theme: LeaderboardTheme,
    modifier: Modifier,
    listState: LazyListState,
    rowContent: (@Composable (entry: LeaderboardEntry, isCurrentUser: Boolean) -> Unit)?,
    avatarResolver: AvatarResolver,
) {
    // rememberUpdatedState so the long-lived scroll-position collector below always
    // reads the latest canLoadMore/isLoadingMore/entries, not the values from
    // whichever recomposition first launched it.
    val latestState by rememberUpdatedState(state)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val s = latestState
                val threshold = s.entries.size - 1 - s.config.prefetchDistance
                if (lastVisibleIndex != null && lastVisibleIndex >= threshold && s.canLoadMore && !s.isLoadingMore) {
                    onIntent(LeaderboardIntent.LoadMore)
                }
            }
    }

    val isCurrentUserOffscreen = state.currentUserEntry != null && state.entries.none { it.userId == currentUserId }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(state.entries, key = { it.userId }) { entry ->
                val isCurrentUser = entry.userId == currentUserId
                Box(modifier = Modifier.animateItem(placementSpec = theme.rankChangeAnimationSpec)) {
                    if (rowContent != null) {
                        rowContent(entry, isCurrentUser)
                    } else {
                        LeaderboardRow(entry = entry, isCurrentUser = isCurrentUser, theme = theme, avatarResolver = avatarResolver)
                    }
                }
            }
            if (state.isLoadingMore) {
                item(key = "__loading_more__") {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        val pinnedEntry = state.currentUserEntry
        if (isCurrentUserOffscreen && pinnedEntry != null) {
            HorizontalDivider()
            if (rowContent != null) {
                rowContent(pinnedEntry, true)
            } else {
                LeaderboardRow(entry = pinnedEntry, isCurrentUser = true, theme = theme, avatarResolver = avatarResolver)
            }
        }
    }
}
