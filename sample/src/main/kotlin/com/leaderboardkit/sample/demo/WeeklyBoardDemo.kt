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
import com.leaderboardkit.LeaderboardKit
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.sample.SampleUser
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.ResetCountdown
import com.leaderboardkit.sample.ui.randomDemoScore
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * `TimeWindow.Weekly` resolves to `Polling(3min)` by default — frequent enough to
 * feel live during the window, without a live listener over what's typically a
 * large, high-churn result set (see the README's RefreshStrategy table). The
 * countdown above the board is entirely the sample app's own composable — see
 * [ResetCountdown] KDoc for why it isn't part of the library itself.
 */
@Composable
fun WeeklyBoardDemo(onBack: () -> Unit) {
    val config = remember {
        LeaderboardKit.buildConfig("weekly_coins") {
            timeWindow = TimeWindow.Weekly(resetTimeZone = TimeZone.UTC)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DemoScaffold(
        title = "Weekly",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        onSubmitRandomScore = {
            coroutineScope.launch {
                LeaderboardKit.submitScore(config, randomDemoScore(), SampleUser.PROFILE_METADATA)
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Submission failed") }
            }
        },
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {
            ResetCountdown(modifier = Modifier.padding(16.dp))
            LeaderboardKit.screen(
                config = config,
                modifier = Modifier.weight(1f),
                onShowError = { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
            )
        }
    }
}
