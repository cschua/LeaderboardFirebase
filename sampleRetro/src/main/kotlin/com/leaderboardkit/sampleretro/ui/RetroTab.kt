package com.leaderboardkit.sampleretro.ui

import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.RefreshStrategy
import com.leaderboardkit.domain.model.TimeWindow
import com.leaderboardkit.domain.model.leaderboardConfig
import kotlinx.datetime.TimeZone

/** Every tab in this app reads/writes the same board — only the time window differs. */
const val RETRO_BOARD_ID = "retro_arcade"

/**
 * The three time-window views onto the shared [RETRO_BOARD_ID] board. Every tab
 * builds [RefreshStrategy.RealtimeListener] rather than [RefreshStrategyDefaults]'s
 * Polling default (45s-3min depending on window) — this demo board is small and
 * hand-tested, so instant feedback after "INSERT COIN" matters far more than the
 * read-cost savings a production-scale global board would need (see
 * [RefreshStrategy] KDoc's recommended-defaults table).
 */
enum class RetroTab(val label: String, val timeWindow: TimeWindow) {
    WEEKLY("WEEKLY", TimeWindow.Weekly(resetTimeZone = TimeZone.UTC)),
    MONTHLY("MONTHLY", TimeWindow.Monthly(resetTimeZone = TimeZone.UTC)),
    ALL_TIME("ALL-TIME", TimeWindow.AllTime),
    ;

    fun buildConfig(): LeaderboardConfig = leaderboardConfig(RETRO_BOARD_ID) {
        timeWindow = this@RetroTab.timeWindow
        refreshStrategy = RefreshStrategy.RealtimeListener
    }
}
