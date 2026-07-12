package com.leaderboardkit.data.firestore

import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardConfig

/**
 * The write path used by [FirestoreLeaderboardRepository.submitScore]. Two
 * implementations are provided and neither is picked silently — the host app
 * binds one via Hilt:
 *
 * - [DirectWriteScoreSubmitter]: the client writes straight to Firestore inside a
 *   transaction, guarded by [com.leaderboardkit.data.ratelimit.ClientRateLimiter].
 *   Simplest, lowest latency, no Cloud Functions deployment required — but a
 *   modified client can submit an arbitrary score, since Firestore security rules
 *   can only validate shape/range, not "did this player actually earn this".
 * - [CloudFunctionScoreSubmitter]: the client calls a callable Cloud Function,
 *   which validates and performs the write server-side. Required whenever score
 *   integrity matters (competitive/ranked boards, anything with real-world
 *   stakes) since it's the only path immune to a hacked client. Costs a Functions
 *   invocation per submission and adds a server round trip.
 *
 * Default binding (see `LeaderboardBackendModule`) is [DirectWriteScoreSubmitter];
 * switch to [CloudFunctionScoreSubmitter] by rebinding when anti-cheat matters.
 */
@InternalLeaderboardKitApi
interface ScoreSubmitter {
    suspend fun submit(
        userId: String,
        score: Long,
        config: LeaderboardConfig,
        metadata: Map<String, Any>,
    ): Result<Unit>
}
