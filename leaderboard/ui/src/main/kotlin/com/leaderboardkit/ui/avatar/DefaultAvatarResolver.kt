package com.leaderboardkit.ui.avatar

import androidx.annotation.DrawableRes
import com.leaderboardkit.ui.R

/**
 * The bundled fallback avatar set: 12 simple placeholder drawables
 * (`avatar_01`..`avatar_12`), matching the id space `:leaderboard:data`'s
 * `AvatarDefaults.PLACEHOLDER_AVATAR_IDS` fixtures use. An unrecognized
 * [AvatarResolver.resolve] id (e.g. a host app's own custom id passed through
 * without a matching [AvatarResolver]) falls back to `avatar_01` rather than
 * crashing — a leaderboard row should never fail to render over a missing avatar.
 */
object DefaultAvatarResolver : AvatarResolver {

    private val byId: Map<String, Int> = mapOf(
        "avatar_01" to R.drawable.avatar_01,
        "avatar_02" to R.drawable.avatar_02,
        "avatar_03" to R.drawable.avatar_03,
        "avatar_04" to R.drawable.avatar_04,
        "avatar_05" to R.drawable.avatar_05,
        "avatar_06" to R.drawable.avatar_06,
        "avatar_07" to R.drawable.avatar_07,
        "avatar_08" to R.drawable.avatar_08,
        "avatar_09" to R.drawable.avatar_09,
        "avatar_10" to R.drawable.avatar_10,
        "avatar_11" to R.drawable.avatar_11,
        "avatar_12" to R.drawable.avatar_12,
    )

    @DrawableRes
    override fun resolve(avatarId: String): Int = byId[avatarId] ?: R.drawable.avatar_01
}
