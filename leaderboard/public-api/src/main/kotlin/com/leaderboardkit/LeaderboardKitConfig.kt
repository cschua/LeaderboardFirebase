package com.leaderboardkit

import com.leaderboardkit.domain.model.LeaderboardScope
import com.leaderboardkit.ui.avatar.AvatarResolver
import com.leaderboardkit.ui.avatar.DefaultAvatarResolver
import com.leaderboardkit.ui.theme.LeaderboardTheme

/**
 * One-time [LeaderboardKit.initialize] input. This is the only configuration
 * object most host apps construct by hand — everything below `:leaderboard:ui`
 * (repositories, mappers, the ViewModel) is wired up internally from it.
 *
 * @param firebaseAppName Name of an already-initialized secondary
 *   [com.google.firebase.FirebaseApp] to use (see `FirebaseApp.initializeApp(context, options, name)`),
 *   or `null` for the default app — this is the "Firebase app selection" knob:
 *   most apps never set it, multi-tenant hosts running more than one Firebase
 *   project in-process do.
 * @param currentUserId Called every time the library needs to know "who is the
 *   current player" (row highlighting, score submission, rank lookups). A
 *   lambda rather than a fixed `String` because the signed-in user can change
 *   after `initialize()` runs (e.g. sign-out/sign-in) — this library has no
 *   concept of auth state itself, it only ever asks the host for the current
 *   value.
 * @param defaultScope Used by [LeaderboardKit.buildConfig] to pre-fill
 *   [LeaderboardScope] so most boards don't need to repeat it.
 * @param defaultTheme A fully-resolved [LeaderboardTheme] to use whenever
 *   [LeaderboardKit.screen]/[LeaderboardKit.widget] aren't given one explicitly,
 *   for apps that want one consistent look everywhere without passing a theme at
 *   every call site. Leave `null` (the common case) to fall back to
 *   `rememberLeaderboardTheme()`'s Material3-derived defaults at each call site.
 * @param avatarResolver How `avatarId`s resolve to drawables — override only if
 *   your app ships its own avatar art instead of the bundled placeholder set.
 */
data class LeaderboardKitConfig(
    val currentUserId: () -> String,
    val firebaseAppName: String? = null,
    val defaultScope: LeaderboardScope = LeaderboardScope.Global,
    val defaultTheme: LeaderboardTheme? = null,
    val avatarResolver: AvatarResolver = DefaultAvatarResolver,
)
