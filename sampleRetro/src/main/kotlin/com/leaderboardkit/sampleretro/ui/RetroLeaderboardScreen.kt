package com.leaderboardkit.sampleretro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaderboardkit.presentation.LeaderboardDependencies
import com.leaderboardkit.presentation.LeaderboardEffect
import com.leaderboardkit.presentation.LeaderboardIntent
import com.leaderboardkit.presentation.LeaderboardViewModel
import com.leaderboardkit.sampleretro.RetroUser
import com.leaderboardkit.sampleretro.ui.theme.RetroBackground
import com.leaderboardkit.sampleretro.ui.theme.RetroGreen
import com.leaderboardkit.sampleretro.ui.theme.RetroMonoFont
import com.leaderboardkit.sampleretro.ui.theme.RetroSurface
import com.leaderboardkit.sampleretro.ui.theme.rememberRetroLeaderboardTheme
import com.leaderboardkit.ui.screen.LeaderboardContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drives one [LeaderboardViewModel] against the MVI contract exposed directly by
 * `:leaderboard:presentation` ([LeaderboardViewModel.state]/[LeaderboardViewModel.onIntent]/
 * [LeaderboardViewModel.effects]) and rendered through `:leaderboard:ui`'s stateless
 * [LeaderboardContent] — the same "reach for the MVI contract directly" path
 * `:leaderboard:public-api`'s `LeaderboardKit.screen` opts out of by default (see
 * its KDoc). Tab selection dispatches [LeaderboardIntent.ChangeTimeWindow] against
 * the one [RetroTab.buildConfig]'d [LeaderboardViewModel] rather than mounting
 * three separate screens: a single board with three windows is exactly the
 * "swap the window, keep everything else" transition the ViewModel already owns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroLeaderboardScreen(
    dependencies: LeaderboardDependencies,
    currentUserId: String,
    modifier: Modifier = Modifier,
) {
    val initialConfig = remember { RetroTab.ALL_TIME.buildConfig() }
    val viewModel: LeaderboardViewModel = viewModel(
        factory = remember(dependencies) { LeaderboardViewModel.factory(initialConfig, currentUserId, dependencies) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val theme = rememberRetroLeaderboardTheme()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LeaderboardEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                LeaderboardEffect.ScrollToUserRank -> {
                    val index = viewModel.state.value.entries.indexOfFirst { it.userId == currentUserId }
                    if (index >= 0) listState.animateScrollToItem(index)
                }
            }
        }
    }

    val selectedTab = RetroTab.entries.firstOrNull { it.timeWindow == state.config.timeWindow } ?: RetroTab.ALL_TIME

    Scaffold(
        modifier = modifier,
        containerColor = RetroBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "RETRO ARCADE",
                            style = TextStyle(fontFamily = RetroMonoFont, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RetroSurface, titleContentColor = RetroGreen),
                )
                RetroTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> viewModel.onIntent(LeaderboardIntent.ChangeTimeWindow(tab.timeWindow)) },
                )
                if (selectedTab == RetroTab.WEEKLY) {
                    RetroResetCountdown(
                        modifier = Modifier.fillMaxWidth().background(RetroSurface).padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        submitScoreToAllWindows(dependencies, currentUserId, randomRetroScore())
                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "SUBMISSION FAILED") }
                    }
                },
                containerColor = RetroGreen,
                contentColor = Color.Black,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("INSERT COIN", style = TextStyle(fontFamily = RetroMonoFont, fontWeight = FontWeight.Bold)) },
            )
        },
    ) { padding ->
        LeaderboardContent(
            state = state,
            onIntent = viewModel::onIntent,
            currentUserId = currentUserId,
            theme = theme,
            modifier = Modifier.padding(padding).fillMaxSize().background(RetroBackground),
            listState = listState,
            emptyStateContent = { RetroEmptyState() },
            errorStateContent = { error, onRetry -> RetroErrorState(error, onRetry) },
            loadingStateContent = { RetroLoadingState() },
        )
    }
}

@Composable
private fun RetroTabRow(selectedTab: RetroTab, onTabSelected: (RetroTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(RetroSurface).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RetroTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, if (selected) RetroGreen else RetroGreen.copy(alpha = 0.35f), RectangleShape)
                    .background(if (selected) RetroGreen.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = TextStyle(fontFamily = RetroMonoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = if (selected) RetroGreen else RetroGreen.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun randomRetroScore(): Long = Random.nextLong(100, 10_000)

/**
 * One "INSERT COIN" tap writes [score] into all three [RetroTab] window buckets —
 * [com.leaderboardkit.data.firestore.DefaultFirestorePathStrategy] gives every
 * [com.leaderboardkit.domain.model.TimeWindow] its own Firestore collection, so a
 * single [LeaderboardDependencies.submitScore] call only ever lands in whichever
 * one [com.leaderboardkit.domain.model.LeaderboardConfig.timeWindow] names. Fanning out here keeps the score
 * consistent no matter which tab the player checks afterward, rather than only
 * wherever they happened to be looking when they submitted. The 300ms stagger
 * clears [com.leaderboardkit.data.ratelimit.ClientRateLimiter]'s per-`(userId, boardId)`
 * cooldown between writes — see `RetroApplication`'s rate limiter wiring.
 */
private suspend fun submitScoreToAllWindows(
    dependencies: LeaderboardDependencies,
    userId: String,
    score: Long,
): Result<Unit> {
    for (tab in RetroTab.entries) {
        val result = dependencies.submitScore(userId, score, tab.buildConfig(), RetroUser.PROFILE_METADATA)
        if (result.isFailure) return result
        delay(300)
    }
    return Result.success(Unit)
}
