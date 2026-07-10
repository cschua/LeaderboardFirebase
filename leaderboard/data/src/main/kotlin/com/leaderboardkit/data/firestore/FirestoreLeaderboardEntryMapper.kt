package com.leaderboardkit.data.firestore

import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.domain.model.LeaderboardEntry

private const val FIELD_USER_ID = "userId"
private const val FIELD_DISPLAY_NAME = "displayName"
private const val FIELD_AVATAR_ID = "avatarId"
private const val FIELD_SCORE = "score"
private const val FIELD_RANK = "rank"
private const val FIELD_METADATA = "metadata"

/**
 * Converts between [LeaderboardEntry] and the plain `Map<String, Any?>` shape
 * Firestore documents are read/written as.
 *
 * Deliberately takes the document id and its `.data` map rather than a
 * `DocumentSnapshot`/`QueryDocumentSnapshot` directly: it keeps this class unit
 * testable with plain maps (no emulator/Robolectric needed) and is the only place
 * in the module that knows the on-the-wire field names — repositories must never
 * map fields inline.
 */
class FirestoreLeaderboardEntryMapper {

    @Suppress("UNCHECKED_CAST")
    fun fromDocument(documentId: String, data: Map<String, Any?>): LeaderboardEntry = LeaderboardEntry(
        userId = data[FIELD_USER_ID] as? String ?: documentId,
        displayName = data[FIELD_DISPLAY_NAME] as? String ?: "",
        avatarId = data[FIELD_AVATAR_ID] as? String ?: AvatarDefaults.DEFAULT_AVATAR_ID,
        score = (data[FIELD_SCORE] as? Number)?.toLong() ?: 0L,
        rank = (data[FIELD_RANK] as? Number)?.toInt(),
        metadata = (data[FIELD_METADATA] as? Map<String, Any>) ?: emptyMap(),
    )

    fun toDocument(entry: LeaderboardEntry): Map<String, Any?> = buildMap {
        put(FIELD_USER_ID, entry.userId)
        put(FIELD_DISPLAY_NAME, entry.displayName)
        put(FIELD_AVATAR_ID, entry.avatarId)
        put(FIELD_SCORE, entry.score)
        put(FIELD_RANK, entry.rank)
        put(FIELD_METADATA, entry.metadata)
    }
}
