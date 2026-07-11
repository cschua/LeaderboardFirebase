package com.leaderboardkit.sample.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leaderboardkit.sample.Destination

private data class DemoEntry(val title: String, val subtitle: String, val destination: Destination)

private val demos = listOf(
    DemoEntry(
        title = "Global all-time",
        subtitle = "LeaderboardScope.Global, TimeWindow.AllTime — defaults to Polling(45s)",
        destination = Destination.GlobalBoard,
    ),
    DemoEntry(
        title = "Friends",
        subtitle = "LeaderboardScope.Friends — defaults to RealtimeListener",
        destination = Destination.FriendsBoard,
    ),
    DemoEntry(
        title = "Weekly, with reset countdown",
        subtitle = "TimeWindow.Weekly — defaults to Polling(3min)",
        destination = Destination.WeeklyBoard,
    ),
    DemoEntry(
        title = "Monthly",
        subtitle = "TimeWindow.Monthly — defaults to Polling(3min)",
        destination = Destination.MonthlyBoard,
    ),
    DemoEntry(
        title = "Fully custom theme",
        subtitle = "Dark palette, square avatars, medal rank badges, custom typography",
        destination = Destination.CustomTheme,
    ),
    DemoEntry(
        title = "Custom row content",
        subtitle = "rowContent slot replaces LeaderboardRow entirely",
        destination = Destination.CustomRow,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (Destination) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Leaderboard Kit Sample") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(demos) { demo ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = { onNavigate(demo.destination) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = demo.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = demo.subtitle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
