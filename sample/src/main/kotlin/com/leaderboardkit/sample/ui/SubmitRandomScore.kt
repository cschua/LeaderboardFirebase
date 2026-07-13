package com.leaderboardkit.sample.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.leaderboardkit.LeaderboardClient
import com.leaderboardkit.domain.model.LeaderboardConfig
import kotlinx.coroutines.launch

/**
 * The "submit a random score" [DemoScaffold] FAB action shared by every demo
 * screen: fires [LeaderboardClient.submitScore] on [config] and surfaces a
 * failure through [snackbarHostState], the same way [com.leaderboardkit.LeaderboardScreen]'s
 * own `onShowError` does for read-path errors. Centralizing this here (rather than
 * each demo hand-rolling its own `coroutineScope.launch { ... }` block) keeps
 * that submit-and-report-failure policy defined in exactly one place.
 */
@Composable
fun rememberSubmitRandomScoreAction(
    client: LeaderboardClient,
    config: LeaderboardConfig,
    metadata: Map<String, Any>,
    snackbarHostState: SnackbarHostState,
): () -> Unit {
    val scope = rememberCoroutineScope()
    return remember(client, config, metadata, snackbarHostState) {
        {
            scope.launch {
                client.submitScore(config, randomDemoScore(), metadata)
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Submission failed") }
            }
        }
    }
}
