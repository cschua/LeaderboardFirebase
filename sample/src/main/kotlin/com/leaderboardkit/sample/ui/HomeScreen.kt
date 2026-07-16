package com.leaderboardkit.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leaderboardkit.sample.Destination

private data class DemoEntry(
    val title: String,
    val subtitle: String,
    val destination: Destination,
    val icon: ImageVector,
)

private val demos = listOf(
    DemoEntry(
        title = "Global all-time",
        subtitle = "LeaderboardScope.Global, TimeWindow.AllTime — defaults to Polling(45s)",
        destination = Destination.GlobalBoard,
        icon = Icons.Filled.Star,
    ),
    DemoEntry(
        title = "Friends",
        subtitle = "LeaderboardScope.Friends — defaults to RealtimeListener",
        destination = Destination.FriendsBoard,
        icon = Icons.Filled.Person,
    ),
    DemoEntry(
        title = "Weekly, with reset countdown",
        subtitle = "TimeWindow.Weekly — defaults to Polling(3min)",
        destination = Destination.WeeklyBoard,
        icon = Icons.Filled.Refresh,
    ),
    DemoEntry(
        title = "Monthly",
        subtitle = "TimeWindow.Monthly — defaults to Polling(3min)",
        destination = Destination.MonthlyBoard,
        icon = Icons.Filled.DateRange,
    ),
    DemoEntry(
        title = "Fully custom theme",
        subtitle = "Dark palette, square avatars, medal rank badges, custom typography",
        destination = Destination.CustomTheme,
        icon = Icons.Filled.Build,
    ),
    DemoEntry(
        title = "Custom row content",
        subtitle = "rowContent slot replaces LeaderboardRow entirely",
        destination = Destination.CustomRow,
        icon = Icons.AutoMirrored.Filled.List,
    ),
    DemoEntry(
        title = "Realtime Database backend",
        subtitle = "Configured to use Firebase Realtime DB instead of Firestore",
        destination = Destination.RealtimeDb,
        icon = Icons.Filled.Refresh,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (Destination) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard Kit", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Explore what :leaderboard: looks like configured for different scopes, windows, and themes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(demos) { demo ->
                DemoCard(demo = demo, onClick = { onNavigate(demo.destination) })
            }
        }
    }
}

@Composable
private fun DemoCard(demo: DemoEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = demo.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = demo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = demo.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
