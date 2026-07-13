package com.leaderboardkit.sample.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leaderboardkit.LocalLeaderboardClient
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.sample.SampleUser
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.ResetCountdown
import com.leaderboardkit.sample.ui.rememberSubmitRandomScoreAction
import com.leaderboardkit.LeaderboardScreen
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * `TimeWindow.Weekly` resolves to `Polling(3min)` by default — frequent enough to
 * feel live during the window, without a live listener over what's typically a
 * large, high-churn result set (see the README's RefreshStrategy table).
 */
@Composable
fun WeeklyBoardDemo(onBack: () -> Unit) {
    val client = requireNotNull(LocalLeaderboardClient.current) {
        "WeeklyBoardDemo must be composed under ProvideLeaderboardClient."
    }
    val config = remember(client) {
        client.buildConfig("weekly_demo") {
            timeWindow = TimeWindow.Weekly(resetTimeZone = TimeZone.UTC)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DemoScaffold(
        title = "Weekly",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        onSubmitRandomScore = rememberSubmitRandomScoreAction(client, config, SampleUser.PROFILE_METADATA, snackbarHostState),
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {
            ResetCountdown(client, config, modifier = Modifier.padding(16.dp))
            LeaderboardScreen(
                config = config,
                modifier = Modifier.weight(1f),
                onShowError = { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
            )
        }
    }
}
