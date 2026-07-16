package com.leaderboardkit.sample.demo

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.leaderboardkit.LeaderboardKitConfig
import com.leaderboardkit.LeaderboardScreen
import com.leaderboardkit.ProvideLeaderboardClient
import com.leaderboardkit.createLeaderboardClient
import com.leaderboardkit.domain.model.LeaderboardBackend
import com.leaderboardkit.domain.model.RefreshStrategy
import com.leaderboardkit.sample.SampleUser
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.SubmissionDialog
import kotlinx.coroutines.launch
import java.util.UUID

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

    var showSubmitDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showNewUserDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    ProvideLeaderboardClient(rtdbClient) {
        val config = remember(rtdbClient) {
            rtdbClient.buildConfig("rtdb_global") {
                refreshStrategy = RefreshStrategy.RealtimeListener
            }
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        if (showSubmitDialog) {
            SubmissionDialog(
                title = "Submit Your Score",
                showNameField = false,
                onConfirm = { _, score ->
                    showSubmitDialog = false
                    scope.launch {
                        rtdbClient.submitScore(config, score, SampleUser.PROFILE_METADATA)
                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "Failed") }
                    }
                },
                onDismiss = { showSubmitDialog = false }
            )
        }

        if (showNewUserDialog) {
            SubmissionDialog(
                title = "Add New Test User",
                initialName = listOf("Alex", "Jordan", "Casey", "Taylor", "Riley", "Quinn").random(),
                onConfirm = { name, score ->
                    showNewUserDialog = false
                    scope.launch {
                        val randomId = "test_" + UUID.randomUUID().toString().take(8)
                        val randomAvatar = "avatar_0${(1..9).random()}"
                        val metadata = mapOf("displayName" to name, "avatarId" to randomAvatar)
                        rtdbClient.submitScore(config, randomId, score, metadata)
                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "Failed") }
                    }
                },
                onDismiss = { showNewUserDialog = false }
            )
        }

        DemoScaffold(
            title = "Realtime DB backend",
            onBack = onBack,
            snackbarHostState = snackbarHostState,
            onSubmitRandomScore = { showSubmitDialog = true },
            onSeedBoard = { showNewUserDialog = true },
        ) { modifier ->
            LeaderboardScreen(
                config = config,
                modifier = modifier,
                onShowError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            )
        }
    }
}
