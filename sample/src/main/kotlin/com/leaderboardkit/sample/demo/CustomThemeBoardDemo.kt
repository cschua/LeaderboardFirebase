package com.leaderboardkit.sample.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leaderboardkit.LeaderboardKit
import com.leaderboardkit.sample.SampleUser
import com.leaderboardkit.sample.ui.DemoScaffold
import com.leaderboardkit.sample.ui.randomDemoScore
import com.leaderboardkit.ui.theme.AvatarShape
import com.leaderboardkit.ui.theme.RankBadgeStyle
import com.leaderboardkit.ui.theme.rememberLeaderboardTheme
import kotlinx.coroutines.launch

private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFF7C4DFF)

/**
 * Everything [rememberLeaderboardTheme] exposes, changed at once: a dark
 * Material3 [androidx.compose.material3.ColorScheme] (colors), monospace
 * display-name/score text styles (typography), square avatars instead of the
 * default circles, and medal icons instead of numeric rank badges.
 */
@Composable
fun CustomThemeBoardDemo(onBack: () -> Unit) {
    val config = remember { LeaderboardKit.buildConfig("custom_theme_demo") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme(primary = NeonCyan, secondary = NeonPurple)) {
        val theme = rememberLeaderboardTheme(
            topThreeHighlight = NeonPurple.copy(alpha = 0.35f),
            currentUserRowHighlight = NeonCyan.copy(alpha = 0.25f),
            displayNameStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            scoreStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NeonCyan),
            rankStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            rowHeight = 72.dp,
            avatarShape = AvatarShape.Square,
            rankBadgeStyle = RankBadgeStyle.MedalIcon,
        )

        DemoScaffold(
            title = "Custom theme",
            onBack = onBack,
            snackbarHostState = snackbarHostState,
            onSubmitRandomScore = {
                coroutineScope.launch {
                    LeaderboardKit.submitScore(config, randomDemoScore(), SampleUser.PROFILE_METADATA)
                        .onFailure { snackbarHostState.showSnackbar(it.message ?: "Submission failed") }
                }
            },
        ) { modifier ->
            LeaderboardKit.screen(
                config = config,
                theme = theme,
                modifier = modifier,
                onShowError = { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
            )
        }
    }
}
