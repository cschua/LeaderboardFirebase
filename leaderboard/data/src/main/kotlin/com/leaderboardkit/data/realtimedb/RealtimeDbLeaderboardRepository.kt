package com.leaderboardkit.data.realtimedb

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.leaderboardkit.data.common.BaseBoardState
import com.leaderboardkit.data.common.EntryFields
import com.leaderboardkit.data.common.RepositoryUtils
import com.leaderboardkit.data.common.ScoreSubmissionHelper
import com.leaderboardkit.data.common.assignRanks
import com.leaderboardkit.data.common.rankFromAscendingIndex
import com.leaderboardkit.data.common.surroundingWindow
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.LeaderboardException
import com.leaderboardkit.domain.model.SortDirection
import com.leaderboardkit.domain.repository.LeaderboardRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * [LeaderboardRepository] backed by Realtime Database, mirroring
 * [com.leaderboardkit.data.firestore.FirestoreLeaderboardRepository]'s
 * `RefreshStrategy`/pagination handling.
 *
 * Realtime Database has no server-side aggregate `count()` the way Firestore
 * does, so [getUserRank]/[getSurroundingEntries] fall back to fetching the full
 * ordered entry list and locating the user client-side. Per the library's
 * documented rank-computation strategy, that's fine under roughly 1000 entries;
 * beyond that, denormalize a `rank` field via a scheduled Cloud Function/backend
 * job and read it directly instead of computing it here.
 */
@InternalLeaderboardKitApi
class RealtimeDbLeaderboardRepository(
    private val database: DatabaseReference,
    private val pathStrategy: RealtimeDbPathStrategy,
    private val mapper: RealtimeDbLeaderboardEntryMapper,
) : LeaderboardRepository {

    private data class Cursor(val score: Double, val key: String)

    private val boardStates = ConcurrentHashMap<String, BaseBoardState<Cursor>>()

    private fun stateFor(config: LeaderboardConfig): BaseBoardState<Cursor> =
        boardStates.getOrPut(config.boardId) { BaseBoardState() }

    private fun baseQuery(config: LeaderboardConfig): Query =
        database.child(pathStrategy.nodePath(config)).orderByChild("score")

    private fun scoreOf(snapshot: DataSnapshot): Double =
        (snapshot.child("score").value as? Number)?.toDouble() ?: 0.0

    @Suppress("UNCHECKED_CAST")
    private fun toEntries(rawAscendingChildren: List<DataSnapshot>): List<LeaderboardEntry> =
        rawAscendingChildren.map { child ->
            mapper.fromNode(child.key.orEmpty(), (child.value as? Map<String, Any?>).orEmpty())
        }

    /** Fetches one page, ordered for display (best rank first) regardless of [SortDirection]. */
    private suspend fun fetchPage(config: LeaderboardConfig, after: Cursor?): Pair<List<LeaderboardEntry>, Cursor?> {
        val query = when (config.sortDirection) {
            SortDirection.Descending -> {
                val q = if (after == null) baseQuery(config) else baseQuery(config).endBefore(after.score, after.key)
                q.limitToLast(config.pageSize)
            }
            SortDirection.Ascending -> {
                val q = if (after == null) baseQuery(config) else baseQuery(config).startAfter(after.score, after.key)
                q.limitToFirst(config.pageSize)
            }
        }
        val snapshot = query.get().await()
        val rawAscending = snapshot.children.toList() // RTDB always iterates ascending by the orderBy field
        if (rawAscending.isEmpty()) return emptyList<LeaderboardEntry>() to null

        val display = if (config.sortDirection == SortDirection.Descending) rawAscending.asReversed() else rawAscending
        val nextCursorRaw = if (config.sortDirection == SortDirection.Descending) rawAscending.first() else rawAscending.last()
        val nextCursor = Cursor(scoreOf(nextCursorRaw), nextCursorRaw.key.orEmpty())
        return toEntries(display) to nextCursor
    }

    override fun observeEntries(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> =
        RepositoryUtils.observeEntries(config, ::observeRealtime, ::fetchFirstPage)

    private suspend fun fetchFirstPage(config: LeaderboardConfig): List<LeaderboardEntry> {
        val (entries, cursor) = fetchPage(config, after = null)
        val ranked = assignRanks(entries, startRank = 1)
        val state = stateFor(config)
        state.update(
            newEntries = ranked,
            nextCursor = cursor,
            isEnd = entries.size < config.pageSize,
        )
        return ranked
    }

    private fun observeRealtime(config: LeaderboardConfig): Flow<List<LeaderboardEntry>> = callbackFlow {
        val query = when (config.sortDirection) {
            SortDirection.Descending -> baseQuery(config).limitToLast(config.pageSize)
            SortDirection.Ascending -> baseQuery(config).limitToFirst(config.pageSize)
        }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawAscending = snapshot.children.toList()
                val display = if (config.sortDirection == SortDirection.Descending) rawAscending.asReversed() else rawAscending
                val ranked = assignRanks(toEntries(display), startRank = 1)
                stateFor(config).loadedEntries = ranked
                trySend(ranked)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun loadMore(config: LeaderboardConfig): Result<Boolean> = runCatching {
        val state = stateFor(config)
        state.mutex.withLock {
            if (state.endReached) return@withLock false
            val (entries, cursor) = fetchPage(config, after = state.cursor)
            if (entries.isEmpty()) {
                state.endReached = true
                return@withLock false
            }
            val ranked = assignRanks(entries, startRank = state.loadedEntries.size + 1)
            state.update(
                newEntries = ranked,
                nextCursor = cursor,
                isEnd = entries.size < config.pageSize,
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
    ): Result<Unit> = runCatching {
        val nodeRef = database.child(pathStrategy.nodePath(config)).child(userId)
        val existing = nodeRef.get().await()
        val existingScore = (existing.child(EntryFields.SCORE).value as? Number)?.toLong()
        val shouldWrite = existingScore == null || ScoreSubmissionHelper.isBetter(score, existingScore, config)
        if (shouldWrite) {
            val entry = ScoreSubmissionHelper.createSubmissionEntry(
                userId = userId,
                score = score,
                metadata = metadata,
                existingDisplayName = existing.child(EntryFields.DISPLAY_NAME).value as? String,
                existingAvatarId = existing.child(EntryFields.AVATAR_ID).value as? String,
            )
            nodeRef.setValue(mapper.toNode(entry)).await()
        }
    }

    /** Fetches the full ordered entry set. Only safe for boards small enough for client-side ranking — see class KDoc. */
    private suspend fun fetchAllOrderedAscending(config: LeaderboardConfig): List<DataSnapshot> =
        baseQuery(config).get().await().children.toList()

    override suspend fun getUserRank(userId: String, config: LeaderboardConfig): Result<Int?> = runCatching {
        val all = fetchAllOrderedAscending(config)
        val index = all.indexOfFirst { it.key == userId }
        if (index < 0) return@runCatching null
        rankFromAscendingIndex(index, all.size, config.sortDirection)
    }

    override suspend fun getSurroundingEntries(
        userId: String,
        radius: Int,
        config: LeaderboardConfig,
    ): Result<List<LeaderboardEntry>> = runCatching {
        val all = fetchAllOrderedAscending(config)
        val index = all.indexOfFirst { it.key == userId }
        if (index < 0) throw LeaderboardException.UserNotFound(userId)

        val window = surroundingWindow(all, index, radius, config.sortDirection)
        assignRanks(toEntries(window.items), startRank = window.startRank)
    }
}
