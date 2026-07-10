package com.leaderboardkit.sample.demo

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.leaderboardkit.LeaderboardKit
import com.leaderboardkit.domain.model.LeaderboardScope
import com.leaderboardkit.sample.SampleUser
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.randomDemoScore
import kotlinx.coroutines.launch

/**
 * `LeaderboardScope.Friends` resolves to `RealtimeListener` by default: a
 * friends list is small and bounded, so a live snapshot listener's read cost
 * stays low regardless of churn (see the README's RefreshStrategy table).
 * Friend-graph resolution ([SampleUser.FRIEND_IDS]) is entirely this sample
 * app's job — the library never resolves a social graph itself.
 */
@Composable
fun FriendsBoardDemo(onBack: () -> Unit) {
    val config = remember {
        LeaderboardKit.buildConfig("friends_demo") {
            scope = LeaderboardScope.Friends(SampleUser.ID, SampleUser.FRIEND_IDS)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DemoScaffold(
        title = "Friends",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        onSubmitRandomScore = {
            coroutineScope.launch {
                LeaderboardKit.submitScore(config, randomDemoScore())
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Submission failed") }
            }
        },
    ) { modifier ->
        LeaderboardKit.screen(
            config = config,
            modifier = modifier,
            onShowError = { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
        )
    }
}
