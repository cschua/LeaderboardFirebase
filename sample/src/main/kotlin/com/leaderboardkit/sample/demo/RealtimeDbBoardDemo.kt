package com.leaderboardkit.sample.demo

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.leaderboardkit.LeaderboardKitConfig
import com.leaderboardkit.LeaderboardScreen
import com.leaderboardkit.ProvideLeaderboardClient
import com.leaderboardkit.createLeaderboardClient
import com.leaderboardkit.domain.model.LeaderboardBackend
import com.leaderboardkit.sample.SampleUser
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.rememberSubmitRandomScoreAction
import kotlinx.coroutines.launch

/**
 * Demonstrates the Realtime Database backend. This demo creates its own
 * [com.leaderboardkit.LeaderboardClient] configured with [LeaderboardBackend.RealtimeDatabase]
 * and scopes it over the screen, showing how to switch backends.
 */
@Composable
fun RealtimeDbBoardDemo(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val rtdbClient = remember(context) {
        createLeaderboardClient(
            context = context,
            config = LeaderboardKitConfig(
                currentUserId = { SampleUser.ID },
                backend = LeaderboardBackend.RealtimeDatabase,
            ),
        )
    }

    ProvideLeaderboardClient(rtdbClient) {
        val config = remember(rtdbClient) { rtdbClient.buildConfig("rtdb_global") }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        DemoScaffold(
            title = "Realtime DB backend",
            onBack = onBack,
            snackbarHostState = snackbarHostState,
            onSubmitRandomScore = rememberSubmitRandomScoreAction(rtdbClient, config, SampleUser.PROFILE_METADATA, snackbarHostState),
        ) { modifier ->
            LeaderboardScreen(
                config = config,
                modifier = modifier,
                onShowError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            )
        }
    }
}
