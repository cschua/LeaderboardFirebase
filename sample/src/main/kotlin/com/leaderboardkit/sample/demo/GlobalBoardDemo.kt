package com.leaderboardkit.sample.demo

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.leaderboardkit.LeaderboardKit
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.randomDemoScore
import kotlinx.coroutines.launch

/**
 * `LeaderboardScope.Global` + `TimeWindow.AllTime` — the config's own
 * [com.leaderboardkit.domain.model.RefreshStrategyDefaults] resolves this to
 * `Polling(45s)` with no explicit `refreshStrategy` line, since a global board is
 * large and high-churn (see the README's RefreshStrategy table).
 */
@Composable
fun GlobalBoardDemo(onBack: () -> Unit) {
    val config = remember { LeaderboardKit.buildConfig("global_alltime") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DemoScaffold(
        title = "Global all-time",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        onSubmitRandomScore = {
            scope.launch {
                LeaderboardKit.submitScore(config, randomDemoScore())
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Submission failed") }
            }
        },
    ) { modifier ->
        LeaderboardKit.screen(
            config = config,
            modifier = modifier,
            onShowError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
        )
    }
}
