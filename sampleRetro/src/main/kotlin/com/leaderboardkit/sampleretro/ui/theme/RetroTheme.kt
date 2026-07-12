package com.leaderboardkit.sampleretro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leaderboardkit.ui.theme.AvatarShape
import com.leaderboardkit.ui.theme.LeaderboardTheme
import com.leaderboardkit.ui.theme.RankBadgeStyle
import com.leaderboardkit.ui.theme.rememberLeaderboardTheme

val RetroBackground = Color(0xFF0A0A14)
val RetroSurface = Color(0xFF16162E)
val RetroGreen = Color(0xFF39FF14)
val RetroMagenta = Color(0xFFFF2E97)
val RetroCyan = Color(0xFF00F0FF)
val RetroAmber = Color(0xFFFFD500)

val RetroMonoFont = FontFamily.Monospace

private val RetroColorScheme = darkColorScheme(
    primary = RetroGreen,
    onPrimary = Color.Black,
    secondary = RetroMagenta,
    onSecondary = Color.Black,
    tertiary = RetroCyan,
    background = RetroBackground,
    onBackground = RetroGreen,
    surface = RetroSurface,
    onSurface = RetroGreen,
    error = RetroMagenta,
    onError = Color.Black,
)

/** App-wide dark arcade-cabinet palette — everything in `sampleRetro` renders inside this. */
@Composable
fun RetroAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RetroColorScheme, content = content)
}

/**
 * Retro skin for [com.leaderboardkit.ui.screen.LeaderboardContent]'s default row/theme
 * knobs — same technique as `:sample`'s `CustomThemeBoardDemo`, pushed further into
 * neon monospace type, square avatars, and medal ranks.
 */
@Composable
fun rememberRetroLeaderboardTheme(): LeaderboardTheme = rememberLeaderboardTheme(
    colorScheme = RetroColorScheme,
    topThreeHighlight = RetroMagenta.copy(alpha = 0.22f),
    currentUserRowHighlight = RetroCyan.copy(alpha = 0.20f),
    displayNameStyle = TextStyle(
        fontFamily = RetroMonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = RetroGreen,
        letterSpacing = 1.sp,
    ),
    scoreStyle = TextStyle(
        fontFamily = RetroMonoFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        color = RetroAmber,
    ),
    rankStyle = TextStyle(
        fontFamily = RetroMonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = RetroCyan,
    ),
    rowHeight = 72.dp,
    avatarShape = AvatarShape.Square,
    rankBadgeStyle = RankBadgeStyle.MedalIcon,
)
