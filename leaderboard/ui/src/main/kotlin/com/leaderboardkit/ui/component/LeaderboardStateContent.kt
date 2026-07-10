package com.leaderboardkit.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leaderboardkit.presentation.LeaderboardError
import com.leaderboardkit.presentation.toDisplayMessage
import kotlin.time.Duration.Companion.seconds

/** Default `emptyStateContent` for [com.leaderboardkit.ui.screen.LeaderboardScreen]. */
@Composable
fun DefaultEmptyLeaderboardState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "No scores yet", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Be the first to make the board.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Default `loadingStateContent` for [com.leaderboardkit.ui.screen.LeaderboardScreen]. */
@Composable
fun DefaultLoadingLeaderboardState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Default `errorStateContent` for [com.leaderboardkit.ui.screen.LeaderboardScreen]. */
@Composable
fun DefaultErrorLeaderboardState(
    error: LeaderboardError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = error.toDisplayMessage(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Retry")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultEmptyLeaderboardStatePreview() {
    DefaultEmptyLeaderboardState()
}

@Preview(showBackground = true)
@Composable
private fun DefaultLoadingLeaderboardStatePreview() {
    DefaultLoadingLeaderboardState()
}

@Preview(showBackground = true)
@Composable
private fun DefaultErrorLeaderboardStatePreview() {
    DefaultErrorLeaderboardState(error = LeaderboardError.RateLimited(5.seconds), onRetry = {})
}
