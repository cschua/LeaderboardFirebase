package com.leaderboardkit.data.firestore

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.leaderboardkit.data.common.BaseBoardState
import com.leaderboardkit.data.common.RepositoryUtils
import com.leaderboardkit.data.common.aboveWindowStartRank
import com.leaderboardkit.data.common.assignRanks
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.LeaderboardException
import com.leaderboardkit.domain.model.RefreshStrategy
import com.leaderboardkit.domain.model.SortDirection
import com.leaderboardkit.domain.model.TieBreak
import com.leaderboardkit.domain.repository.LeaderboardRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

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

    private val boardStates = ConcurrentHashMap<String, BaseBoardState<DocumentSnapshot>>()
    private val reconnectPolicy = ListenerReconnectPolicy()

    private fun stateFor(config: LeaderboardConfig): BaseBoardState<DocumentSnapshot> =
        boardStates.getOrPut(boardKey(config)) { BaseBoardState() }

    private suspend fun <T> withNetworkTimeout(block: suspend () -> T): T = try {
        withTimeout(10.seconds) { block() }
    } catch (_: TimeoutCancellationException) {
        throw LeaderboardException.NetworkTimeout("Firestore request timed out. Check your connection or Firebase console.")
    }

    private fun boardKey(config: LeaderboardConfig): String = config.boardId

    private fun baseQuery(config: LeaderboardConfig): Query {
        val direction = when (config.sortDirection) {
            SortDirection.Descending -> Query.Direction.DESCENDING
            SortDirection.Ascending -> Query.Direction.ASCENDING
        }
        var query = firestore.collection(pathStrategy.collectionPath(config))
            .orderBy("score", direction)

        when (val tb = config.tieBreak) {
            is TieBreak.None -> {}
            is TieBreak.EarliestAchievedFirst -> {
                query = query.orderBy("metadata.${tb.metadataKey}", Query.Direction.ASCENDING)
            }
            is TieBreak.LatestAchievedFirst -> {
                query = query.orderBy("metadata.${tb.metadataKey}", Query.Direction.DESCENDING)
            }
        }
        return query
    }

    private suspend fun fetchFirstPage(config: LeaderboardConfig, source: Source = Source.DEFAULT): List<LeaderboardEntry> {
        val snapshot = withNetworkTimeout { baseQuery(config).limit(config.pageSize.toLong()).get(source).await() }
        val entries = assignRanks(snapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) }, startRank = 1)
        val state = stateFor(config)
        state.update(
            newEntries = entries,
            nextCursor = snapshot.documents.lastOrNull(),
            isEnd = snapshot.documents.size < config.pageSize,
        )
        return entries
    }

    override fun observeEntries(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> =
        RepositoryUtils.observeEntries(config, ::observeRealtime, ::fetchFirstPage)

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
                state.update(
                    newEntries = entries,
                    nextCursor = snapshot.documents.lastOrNull(),
                    isEnd = snapshot.documents.size < config.pageSize,
                )
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
            val cursor = state.cursor
            val query = baseQuery(config).limit(config.pageSize.toLong()).let {
                if (cursor != null) it.startAfter(cursor) else it
            }
            val snapshot = withNetworkTimeout { query.get().await() }
            if (snapshot.documents.isEmpty()) {
                state.endReached = true
                return@withLock false
            }
            val nextPage = assignRanks(
                snapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) },
                startRank = state.loadedEntries.size + 1,
            )
            state.update(
                newEntries = nextPage,
                nextCursor = snapshot.documents.last(),
                isEnd = snapshot.documents.size < config.pageSize,
                append = true,
            )
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
        return withNetworkTimeout { betterQuery.count().get(AggregateSource.SERVER).await().count }
    }

    override suspend fun getUserRank(userId: String, config: LeaderboardConfig): Result<Int?> = runCatching {
        val userDoc = withNetworkTimeout { firestore.collection(pathStrategy.collectionPath(config)).document(userId).get().await() }
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
        val anchorDoc = withNetworkTimeout { collection.document(userId).get().await() }
        if (!anchorDoc.exists()) throw LeaderboardException.UserNotFound(userId)
        val anchorScore = (anchorDoc.get("score") as? Number)?.toLong()
            ?: return@runCatching emptyList()
        val anchorRank = (withNetworkTimeout { countBetter(config, anchorScore) } + 1).toInt()
        val anchorEntry = mapper.fromDocument(anchorDoc.id, anchorDoc.data.orEmpty()).copy(rank = anchorRank)

        val (betterOp, worseOp) = when (config.sortDirection) {
            SortDirection.Descending -> Query.Direction.ASCENDING to Query.Direction.DESCENDING
            SortDirection.Ascending -> Query.Direction.DESCENDING to Query.Direction.ASCENDING
        }

        val aboveSnapshot = withNetworkTimeout {
            collection
                .let {
                    if (config.sortDirection == SortDirection.Descending) {
                        it.whereGreaterThan("score", anchorScore)
                    } else {
                        it.whereLessThan("score", anchorScore)
                    }
                }
                .orderBy("score", betterOp)
                .limit(radius.toLong())
                .get().await()
        }
        val above = assignRanks(
            aboveSnapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) }.asReversed(),
            startRank = aboveWindowStartRank(anchorRank, aboveSnapshot.documents.size),
        )

        val belowSnapshot = withNetworkTimeout {
            collection
                .let {
                    if (config.sortDirection == SortDirection.Descending) {
                        it.whereLessThan("score", anchorScore)
                    } else {
                        it.whereGreaterThan("score", anchorScore)
                    }
                }
                .orderBy("score", worseOp)
                .limit(radius.toLong())
                .get().await()
        }
        val below = assignRanks(
            belowSnapshot.documents.map { mapper.fromDocument(it.id, it.data.orEmpty()) },
            startRank = anchorRank + 1,
        )

        above + anchorEntry + below
    }
}
