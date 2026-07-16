package com.leaderboardkit.domain.annotations

/**
 * Marks declarations that are public only for cross-module wiring inside LeaderboardKit
 * (`:leaderboard:data`, `:leaderboard:presentation`) or for advanced host-app customization
 * (custom path strategies, the MVI contract, alternate backends) — not the stable, default
 * integration surface `:leaderboard:public-api` hands most host apps. They can change or be
 * removed without the usual deprecation cycle.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal LeaderboardKit API. It is public for cross-module " +
        "wiring and advanced customization, but is not considered a stable public contract.",
)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class InternalLeaderboardKitApi
