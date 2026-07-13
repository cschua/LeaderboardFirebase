package com.leaderboardkit.data.firestore

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.RefreshStrategy
import com.leaderboardkit.domain.model.SortDirection
import com.leaderboardkit.domain.repository.LeaderboardRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * [LeaderboardRepository] backed by Firestore. See [ScoreSubmitter] for the two
 * supported score-write paths and [FirestorePathStrategy] for how boards map to
 * collections.
 *
 * ### RefreshStrategy handling
 * - [RefreshStrategy.RealtimeListener]: [callbackFlow] + `addSnapshotListener`,
 *   torn down in [awaitClose]. On (re)attach after a >30min gap,
 *   [ListenerReconnectPolicy] forces one server-sourced read first rather than
 *   trusting a stale cached snapshot (see its KDoc).
 * - [RefreshStrategy.Polling]: a cold `flow { while (true) { emit(fetch()); delay(interval) } }`.
 *   Being a cold flow, the loop is cancelled automatically once the last
 *   collector goes away — the equivalent of `SharingStarted.WhileSubscribed` for a
 *   single-collector repository method. If a caller wants to multicast one poll
 *   loop across multiple simultaneous collectors, that's a presentation-layer
 *   concern (`.stateIn(scope, SharingStarted.WhileSubscribed(5000), ...)`), not
 *   something this repository does on its own — doing so here would mean owning a
 *   long-lived `CoroutineScope`, which conflicts with "structured concurrency
 *   only, no `GlobalScope`".
 * - [RefreshStrategy.ManualOnly]: a single fetch-and-emit, no loop. Re-fetching
 *   (pull-to-refresh) means re-collecting the flow.
 *
 * ### Pagination
 * Cursor-based, per [LeaderboardConfig.boardId] (see [LeaderboardRepository] KDoc
 * for why this library doesn't use `Flow<PagingData>`). [loadMore] is guarded by a
 * per-board [Mutex] so concurrent calls are serialized rather than racing.
 */
@InternalLeaderboardKitApi
class FirestoreLeaderboardRepository(
    private val firestore: FirebaseFirestore,
    private val pathStrategy: FirestorePathStrategy,
    private val mapper: FirestoreLeaderboardEntryMapper,
    private val scoreSubmitter: ScoreSubmitter,
) : LeaderboardRepository {

    private class BoardState {
        val mutex = Mutex()
        var loadedEntries: List<LeaderboardEntry> = emptyList()
        var lastDocument: DocumentSnapshot? = null
        var endReached: Boolean = false
    }

    private val boardStates = ConcurrentHashMap<String, BoardState>()
    private val reconnectPolicy = ListenerReconnectPolicy()

    private fun stateFor(config: LeaderboardConfig): BoardState =
        boardStates.getOrPut(boardKey(config)) { BoardState() }

    private fun boardKey(config: LeaderboardConfig): String = config.boardId

    private fun baseQuery(config: LeaderboardConfig): Query {
        val direction = when (config.sortDirection) {
            SortDirection.Descending -> Query.Direction.DESCENDING
            SortDirection.Ascending -> Query.Direction.ASCENDING
        }
        return firestore.collection(pathStrategy.collectionPath(config))
            .orderBy("score", direction)
    }

    private fun assignRanks(entries: List<LeaderboardEntry>, startRank: Int): List<LeaderboardEntry> =
        entries.mapIndexed { index, entry -> entry.copy(rank = startRank + index) }

    private suspend fun fetchFirstPage(config: LeaderboardConfig, source: Source = Source.DEFAULT): List<LeaderboardEntry> {
        val snapshot = baseQuery(config).limit(config.pageSize.toLong()).get(source).await()
        val entries = assignRanks(snapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) }, startRank = 1)
        val state = stateFor(config)
        state.loadedEntries = entries
        state.lastDocument = snapshot.documents.lastOrNull()
        state.endReached = snapshot.documents.size < config.pageSize
        return entries
    }

    override fun observeEntries(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> =
        when (val strategy = config.refreshStrategy) {
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

    private fun observeRealtime(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> = callbackFlow {
        val key = boardKey(config)
        val source = if (reconnectPolicy.shouldForceServerRead(key)) Source.SERVER else Source.DEFAULT
        if (source == Source.SERVER) {
            runCatching { fetchFirstPage(config, source = Source.SERVER) }
        }

        val query = baseQuery(config).limit(config.pageSize.toLong())
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                reconnectPolicy.markActive(key)
                val entries = assignRanks(
                    snapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) },
                    startRank = 1,
                )
                val state = stateFor(config)
                state.loadedEntries = entries
                state.lastDocument = snapshot.documents.lastOrNull()
                state.endReached = snapshot.documents.size < config.pageSize
                trySend(entries)
            }
        }

        awaitClose {
            reconnectPolicy.markInactive(key)
            registration.remove()
        }
    }

    override suspend fun loadMore(config: LeaderboardConfig): Result<Boolean> = runCatching {
        val state = stateFor(config)
        state.mutex.withLock {
            if (state.endReached) return@withLock false
            val cursor = state.lastDocument
            val query = baseQuery(config).limit(config.pageSize.toLong()).let {
                if (cursor != null) it.startAfter(cursor) else it
            }
            val snapshot = query.get().await()
            if (snapshot.documents.isEmpty()) {
                state.endReached = true
                return@withLock false
            }
            val nextPage = assignRanks(
                snapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) },
                startRank = state.loadedEntries.size + 1,
            )
            state.loadedEntries = state.loadedEntries + nextPage
            state.lastDocument = snapshot.documents.last()
            state.endReached = snapshot.documents.size < config.pageSize
            true
        }
    }

    override suspend fun submitScore(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any>,
    ): Result<Unit> = scoreSubmitter.submit(userId, score, config, metadata)

    private suspend fun countBetter(config: LeaderboardConfig, score: Long): Long {
        val betterQuery = when (config.sortDirection) {
            SortDirection.Descending -> baseQuery(config).whereGreaterThan("score", score)
            SortDirection.Ascending -> baseQuery(config).whereLessThan("score", score)
        }
        return betterQuery.count().get(AggregateSource.SERVER).await().count
    }

    override suspend fun getUserRank(userId: String, config: LeaderboardConfig): Result<Int?> = runCatching {
        val userDoc = firestore.collection(pathStrategy.collectionPath(config)).document(userId).get().await()
        if (!userDoc.exists()) return@runCatching null
        val userScore = (userDoc.get("score") as? Number)?.toLong() ?: return@runCatching null
        (countBetter(config, userScore) + 1).toInt()
    }

    override suspend fun getSurroundingEntries(
        userId: String,
        radius: Int,
        config: LeaderboardConfig,
    ): Result<List<LeaderboardEntry>> = runCatching {
        val collection = firestore.collection(pathStrategy.collectionPath(config))
        val anchorDoc = collection.document(userId).get().await()
        if (!anchorDoc.exists()) return@runCatching emptyList()
        val anchorScore = (anchorDoc.get("score") as? Number)?.toLong()
            ?: return@runCatching emptyList()
        val anchorRank = (countBetter(config, anchorScore) + 1).toInt()
        val anchorEntry = mapper.fromDocument(anchorDoc.id, anchorDoc.data.orEmpty()).copy(rank = anchorRank)

        val (betterOp, worseOp) = when (config.sortDirection) {
            SortDirection.Descending -> Query.Direction.ASCENDING to Query.Direction.DESCENDING
            SortDirection.Ascending -> Query.Direction.DESCENDING to Query.Direction.ASCENDING
        }

        val aboveSnapshot = collection
            .let { if (config.sortDirection == SortDirection.Descending) it.whereGreaterThan("score", anchorScore) else it.whereLessThan("score", anchorScore) }
            .orderBy("score", betterOp)
            .limit(radius.toLong())
            .get().await()
        val above = assignRanks(
            aboveSnapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) }.asReversed(),
            startRank = (anchorRank - aboveSnapshot.documents.size).coerceAtLeast(1),
        )

        val belowSnapshot = collection
            .let { if (config.sortDirection == SortDirection.Descending) it.whereLessThan("score", anchorScore) else it.whereGreaterThan("score", anchorScore) }
            .orderBy("score", worseOp)
            .limit(radius.toLong())
            .get().await()
        val below = assignRanks(
            belowSnapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) },
            startRank = anchorRank + 1,
        )

        above + anchorEntry + below
    }
}
