# leaderboard-kit

A customizable, configurable leaderboard component for Android — Jetpack Compose UI, Kotlin
Coroutines/Flow, Firebase (Firestore primary, Realtime Database as a reference adapter), MVI
presentation layer, Clean Architecture module boundaries.

```kotlin
// Application.onCreate()
LeaderboardKit.initialize(
    context = this,
    config = LeaderboardKitConfig(currentUserId = { currentUserId() }),
)

// anywhere in a Composable
LeaderboardKit.screen(config = LeaderboardKit.buildConfig("global_alltime"))
```

## Module graph

```
:leaderboard:domain        pure Kotlin — models, LeaderboardRepository contract, use cases
:leaderboard:data          Firestore/Realtime Database repositories, mappers, Hilt modules
:leaderboard:presentation  MVI contract + LeaderboardViewModel
:leaderboard:ui            Compose screens, theme, row composables
:leaderboard:public-api    LeaderboardKit facade — the supported integration surface
:sample                    demo app exercising the facade only
```

`:leaderboard:public-api` only ever depends on the four lower modules and exposes `LeaderboardKit` +
`LeaderboardKitConfig`. The lower modules stay independently usable (and stay public, not `internal`)
for advanced integration — see [Advanced usage](#advanced-usage) — but `:leaderboard:public-api` is
the documented, supported entry point.

## Setup

### 1. Firebase project

1. Create (or reuse) a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app to it, download `google-services.json`, and place it in your app module
   (in this repo: `sample/google-services.json`).
3. Enable **Firestore** in the console (Realtime Database only if you plan to use the RTDB
   reference adapter directly instead of the facade — see [Advanced usage](#advanced-usage)).
4. Apply the `com.google.gms.google-services` Gradle plugin to your app module and add the
   Firebase BOM + `firebase-firestore` (`:leaderboard:public-api` already pulls in the
   Firestore SDK transitively — you don't need to add it again, just the `google-services` plugin
   and its config file).

### 2. Firestore security rules

Every board this library creates lives under `leaderboards/{boardId}/windows/{windowBucket}/...`
(see [`DefaultFirestorePathStrategy`](leaderboard/data/src/main/kotlin/com/leaderboardkit/data/firestore/FirestorePathStrategy.kt)).
A minimal rule set for the direct-write score submission path
([`DirectWriteScoreSubmitter`](leaderboard/data/src/main/kotlin/com/leaderboardkit/data/firestore/DirectWriteScoreSubmitter.kt)):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // leaderboards/{boardId}/windows/{windowBucket}/entries/{userId}
    // and the /friendsOf/{userId}, /category/{categoryId}, /custom/{filterId}
    // scoped variants one level deeper — see DefaultFirestorePathStrategy.
    match /leaderboards/{boardId}/windows/{windowBucket}/{scopePath=**}/entries/{userId} {
      // Anyone signed in can read a board (adjust to your app's visibility rules —
      // e.g. require friendship for the /friendsOf/ variant).
      allow read: if request.auth != null;

      // A user may only ever write their own entry document, and only their own userId.
      allow write: if request.auth != null
                   && request.auth.uid == userId
                   && request.resource.data.userId == userId
                   // Shape/range validation only — this is NOT anti-cheat. A modified
                   // client can still write any score within this range. See the
                   // Cloud Function score-write path below if that matters for your game.
                   && request.resource.data.score is int
                   && request.resource.data.score >= 0
                   && request.resource.data.score < 1000000000;
    }
  }
}
```

**This only validates shape, not legitimacy** — `DirectWriteScoreSubmitter`'s client-side rate
limiter is a UX/cost safeguard, not anti-cheat either. If a score needs to be trusted (competitive
boards, anything with real-world stakes), route submission through a Cloud Function instead
(`CloudFunctionScoreSubmitter` documents the client-side call contract; the Cloud Function itself
is host-app-deployed and explicitly out of scope for this library — see
[Non-goals](#non-goals--deferred-work)). With that path, security rules should deny direct client
writes to `entries/{userId}` entirely and only allow writes from the Cloud Function's service
account.

### 3. Minimal integration

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LeaderboardKit.initialize(
            context = this,
            config = LeaderboardKitConfig(currentUserId = { AuthRepository.currentUserId }),
        )
    }
}

@Composable
fun GameOverScreen() {
    LeaderboardKit.screen(config = LeaderboardKit.buildConfig("global_alltime"))
}
```

That's the whole integration for a single global board. See `:sample` for five more shapes
(friends, weekly-with-countdown, custom theme, custom row content).

## Configuration reference

Every leaderboard "shape" is one `LeaderboardConfig`, built via the `leaderboardConfig { }` DSL
(or `LeaderboardKit.buildConfig(boardId) { }`, which pre-fills `scope` from
`LeaderboardKitConfig.defaultScope`):

```kotlin
val config = leaderboardConfig("weekly_coins") {
    scope = LeaderboardScope.Friends(currentUserId, friendIds)
    timeWindow = TimeWindow.Weekly(resetTimeZone = TimeZone.of("UTC"))
    sortDirection = SortDirection.Descending
    tieBreak = TieBreak.EarliestAchievedFirst()
    pageSize = 25
    prefetchDistance = 5
    refreshStrategy = RefreshStrategy.Polling(45.seconds) // optional — see table below
}
```

| Field | Type | Notes |
|---|---|---|
| `boardId` | `String` | Stable logical id (e.g. `"global_alltime"`, `"weekly_coins"`). Keys the Firestore/RTDB path and the pagination cursor — two configs meant to be the same board **must** share it. |
| `scope` | `LeaderboardScope` | `Global`, `Friends(currentUserId, friendIds)`, `Category(categoryId)`, or `Custom(filterId, params)` for a host-defined query a custom path strategy interprets. Friend-graph resolution is entirely the host app's job — this library has no social graph concept. |
| `timeWindow` | `TimeWindow` | `AllTime`, `Daily(resetTimeZone)`, `Weekly(resetTimeZone)`, `Monthly(resetTimeZone)`, `Season(seasonId, range)`, or `Custom(range)`. Each window gets its own Firestore/RTDB path segment, so window expiry needs no purge pass — see `TimeWindowBucket`. |
| `sortDirection` | `SortDirection` | `Descending` (highest score wins — the common case) or `Ascending` (lowest wins — race times, error counts, ...). |
| `tieBreak` | `TieBreak` | `None`, `EarliestAchievedFirst(metadataKey)`, or `LatestAchievedFirst(metadataKey)`. The achievement timestamp is read from `LeaderboardEntry.metadata`, not a first-class field, since not every board tracks it. |
| `pageSize` | `Int` | Entries per page / per `Polling` tick. |
| `prefetchDistance` | `Int` | How many entries from the end of the loaded window the UI should trigger `loadMore()` at (advisory — `:leaderboard:ui` acts on it, the data layer doesn't). |
| `refreshStrategy` | `RefreshStrategy` | The cost-control knob — see below. Leave unset in the DSL to get the recommended default for the chosen `scope`/`timeWindow`. |

### RefreshStrategy — the cost-control knob

Firestore bills a read for every added/updated/removed document a real-time listener observes, so
an always-on listener over a large or frequently-changing result set is the single biggest cost
lever in this library.

| Board type | Default strategy | Rationale |
|---|---|---|
| Global / all-time | `Polling(45–60s)` | Large, high-churn result set; sub-minute rank precision rarely matters. |
| Friends | `RealtimeListener` | Small, bounded result set, so live cost stays low regardless of churn. |
| Weekly / seasonal / category | `Polling(2–5min)`, plus a hard recompute at window expiry | Matches the board lifecycle: continuous during the window, one-time reset (the window's own path segment makes the "reset" a no-op — old buckets are just never queried again). |
| Current-user rank + surrounding ±N | `RealtimeListener`, scoped to ±10 ranks | Small, bounded read cost where responsiveness matters most to the player. |

`RefreshStrategy` itself has three cases, all overridable per-config regardless of the table above:

```kotlin
sealed interface RefreshStrategy {
    data object RealtimeListener            // live Firestore snapshot listener — small, bounded sets only
    data class Polling(val interval: Duration) // one-shot fetch on a timer
    data object ManualOnly                  // fetch on screen entry / explicit refresh only, no background cost
}
```

## Theming reference

`LeaderboardTheme` is never constructed directly — call `rememberLeaderboardTheme(...)` inside a
composable, which derives every default from the ambient Material3 `ColorScheme`/`Typography` (so
dark/light mode and any app-level `MaterialTheme` are honored automatically) and lets you override
any field individually:

| Field | Type | Default | Visual effect |
|---|---|---|---|
| `colors.colorScheme` | `ColorScheme` | `MaterialTheme.colorScheme` | Base palette every row composable reads text/surface colors from — full Material3 passthrough, including dark mode. |
| `colors.topThreeHighlight` | `Color` | `colorScheme.tertiaryContainer` | Row background for rank 1–3. |
| `colors.currentUserRowHighlight` | `Color` | `colorScheme.primaryContainer` | Row background for the signed-in user's own entry (takes priority over the top-3 highlight if both apply). |
| `typography.displayName` | `TextStyle` | `MaterialTheme.typography.bodyLarge` | Player name text style. |
| `typography.score` | `TextStyle` | `MaterialTheme.typography.titleMedium` | Score text style (rendered with locale thousands separators via `ScoreLabel`). |
| `typography.rank` | `TextStyle` | `MaterialTheme.typography.labelLarge` | Rank number text style (used by `RankBadgeStyle.Numeric`, and as the numeric fallback for ranks below 3rd under `MedalIcon`). |
| `rowHeight` | `Dp` | `64.dp` | Fixed height of every `LeaderboardRow`. |
| `avatarShape` | `AvatarShape` | `Circle` | `Circle`, `Square`, or `RoundedSquare` clip applied to the resolved avatar drawable. |
| `rankBadgeStyle` | `RankBadgeStyle` | `Numeric` | `Numeric` (plain rank text), `MedalIcon` (gold/silver/bronze circles for 1–3, numeric below), or `Custom(content)` — a full `@Composable (rank: Int?) -> Unit` override. |
| `rankChangeAnimationSpec` | `FiniteAnimationSpec<IntOffset>` | `spring()` | Placement animation (`Modifier.animateItem`) when a row's position shifts after a rank change. |

Avatars are resolved separately from the theme, via `AvatarResolver` (`fun interface AvatarResolver
{ fun resolve(avatarId: String): Int }`, a `@DrawableRes` lookup — never a network fetch, since
avatars are a fixed, locally-bundled set). `DefaultAvatarResolver` ships 12 placeholder drawables
(`avatar_01`..`avatar_12`); supply your own `AvatarResolver` via `LeaderboardKitConfig.avatarResolver`
to use your own art.

For a row layout unlike anything `LeaderboardTheme` can express, skip theming it and pass
`rowContent` to `LeaderboardKit.screen(...)` instead — a full `@Composable (LeaderboardEntry,
isCurrentUser: Boolean) -> Unit` override, see `:sample`'s "custom row content" demo.

## Advanced usage

Most apps only ever touch `:leaderboard:public-api`. Depend on the lower modules directly instead if you need to:

- **Swap Firestore for the Realtime Database reference adapter**, or supply your own
  `LeaderboardRepository` — `LeaderboardKit` only ever wires up Firestore. Bind your own repository
  via one `@Binds` line in a Hilt module installed alongside `:leaderboard:data`'s (see
  `LeaderboardDataModule` KDoc for the exact snippet), then build your own `LeaderboardViewModel`
  from `:leaderboard:presentation`'s use cases instead of going through the facade.
- **Reach the MVI contract directly** (`LeaderboardState`/`LeaderboardIntent`/`LeaderboardEffect`,
  `LeaderboardViewModel`) — e.g. to drive a UI that isn't `:leaderboard:ui`'s `LeaderboardScreen`/`LeaderboardWidget`.
- **Score submission with server-side validation** — bind `CloudFunctionScoreSubmitter` in place of
  the default `DirectWriteScoreSubmitter`; see that class's KDoc for the client-call contract. The
  Cloud Function itself is not part of this library (see Non-goals).
- **Route a board through a custom Firestore/RTDB path strategy** (multi-tenant apps) — implement
  `FirestorePathStrategy`/`RealtimeDbPathStrategy`, typically by wrapping `DefaultFirestorePathStrategy`
  with a tenant prefix, and bind it in place of the default.

## Testing

Unit-tested, no Firebase Emulator required:

- **Use cases** (`:leaderboard:domain`) — all five (`Observe`, `SubmitScore`, `GetUserRank`,
  `GetNearbyRanks`, `LoadMore`), each against a hand-written repository double.
- **Reducer** (`:leaderboard:presentation`) — `reduceLeaderboardState`, one test per
  `LeaderboardChange` case, pure function, zero coroutines/backend involved.
- **Mappers** (`:leaderboard:data`) — Firestore and Realtime Database entry mappers, round-trip and
  missing-field-default cases.
- **Path strategies, rate limiter, reconnect policy, window-bucket math** (`:leaderboard:data`) —
  pure logic, no Firebase SDK objects needed.
- **`FakeLeaderboardRepository`** (`:leaderboard:data`) — exercised directly, and used to back
  ViewModel-level orchestration tests and every `:leaderboard:ui` `@Preview`.

**Not unit tested, by design**: `FirestoreLeaderboardRepository`/`RealtimeDbLeaderboardRepository`
themselves, and both `ScoreSubmitter` implementations — these are thin, mostly-mechanical wrappers
around Firestore/RTDB SDK calls (query construction, `Task<T>.await()`), and meaningfully testing
them needs the Firebase Emulator Suite rather than mocking the SDK. **Optional stretch goal, not
built here**: an `androidTest`/emulator-backed integration test module driving
`FirestoreLeaderboardRepository` against `firebase emulators:start --only firestore`, asserting
pagination/rank-computation/security-rules behavior against a real (local) Firestore instance.

## Non-goals / deferred work

- **Server-side anti-cheat / score validation** — only the documented extension point
  (`CloudFunctionScoreSubmitter`'s client contract) exists; no Cloud Function is implemented.
- **Realtime Database beyond the reference adapter** — `RealtimeDbLeaderboardRepository` exists and
  is tested at the mapper/path-strategy level, but `LeaderboardKit` never wires it up; Firestore is
  the only backend the facade supports.
- **Push notifications for rank changes** — plausible v2 feature, not built.
