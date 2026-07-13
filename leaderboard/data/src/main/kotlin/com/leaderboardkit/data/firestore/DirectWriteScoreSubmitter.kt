package com.leaderboardkit.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.data.ratelimit.ClientRateLimiter
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.LeaderboardException
import com.leaderboardkit.domain.model.SortDirection
import kotlinx.coroutines.tasks.await

/**
 * [displayName]/`avatarId` are not parameters of [ScoreSubmitter.submit] — this
 * library treats score submission and profile display fields as separate
 * concerns. If a caller wants to (re)set them alongside a score, it passes them
 * through `metadata["displayName"]` / `metadata["avatarId"]`; otherwise the
 * previously stored values are preserved (falling back to
 * [AvatarDefaults.DEFAULT_AVATAR_ID] for a brand new entry with no avatar
 * selection yet). Those two keys are stripped from the persisted `metadata`
 * map so they aren't duplicated with the top-level fields.
 */
@InternalLeaderboardKitApi
class DirectWriteScoreSubmitter(
    private val firestore: FirebaseFirestore,
    private val pathStrategy: FirestorePathStrategy,
    private val mapper: FirestoreLeaderboardEntryMapper,
    private val rateLimiter: ClientRateLimiter,
) : ScoreSubmitter {

    override suspend fun submit(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any>,
    ): Result<Unit> {
        rateLimiter.tryAcquire(userId, config.boardId)?.let { retryAfter ->
            return Result.failure(LeaderboardException.RateLimitExceeded(retryAfter))
        }

        return runCatching {
            val docRef = firestore.collection(pathStrategy.collectionPath(config)).document(userId)
            firestore.runTransaction { transaction ->
                val existing = transaction.get(docRef)
                val existingScore = (existing.get("score") as? Number)?.toLong()
                val shouldWrite = existingScore == null || isBetter(score, existingScore, config)
                if (shouldWrite) {
                    val entry = LeaderboardEntry(
                        userId = userId,
                        displayName = metadata["displayName"] as? String
                            ?: (existing.get("displayName") as? String).orEmpty(),
                        avatarId = metadata["avatarId"] as? String
                            ?: existing.get("avatarId") as? String
                            ?: AvatarDefaults.DEFAULT_AVATAR_ID,
                        score = score,
                        rank = null,
                        metadata = metadata - "displayName" - "avatarId",
                    )
                    transaction.set(docRef, mapper.toDocument(entry))
                }
            }.await()
            Unit
        }
    }

    private fun isBetter(candidate: Long, existing: Long, config: LeaderboardConfig): Boolean =
        when (config.sortDirection) {
            SortDirection.Descending -> candidate > existing
            SortDirection.Ascending -> candidate < existing
        }
}
