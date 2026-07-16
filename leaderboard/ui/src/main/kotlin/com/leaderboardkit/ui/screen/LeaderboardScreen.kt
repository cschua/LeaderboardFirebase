@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.ui.screen

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.presentation.LeaderboardDependencies
import com.leaderboardkit.presentation.LeaderboardEffect
import com.leaderboardkit.presentation.LeaderboardError
import com.leaderboardkit.presentation.LeaderboardViewModel
import com.leaderboardkit.ui.avatar.AvatarResolver
import com.leaderboardkit.ui.avatar.DefaultAvatarResolver
import com.leaderboardkit.ui.component.DefaultEmptyLeaderboardState
import com.leaderboardkit.ui.component.DefaultErrorLeaderboardState
import com.leaderboardkit.ui.component.DefaultLoadingLeaderboardState
import com.leaderboardkit.ui.theme.LeaderboardTheme
import com.leaderboardkit.ui.theme.rememberLeaderboardTheme

/**
 * Full-screen leaderboard, the library's main entry composable. Owns a
 * [LeaderboardViewModel] scoped by [config]'s `boardId` — [LeaderboardWidget] uses
 * the same key scheme, so composing both for the same board under one
 * `ViewModelStoreOwner` (e.g. a compact widget plus an expanded list on the same
 * destination) shares a single ViewModel/upstream subscription rather than each
 * standing up its own.
 *
 * Takes [dependencies]/[currentUserId] directly rather than reaching for any
 * global — `:leaderboard:public-api`'s scoped `LeaderboardClient` /
 * `ProvideLeaderboardClient` is what resolves those two from a
 * `LeaderboardKitConfig` for host apps; this module stays usable without it
 * (see [LeaderboardDependencies] KDoc).
 *
 * Nothing below this composable ever touches [LeaderboardViewModel] directly: it
 * is read into a plain [com.leaderboardkit.presentation.LeaderboardState] here and
 * handed to the stateless [LeaderboardContent]/[com.leaderboardkit.ui.component.LeaderboardRow] tree.
 */
@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    config: LeaderboardConfig,
    currentUserId: String,
    dependencies: LeaderboardDependencies,
    theme: LeaderboardTheme = rememberLeaderboardTheme(),
    rowContent: (@Composable (entry: LeaderboardEntry, isCurrentUser: Boolean) -> Unit)? = null,
    emptyStateContent: @Composable () -> Unit = { DefaultEmptyLeaderboardState() },
    errorStateContent: @Composable (LeaderboardError, onRetry: () -> Unit) -> Unit =
        { error, onRetry -> DefaultErrorLeaderboardState(error, onRetry) },
    loadingStateContent: @Composable () -> Unit = { DefaultLoadingLeaderboardState() },
    avatarResolver: AvatarResolver = DefaultAvatarResolver,
    onShowError: (String) -> Unit = {},
) {
    val viewModel: LeaderboardViewModel = viewModel(
        key = "${config.boardId}_$currentUserId",
        factory = remember(config, currentUserId, dependencies) {
            LeaderboardViewModel.factory(config, currentUserId, dependencies)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LeaderboardEffect.ShowError -> onShowError(effect.message)
                LeaderboardEffect.ScrollToUserRank -> {
                    // Read .value fresh rather than the recomposition-scoped `state`
                    // above: this coroutine is long-lived and must not act on a
                    // snapshot from whenever it happened to launch.
                    val index = viewModel.state.value.entries.indexOfFirst { it.userId == currentUserId }
                    if (index >= 0) listState.animateScrollToItem(index)
                }
            }
        }
    }

    LeaderboardContent(
        state = state,
        onIntent = viewModel::onIntent,
        currentUserId = currentUserId,
        theme = theme,
        modifier = modifier,
        listState = listState,
        rowContent = rowContent,
        emptyStateContent = emptyStateContent,
        errorStateContent = errorStateContent,
        loadingStateContent = loadingStateContent,
        avatarResolver = avatarResolver,
    )
}
