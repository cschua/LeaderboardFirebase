package com.leaderboardkit.data.firestore

import com.google.firebase.functions.FirebaseFunctions
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig
import kotlinx.coroutines.tasks.await

/**
 * Calls a callable Cloud Function (default name `"submitLeaderboardScore"`,
 * configurable via [functionName]) to validate and persist the score
 * server-side. This is the recommended submitter whenever score integrity
 * matters, since [DirectWriteScoreSubmitter] cannot stop a modified client from
 * writing an arbitrary value.
 *
 * The corresponding Cloud Function is **not** part of this library — it is
 * host-app-deployed, since anti-cheat validation logic is inherently
 * game-specific. This class only documents and implements the client-side call
 * contract:
 *
 * ```
 * // request
 * { "boardId": "...", "userId": "...", "score": 1234, "metadata": { ... } }
 * // response: HttpsCallableResult with no meaningful payload (throws on rejection)
 * ```
 */
@InternalLeaderboardKitApi
class CloudFunctionScoreSubmitter(
    private val functions: FirebaseFunctions,
    private val functionName: String = "submitLeaderboardScore",
) : ScoreSubmitter {

    override suspend fun submit(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any>,
    ): Result<Unit> = runCatching {
        val payload = mapOf(
            "boardId" to config.boardId,
            "userId" to userId,
            "score" to score,
            "metadata" to metadata,
        )
        functions.getHttpsCallable(functionName).call(payload).await()
    }
}
