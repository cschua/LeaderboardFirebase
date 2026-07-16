package com.leaderboardkit.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.leaderboardkit.data.common.EntryFields
import com.leaderboardkit.data.common.ScoreSubmissionHelper
import com.leaderboardkit.data.ratelimit.ClientRateLimiter
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import com.leaderboardkit.domain.model.LeaderboardException
import kotlinx.coroutines.tasks.await

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
                val existingScore = (existing.get(EntryFields.SCORE) as? Number)?.toLong()
                val shouldWrite = existingScore == null || ScoreSubmissionHelper.isBetter(score, existingScore, config)
                if (shouldWrite) {
                    val entry = ScoreSubmissionHelper.createSubmissionEntry(
                        userId = userId,
                        score = score,
                        metadata = metadata,
                        existingDisplayName = existing.get(EntryFields.DISPLAY_NAME) as? String,
                        existingAvatarId = existing.get(EntryFields.AVATAR_ID) as? String,
                    )
                    transaction.set(docRef, mapper.toDocument(entry))
                }
            }.await()
        }
    }
}
