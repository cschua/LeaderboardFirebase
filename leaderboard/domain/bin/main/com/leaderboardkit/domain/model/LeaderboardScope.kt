package com.leaderboardkit.domain.model

/**
 * Who is eligible to appear on the board.
 */
sealed interface LeaderboardScope {

    /** Every user, unfiltered. Typically large and high-churn — see [RefreshStrategy]. */
    data object Global : LeaderboardScope

    /**
     * Restricted to [currentUserId] and [friendUserIds]. Friend-graph resolution is
     * the host app's responsibility (e.g. a separate social/friends module) — this
     * scope just carries the already-resolved whitelist, keeping the leaderboard
     * library decoupled from any particular social graph implementation.
     */
    data class Friends(
        val currentUserId: String,
        val friendUserIds: List<String>,
    ) : LeaderboardScope

    /** A single category/event/game-mode board, identified by [categoryId]. */
    data class Category(val categoryId: String) : LeaderboardScope

    /**
     * An escape hatch for host-app-defined filtering that doesn't fit the built-in
     * scopes. [filterId] is an opaque identifier that a data-layer path/query
     * strategy is expected to interpret; [params] carries whatever that strategy
     * needs. The domain layer never inspects these values.
     */
    data class Custom(
        val filterId: String,
        val params: Map<String, Any> = emptyMap(),
    ) : LeaderboardScope
}
