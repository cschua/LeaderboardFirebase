package com.leaderboardkit.domain.model

/**
 * Which Firebase service to use as the source of truth for leaderboard data.
 */
enum class LeaderboardBackend {
    /**
     * Use Cloud Firestore. Recommended for most use cases — supports complex queries,
     * server-side aggregation (`count()`), and better scaling for large datasets.
     */
    Firestore,

    /**
     * Use Realtime Database. Ideal for very high-frequency updates or when
     * lower latency is required. Note: uses client-side ranking for some operations.
     */
    RealtimeDatabase
}
