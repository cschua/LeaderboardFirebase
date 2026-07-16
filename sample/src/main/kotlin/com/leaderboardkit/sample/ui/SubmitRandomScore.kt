package com.leaderboardkit.sample.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.leaderboardkit.LeaderboardClient
import com.leaderboardkit.domain.model.LeaderboardConfig
import kotlinx.coroutines.launch
import java.util.UUID

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
    asNewUser: Boolean = false,
): () -> Unit {
    val scope = rememberCoroutineScope()
    // Local state to keep track of the "current" score for this session so clicking
    // "Submit" always shows an improvement.
    var sessionScore by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    return remember(client, config, metadata, snackbarHostState, asNewUser) {
        {
            scope.launch {
                val result = if (asNewUser) {
                    val randomId = "test_" + UUID.randomUUID().toString().take(8)
                    val randomName = listOf("Alex", "Jordan", "Casey", "Taylor", "Riley", "Quinn").random()
                    val randomAvatar = "avatar_0${(1..9).random()}"
                    val testMetadata = metadata + mapOf("displayName" to randomName, "avatarId" to randomAvatar)
                    client.submitScore(config, randomId, randomDemoScore(), testMetadata)
                } else {
                    val nextScore = randomDemoScore(sessionScore)
                    sessionScore = nextScore
                    client.submitScore(config, nextScore, metadata)
                }

                result.onFailure { snackbarHostState.showSnackbar(it.message ?: "Submission failed") }
            }
        }
    }
}
