package com.leaderboardkit.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import java.text.NumberFormat
import java.util.Locale

/** [score] formatted with locale-appropriate thousands separators (e.g. "12,345"). */
@Composable
fun ScoreLabel(
    score: Long,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Text(text = formatScore(score), style = textStyle, modifier = modifier)
}

private fun formatScore(score: Long): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(score)

@Preview(showBackground = true)
@Composable
private fun ScoreLabelPreview() {
    ScoreLabel(score = 1_234_567)
}
