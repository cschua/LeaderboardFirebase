package com.leaderboardkit.sampleretro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leaderboardkit.presentation.LeaderboardError
import com.leaderboardkit.presentation.toDisplayMessage
import com.leaderboardkit.sampleretro.ui.theme.RetroCyan
import com.leaderboardkit.sampleretro.ui.theme.RetroGreen
import com.leaderboardkit.sampleretro.ui.theme.RetroMagenta
import com.leaderboardkit.sampleretro.ui.theme.RetroMonoFont

/** Arcade-flavored `emptyStateContent` for [com.leaderboardkit.ui.screen.LeaderboardContent]. */
@Composable
fun RetroEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "NO SCORES YET",
            style = TextStyle(fontFamily = RetroMonoFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = RetroGreen),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "BE THE FIRST TO CLAIM THE TOP SPOT",
            style = TextStyle(fontFamily = RetroMonoFont, fontSize = 13.sp, color = RetroCyan),
            textAlign = TextAlign.Center,
        )
    }
}

/** Arcade-flavored `loadingStateContent` for [com.leaderboardkit.ui.screen.LeaderboardContent]. */
@Composable
fun RetroLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = RetroGreen)
        Spacer(Modifier.height(12.dp))
        Text("LOADING HIGH SCORES...", style = TextStyle(fontFamily = RetroMonoFont, fontSize = 13.sp, color = RetroGreen))
    }
}

/** Arcade-flavored `errorStateContent` for [com.leaderboardkit.ui.screen.LeaderboardContent] — retry dispatches [com.leaderboardkit.presentation.LeaderboardIntent.Refresh]. */
@Composable
fun RetroErrorState(error: LeaderboardError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "GAME OVER",
            style = TextStyle(fontFamily = RetroMonoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = RetroMagenta),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error.toDisplayMessage(),
            style = TextStyle(fontFamily = RetroMonoFont, fontSize = 13.sp, color = RetroCyan),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = RetroGreen, contentColor = Color.Black),
        ) {
            Text("CONTINUE?", style = TextStyle(fontFamily = RetroMonoFont, fontWeight = FontWeight.Bold))
        }
    }
}
