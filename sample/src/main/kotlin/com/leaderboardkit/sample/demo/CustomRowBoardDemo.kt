package com.leaderboardkit.sample.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leaderboardkit.LeaderboardKit
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.randomDemoScore
import kotlinx.coroutines.launch

/**
 * `rowContent` fully replaces [com.leaderboardkit.ui.component.LeaderboardRow] —
 * this row doesn't even show an avatar, proving the slot really does bypass the
 * library's default row rendering rather than layering on top of it. Everything
 * else (pagination, the pinned off-screen current-user row, refresh strategy
 * handling) keeps working unchanged: only the per-row visuals are swapped.
 */
@Composable
fun CustomRowBoardDemo(onBack: () -> Unit) {
    val config = remember { LeaderboardKit.buildConfig("custom_row_demo") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DemoScaffold(
        title = "Custom row content",
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
            rowContent = { entry, isCurrentUser ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isCurrentUser) Color(0xFFFFF9C4) else Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#${entry.rank ?: "-"}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp),
                    )
                    Text(text = entry.displayName, modifier = Modifier.weight(1f))
                    Text(
                        text = "${entry.score} pts",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
    }
}
