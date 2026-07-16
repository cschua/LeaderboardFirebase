package com.leaderboardkit.domain.model

/**
 * DSL entry point for building a [LeaderboardConfig]:
 *
 * ```
 * val config = leaderboardConfig("weekly_coins") {
 *     scope = LeaderboardScope.Friends(currentUserId, friendIds)
 *     timeWindow = TimeWindow.Weekly(resetTimeZone = TimeZone.of("UTC"))
 *     pageSize = 25
 *     tieBreak = TieBreak.EarliestAchievedFirst()
 *     refreshStrategy = RefreshStrategy.Polling(45.seconds)
 * }
 * ```
 *
 * [LeaderboardConfigBuilder.refreshStrategy] may be left unset: if so, [build]
 * fills it in from [RefreshStrategyDefaults] based on the chosen [scope] and
 * [timeWindow], per the recommended-defaults table on [RefreshStrategy].
 */
fun leaderboardConfig(boardId: String, block: LeaderboardConfigBuilder.() -> Unit): LeaderboardConfig =
    LeaderboardConfigBuilder(boardId).apply(block).build()

class LeaderboardConfigBuilder(private val boardId: String) {
    var scope: LeaderboardScope = LeaderboardScope.Global
    var timeWindow: TimeWindow = TimeWindow.AllTime
    var sortDirection: SortDirection = SortDirection.Descending
    var tieBreak: TieBreak = TieBreak.None
    var pageSize: Int = 25
    var prefetchDistance: Int = 5

    /** Left null to fall back to [RefreshStrategyDefaults.recommendedFor]. */
    var refreshStrategy: RefreshStrategy? = null

    fun build(): LeaderboardConfig = LeaderboardConfig(
        boardId = boardId,
        scope = scope,
        timeWindow = timeWindow,
        sortDirection = sortDirection,
        tieBreak = tieBreak,
        pageSize = pageSize,
        prefetchDistance = prefetchDistance,
        refreshStrategy = refreshStrategy ?: RefreshStrategyDefaults.recommendedFor(scope, timeWindow),
    )
}
