package com.leaderboardkit.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leaderboardkit.ui.theme.RankBadgeStyle

private val GoldMedal = Color(0xFFFFD700)
private val SilverMedal = Color(0xFFC0C0C0)
private val BronzeMedal = Color(0xFFCD7F32)

private const val RANK_FIRST = 1
private const val RANK_SECOND = 2
private const val RANK_THIRD = 3

/** Renders [rank] per [style]. `null` (unranked) always falls back to a "-" placeholder. */
@Composable
fun RankBadge(
    rank: Int?,
    style: RankBadgeStyle,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
) {
    when (style) {
        RankBadgeStyle.Numeric -> NumericRankBadge(rank, modifier, textStyle)
        RankBadgeStyle.MedalIcon -> when (rank) {
            RANK_FIRST -> MedalRankBadge(GoldMedal, rank, modifier)
            RANK_SECOND -> MedalRankBadge(SilverMedal, rank, modifier)
            RANK_THIRD -> MedalRankBadge(BronzeMedal, rank, modifier)
            else -> NumericRankBadge(rank, modifier, textStyle)
        }
        is RankBadgeStyle.Custom -> style.content(rank)
    }
}

@Composable
private fun NumericRankBadge(rank: Int?, modifier: Modifier, textStyle: TextStyle) {
    Text(text = rank?.toString() ?: "-", style = textStyle, modifier = modifier)
}

@Composable
private fun MedalRankBadge(color: Color, rank: Int, modifier: Modifier) {
    Box(
        modifier = modifier.size(28.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = rank.toString(), style = MaterialTheme.typography.labelMedium, color = Color.Black.copy(alpha = 0.7f))
    }
}

@Preview(showBackground = true)
@Composable
private fun RankBadgeNumericPreview() {
    RankBadge(rank = 42, style = RankBadgeStyle.Numeric)
}

@Preview(showBackground = true)
@Composable
private fun RankBadgeMedalPreview() {
    RankBadge(rank = 1, style = RankBadgeStyle.MedalIcon)
}
