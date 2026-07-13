@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.ui.screen.LeaderboardScreen
import com.leaderboardkit.ui.screen.LeaderboardWidget
import com.leaderboardkit.ui.theme.LeaderboardTheme
import com.leaderboardkit.ui.theme.rememberLeaderboardTheme

/**
 * Holds the [LeaderboardClient] that [ProvideLeaderboardClient] scoped into the
 * composition, `null` where none has been — [LeaderboardScreen]/[LeaderboardWidget] read this rather
 * than a global, so a composable tree with no [ProvideLeaderboardClient] above
 * it renders a helpful message instead of crashing.
 */
val LocalLeaderboardClient = staticCompositionLocalOf<LeaderboardClient?> { null }

/**
 * Scopes [client] (from [createLeaderboardClient]) to [content]'s composition
 * so [LeaderboardScreen]/[LeaderboardWidget] anywhere below it can find it via [LocalLeaderboardClient].
 * Usually wraps a whole screen or the app root, once, right below where
 * [createLeaderboardClient] produced [client].
 */
@Composable
fun ProvideLeaderboardClient(client: LeaderboardClient, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLeaderboardClient provides client, content = content)
}

/**
 * The library's main entry composable — a full-screen, scrollable, paginated
 * board. Reads its [LeaderboardClient] from [LocalLeaderboardClient]; renders a
 * message pointing at [ProvideLeaderboardClient] instead of the board if none
 * was provided.
 */
@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    config: LeaderboardConfig,
    theme: LeaderboardTheme = LocalLeaderboardClient.current?.config?.defaultTheme ?: rememberLeaderboardTheme(),
    rowContent: (@Composable (entry: LeaderboardEntry, isCurrentUser: Boolean) -> Unit)? = null,
    onShowError: (String) -> Unit = {},
) {
    val client = LocalLeaderboardClient.current
        ?: return MissingLeaderboardClientMessage(modifier)

    LeaderboardScreen(
        config = config,
        currentUserId = client.config.currentUserId(),
        dependencies = client.dependencies,
        theme = theme,
        modifier = modifier,
        rowContent = rowContent,
        avatarResolver = client.config.avatarResolver,
        onShowError = onShowError,
    )
}

/**
 * A compact, embeddable top-N card for dashboards/home screens — see
 * `LeaderboardWidget` KDoc for the ViewModel-sharing rules with [LeaderboardScreen]. Reads
 * its [LeaderboardClient] from [LocalLeaderboardClient]; renders a message
 * pointing at [ProvideLeaderboardClient] instead of the widget if none was
 * provided.
 */
@Composable
fun LeaderboardWidget(
    modifier: Modifier = Modifier,
    config: LeaderboardConfig,
    theme: LeaderboardTheme = LocalLeaderboardClient.current?.config?.defaultTheme ?: rememberLeaderboardTheme(),
    topCount: Int = 5,
    onSeeAllClick: (() -> Unit)? = null,
) {
    val client = LocalLeaderboardClient.current
        ?: return MissingLeaderboardClientMessage(modifier)

    LeaderboardWidget(
        config = config,
        currentUserId = client.config.currentUserId(),
        dependencies = client.dependencies,
        theme = theme,
        modifier = modifier,
        topCount = topCount,
        avatarResolver = client.config.avatarResolver,
        onSeeAllClick = onSeeAllClick,
    )
}

/** Rendered by [LeaderboardScreen]/[LeaderboardWidget] in place of the board when no [LeaderboardClient] reaches them via [LocalLeaderboardClient]. */
@Composable
private fun MissingLeaderboardClientMessage(modifier: Modifier = Modifier) {
    Text(
        text = "No LeaderboardClient found. Wrap this composable in " +
            "ProvideLeaderboardClient(client) { ... }, using the client returned by " +
            "createLeaderboardClient(context, config).",
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}
