package com.leaderboardkit.ui.avatar

import androidx.annotation.DrawableRes

/**
 * Resolves [com.leaderboardkit.domain.model.LeaderboardEntry.avatarId] — a key
 * into a fixed, locally-bundled avatar set — to the drawable resource for it.
 * Avatars are never fetched over the network, so this is a plain synchronous
 * lookup, not a suspend function or an image-loading pipeline.
 *
 * Host apps with their own avatar art supply their own implementation (a `when`
 * over their ids, or their own map); [DefaultAvatarResolver] is the bundled
 * fallback set for apps that don't need custom art.
 */
fun interface AvatarResolver {
    @DrawableRes
    fun resolve(avatarId: String): Int
}
