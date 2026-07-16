package com.leaderboardkit.data.common

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.RefreshStrategy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@InternalLeaderboardKitApi
object RepositoryUtils {

    fun observeEntries(
        config: LeaderboardConfig,
        observeRealtime: (LeaderboardConfig) -> Flow<List<LeaderboardEntry>>,
        fetchFirstPage: suspend (LeaderboardConfig) -> List<LeaderboardEntry>,
    ): Flow<List<LeaderboardEntry>> = when (val strategy = config.refreshStrategy) {
        is RefreshStrategy.RealtimeListener -> observeRealtime(config)
        is RefreshStrategy.Polling -> flow {
            while (true) {
                emit(fetchFirstPage(config))
                delay(strategy.interval)
            }
        }
        is RefreshStrategy.ManualOnly -> flow {
            emit(fetchFirstPage(config))
        }
    }
}
