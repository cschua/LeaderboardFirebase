package com.leaderboardkit.sample.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.leaderboardkit.LeaderboardClient
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.formatResetCountdown
import kotlinx.coroutines.delay

/** Ticking "resets in ..." label for [config]'s time window, via [LeaderboardClient.timeUntilNextReset]. */
@Composable
fun ResetCountdown(client: LeaderboardClient, config: LeaderboardConfig, modifier: Modifier = Modifier) {
    var remaining by remember(client, config) { mutableStateOf(client.timeUntilNextReset(config)) }

    LaunchedEffect(client, config) {
        while (true) {
            remaining = client.timeUntilNextReset(config)
            delay(1_000)
        }
    }

    val label = remaining?.let { "Resets in ${formatResetCountdown(it)}" } ?: "Never resets"
    Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = modifier)
}
