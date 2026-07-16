package com.leaderboardkit.data.firestore

import com.leaderboardkit.data.common.EntryMapper
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardEntry

/**
 * Converts between [LeaderboardEntry] and the plain `Map<String, Any?>` shape
 * Firestore documents are read/written as.
 */
@InternalLeaderboardKitApi
class FirestoreLeaderboardEntryMapper {

    fun fromDocument(documentId: String, data: Map<String, Any?>): LeaderboardEntry =
        EntryMapper.fromMap(documentId, data)

    fun toDocument(entry: LeaderboardEntry): Map<String, Any?> =
        EntryMapper.toMap(entry, includeUserId = true, includeRank = true)
}
