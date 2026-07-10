package com.leaderboardkit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI orchestrator for one leaderboard: `Intent -> [reduceLeaderboardState] -> State`.
 * Holds exactly one [MutableStateFlow] of [LeaderboardState] as the single source
 * of truth; every intent either updates it directly (pure) or launches a use-case
 * call whose *result* is fed back through [reduceLeaderboardState] (impure trigger,
 * pure consumption — see [LeaderboardChange] KDoc).
 *
 * ### currentUserEntry resolution
 * On every entries update, if [currentUserId] is present in the loaded window its
 * entry is mirrored straight from [entries]. If it's not there — the common case
 * for a large board where the user is way outside the top page — a one-shot
 * `getNearbyRanks(currentUserId, radius = 1, ...)` call resolves it instead. Per
 * the repository contract that call returns the anchor entry itself alongside its
 * neighbors, so `radius = 1` is the cheapest lookup that's guaranteed to include it.
 *
 * ### Upstream sharing
 * [entriesFlow] is the *only* place [LeaderboardDependencies.observeLeaderboard] is
 * collected, shared via `stateIn(..., SharingStarted.WhileSubscribed(5000))`. If a
 * host renders both [com.leaderboardkit.ui.screen.LeaderboardScreen] and a
 * [com.leaderboardkit.ui.screen.LeaderboardWidget] off the same ViewModel, or Compose
 * tears down and recreates a collector across a configuration change, neither
 * re-attaches a new Firestore listener/poll loop — they all observe the one shared
 * upstream. The 5s grace period survives a rotation without a full re-fetch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModel(
    initialConfig: LeaderboardConfig,
    private val currentUserId: String,
    private val dependencies: LeaderboardDependencies,
) : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardState(config = initialConfig))
    val state: StateFlow<LeaderboardState> = _state.asStateFlow()

    private val _effects = Channel<LeaderboardEffect>(Channel.BUFFERED)
    val effects: Flow<LeaderboardEffect> = _effects.receiveAsFlow()

    /** Config plus a monotonic token bumped by [refresh] to force a resubscribe even when [config] is unchanged. */
    private data class ObserveKey(val config: LeaderboardConfig, val refreshToken: Int)

    private val observeKey = MutableStateFlow(ObserveKey(initialConfig, refreshToken = 0))

    private val entriesFlow: StateFlow<List<LeaderboardEntry>> = observeKey
        .flatMapLatest { key -> dependencies.observeLeaderboard(key.config) }
        .catch { throwable -> dispatchFailure(throwable) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var resolveCurrentUserJob: Job? = null

    init {
        entriesFlow.onEach { entries ->
            _state.update { reduceLeaderboardState(it, LeaderboardChange.EntriesLoaded(entries)) }
            resolveCurrentUserEntry(entries)
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: LeaderboardIntent) {
        when (intent) {
            LeaderboardIntent.Refresh -> refresh()
            LeaderboardIntent.LoadMore -> loadMore()
            is LeaderboardIntent.ChangeTimeWindow -> changeConfig(_state.value.config.copy(timeWindow = intent.window))
            is LeaderboardIntent.ChangeScope -> changeConfig(_state.value.config.copy(scope = intent.scope))
            is LeaderboardIntent.SubmitScore -> submitScore(intent.score)
        }
    }

    private fun refresh() {
        _state.update { reduceLeaderboardState(it, LeaderboardChange.LoadingStarted) }
        observeKey.update { it.copy(refreshToken = it.refreshToken + 1) }
    }

    private fun changeConfig(newConfig: LeaderboardConfig) {
        _state.update { reduceLeaderboardState(it, LeaderboardChange.ConfigChanged(newConfig)) }
        observeKey.update { it.copy(config = newConfig) }
    }

    private fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.canLoadMore) return

        viewModelScope.launch {
            _state.update { reduceLeaderboardState(it, LeaderboardChange.LoadMoreStarted) }
            dependencies.loadMore(current.config)
                .onSuccess { hasMore -> _state.update { reduceLeaderboardState(it, LeaderboardChange.LoadMoreFinished(hasMore)) } }
                .onFailure { throwable -> dispatchFailure(throwable) }
        }
    }

    private fun submitScore(score: Long) {
        val config = _state.value.config
        viewModelScope.launch {
            dependencies.submitScore(currentUserId, score, config)
                .onSuccess {
                    _effects.trySend(LeaderboardEffect.ScrollToUserRank)
                    resolveCurrentUserEntry(_state.value.entries, force = true)
                }
                .onFailure { throwable -> dispatchFailure(throwable) }
        }
    }

    /**
     * @param force Skip the fast (already-in-window) path and always hit
     *   `getNearbyRanks` for a fresh read. Used right after [submitScore]: the
     *   [entries] passed in may still be the pre-submission snapshot on a
     *   [com.leaderboardkit.domain.model.RefreshStrategy.Polling]/[com.leaderboardkit.domain.model.RefreshStrategy.ManualOnly]
     *   board, so trusting it would show a stale score/rank until the next tick.
     */
    private fun resolveCurrentUserEntry(entries: List<LeaderboardEntry>, force: Boolean = false) {
        if (!force) {
            val inWindow = entries.firstOrNull { it.userId == currentUserId }
            if (inWindow != null) {
                _state.update { reduceLeaderboardState(it, LeaderboardChange.CurrentUserEntryResolved(inWindow)) }
                return
            }
        }

        resolveCurrentUserJob?.cancel()
        resolveCurrentUserJob = viewModelScope.launch {
            dependencies.getNearbyRanks(currentUserId, radius = 1, _state.value.config)
                .onSuccess { nearby ->
                    val self = nearby.firstOrNull { it.userId == currentUserId }
                    _state.update { reduceLeaderboardState(it, LeaderboardChange.CurrentUserEntryResolved(self)) }
                }
            // Best-effort secondary lookup: a failure here isn't surfaced as a
            // board-wide error, the main entries list is still fully usable.
        }
    }

    private fun dispatchFailure(throwable: Throwable) {
        val error = throwable.toLeaderboardError()
        _state.update { reduceLeaderboardState(it, LeaderboardChange.Failed(error)) }
        _effects.trySend(LeaderboardEffect.ShowError(error.toDisplayMessage()))
    }

    companion object {
        /** `viewModel(factory = LeaderboardViewModel.factory(...))` — see [LeaderboardDependencies] for why this isn't Hilt-injected yet. */
        fun factory(
            initialConfig: LeaderboardConfig,
            currentUserId: String,
            dependencies: LeaderboardDependencies,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { LeaderboardViewModel(initialConfig, currentUserId, dependencies) }
        }
    }
}
