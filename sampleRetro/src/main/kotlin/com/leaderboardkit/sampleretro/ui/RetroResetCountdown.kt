@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.sampleretro.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.leaderboardkit.data.common.formatCountdown
import com.leaderboardkit.data.common.timeUntilNextReset
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.sampleretro.ui.theme.RetroCyan
import com.leaderboardkit.sampleretro.ui.theme.RetroMonoFont
import kotlinx.coroutines.delay

/** "NEXT LEVEL IN..." countdown for [timeWindow]'s next reset, via `:leaderboard:data`'s `timeUntilNextReset`. */
@Composable
fun RetroResetCountdown(timeWindow: TimeWindow, modifier: Modifier = Modifier) {
    var remaining by remember(timeWindow) { mutableStateOf(timeWindow.timeUntilNextReset()) }

    LaunchedEffect(timeWindow) {
        while (true) {
            remaining = timeWindow.timeUntilNextReset()
            delay(1_000)
        }
    }

    Text(
        text = remaining?.let { "NEXT LEVEL IN ${formatCountdown(it)}" } ?: "",
        style = TextStyle(fontFamily = RetroMonoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetroCyan),
        modifier = modifier,
    )
}
