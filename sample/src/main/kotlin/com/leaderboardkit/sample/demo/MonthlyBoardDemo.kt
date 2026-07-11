package com.leaderboardkit.sample.demo

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.leaderboardkit.LeaderboardKit
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.sample.SampleUser
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.randomDemoScore
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * `TimeWindow.Monthly` — same shape as [WeeklyBoardDemo], one calendar-month
 * bucket instead of one calendar-week bucket (see `TimeWindowBucket.kt`'s
 * `"%04d-%02d"` branch). No reset countdown here — [ResetCountdown] is
 * hardcoded to weekly-reset math, and duplicating/generalizing it isn't worth
 * it just for this demo.
 */
@Composable
fun MonthlyBoardDemo(onBack: () -> Unit) {
    val config = remember {
        LeaderboardKit.buildConfig("monthly_coins") {
            timeWindow = TimeWindow.Monthly(resetTimeZone = TimeZone.UTC)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DemoScaffold(
        title = "Monthly",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        onSubmitRandomScore = {
            coroutineScope.launch {
                LeaderboardKit.submitScore(config, randomDemoScore(), SampleUser.PROFILE_METADATA)
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
