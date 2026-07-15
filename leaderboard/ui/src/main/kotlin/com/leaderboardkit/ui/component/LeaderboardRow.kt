package com.leaderboardkit.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.ui.avatar.AvatarResolver
import com.leaderboardkit.ui.avatar.DefaultAvatarResolver
import com.leaderboardkit.ui.theme.LeaderboardTheme
import com.leaderboardkit.ui.theme.rememberLeaderboardTheme

private const val TOP_THREE_THRESHOLD = 3

/**
 * A single leaderboard row: rank badge, avatar, display name, score — with
 * top-3 / current-user background highlighting from [theme]. Stateless: takes
 * only the [entry] to render and whether it [isCurrentUser], nothing else. Host
 * apps that want completely different row visuals use
 * [com.leaderboardkit.ui.screen.LeaderboardScreen]'s `rowContent` slot instead
 * of trying to configure this composable further.
 */
@Composable
fun LeaderboardRow(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    theme: LeaderboardTheme,
    modifier: Modifier = Modifier,
    avatarResolver: AvatarResolver = DefaultAvatarResolver,
) {
    val backgroundColor = when {
        isCurrentUser -> theme.colors.currentUserRowHighlight
        (entry.rank ?: Int.MAX_VALUE) <= TOP_THREE_THRESHOLD -> theme.colors.topThreeHighlight
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(theme.rowHeight)
            .background(backgroundColor)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(
            rank = entry.rank,
            style = theme.rankBadgeStyle,
            textStyle = theme.typography.rank,
            modifier = Modifier.widthIn(min = 32.dp),
        )
        Spacer(Modifier.width(12.dp))
        AvatarView(
            avatarId = entry.avatarId,
            shape = theme.avatarShape,
            resolver = avatarResolver,
            contentDescription = entry.displayName,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = entry.displayName,
            style = theme.typography.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        ScoreLabel(score = entry.score, textStyle = theme.typography.score)
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardRowPreview() {
    LeaderboardRow(
        entry = LeaderboardEntry("u1", "Alice", "avatar_03", 12_345, rank = 1),
        isCurrentUser = false,
        theme = rememberLeaderboardTheme(),
    )
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardRowCurrentUserPreview() {
    LeaderboardRow(
        entry = LeaderboardEntry("me", "Me", "avatar_07", 4_200, rank = 4502),
        isCurrentUser = true,
        theme = rememberLeaderboardTheme(),
    )
}
