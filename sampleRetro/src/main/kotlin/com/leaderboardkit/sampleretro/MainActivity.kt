package com.leaderboardkit.sampleretro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.leaderboardkit.sampleretro.ui.RetroLeaderboardScreen
import com.leaderboardkit.sampleretro.ui.theme.RetroAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dependencies = (application as RetroApplication).leaderboardDependencies
        setContent {
            RetroAppTheme {
                RetroLeaderboardScreen(
                    dependencies = dependencies,
                    currentUserId = RetroUser.ID,
                )
            }
        }
    }
}
