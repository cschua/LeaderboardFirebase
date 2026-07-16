package com.leaderboardkit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.leaderboardkit.ProvideLeaderboardClient
import com.leaderboardkit.sample.demo.CustomRowBoardDemo
import com.leaderboardkit.sample.demo.CustomThemeBoardDemo
import com.leaderboardkit.sample.demo.FriendsBoardDemo
import com.leaderboardkit.sample.demo.GlobalBoardDemo
import com.leaderboardkit.sample.demo.MonthlyBoardDemo
import com.leaderboardkit.sample.demo.RealtimeDbBoardDemo
import com.leaderboardkit.sample.demo.WeeklyBoardDemo
import com.leaderboardkit.sample.ui.HomeScreen

/**
 * No navigation-compose dependency here on purpose: seven screens and a plain
 * `when` over [Destination] is simpler than pulling in a whole navigation
 * library for a sample app whose entire point is showcasing `:leaderboard:*`,
 * not screen-to-screen navigation patterns.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ProvideLeaderboardClient((application as SampleApplication).leaderboardClient) {
                        var destination by remember { mutableStateOf<Destination>(Destination.Home) }
                        when (destination) {
                            Destination.Home -> HomeScreen(onNavigate = { destination = it })
                            Destination.GlobalBoard -> GlobalBoardDemo(onBack = { destination = Destination.Home })
                            Destination.FriendsBoard -> FriendsBoardDemo(onBack = { destination = Destination.Home })
                            Destination.WeeklyBoard -> WeeklyBoardDemo(onBack = { destination = Destination.Home })
                            Destination.MonthlyBoard -> MonthlyBoardDemo(onBack = { destination = Destination.Home })
                            Destination.CustomTheme -> CustomThemeBoardDemo(onBack = { destination = Destination.Home })
                            Destination.CustomRow -> CustomRowBoardDemo(onBack = { destination = Destination.Home })
                            Destination.RealtimeDb -> RealtimeDbBoardDemo(onBack = { destination = Destination.Home })
                        }
                    }
                }
            }
        }
    }
}
