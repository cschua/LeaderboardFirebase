@file:OptIn(InternalLeaderboardKitApi::class)

package com.leaderboardkit.data.firestore

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.leaderboardkit.data.common.AvatarDefaults
import com.leaderboardkit.data.ratelimit.ClientRateLimiter
import com.leaderboardkit.domain.annotations.InternalLeaderboardKitApi
import com.leaderboardkit.domain.model.LeaderboardException
import com.leaderboardkit.domain.model.SortDirection
import com.leaderboardkit.domain.model.leaderboardConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Exercises [DirectWriteScoreSubmitter.submit]'s decision logic (rate limiting,
 * better-score comparison, displayName/avatarId preserve-vs-override merge) with
 * a fully mocked Firestore SDK — no emulator involved. [firestore]/[transaction]/
 * [existingSnapshot] stand in for the real SDK types; [mapper] and [pathStrategy]
 * stay close to real ([FirestoreLeaderboardEntryMapper] is genuinely pure) so the
 * data actually written to [transaction] is meaningfully asserted on.
 */
class DirectWriteScoreSubmitterTest {

    private val firestore = mockk<FirebaseFirestore>()
    private val collectionRef = mockk<CollectionReference>()
    private val docRef = mockk<DocumentReference>()
    private val transaction = mockk<Transaction>()
    private val existingSnapshot = mockk<DocumentSnapshot>()
    private val pathStrategy = FirestorePathStrategy { "boards/test" }
    private val mapper = FirestoreLeaderboardEntryMapper()
    private val rateLimiter = mockk<ClientRateLimiter>()
    private val written = slot<Map<String, Any?>>()

    private val submitter = DirectWriteScoreSubmitter(firestore, pathStrategy, mapper, rateLimiter)

    @Before
    fun setUp() {
        coEvery { rateLimiter.tryAcquire(any(), any()) } returns null
        every { firestore.collection("boards/test") } returns collectionRef
        every { collectionRef.document(any()) } returns docRef
        every { transaction.get(docRef) } returns existingSnapshot
        every { transaction.set(docRef, capture(written)) } returns transaction

        val transactionFunction = slot<Transaction.Function<Void>>()
        every { firestore.runTransaction(capture(transactionFunction)) } answers {
            transactionFunction.captured.apply(transaction)
            Tasks.forResult(null)
        }
    }

    private fun stubExisting(score: Long? = null, displayName: String? = null, avatarId: String? = null) {
        every { existingSnapshot.get("score") } returns score
        every { existingSnapshot.get("displayName") } returns displayName
        every { existingSnapshot.get("avatarId") } returns avatarId
    }

    @Test
    fun `blocked by the rate limiter never touches Firestore`() = runTest {
        coEvery { rateLimiter.tryAcquire("u1", "board") } returns 5.seconds

        val result = submitter.submit("u1", 500L, leaderboardConfig("board") {}, emptyMap())

        assertThat(result.exceptionOrNull()).isInstanceOf(LeaderboardException.RateLimitExceeded::class.java)
        verify(exactly = 0) { firestore.collection(any()) }
    }

    @Test
    fun `first submission for a user with no existing document writes unconditionally`() = runTest {
        stubExisting(score = null)

        val result = submitter.submit("u1", 500L, leaderboardConfig("board") {}, mapOf("displayName" to "Alice"))

        assertThat(result.isSuccess).isTrue()
        verify { transaction.set(docRef, any<Map<String, Any?>>()) }
    }

    @Test
    fun `descending board overwrites when the new score is higher`() = runTest {
        stubExisting(score = 100L)

        val result = submitter.submit("u1", 150L, leaderboardConfig("board") { sortDirection = SortDirection.Descending }, emptyMap())

        assertThat(result.isSuccess).isTrue()
        verify { transaction.set(docRef, any<Map<String, Any?>>()) }
    }

    @Test
    fun `descending board keeps the existing score when the new one is lower`() = runTest {
        stubExisting(score = 100L)

        val result = submitter.submit("u1", 50L, leaderboardConfig("board") { sortDirection = SortDirection.Descending }, emptyMap())

        assertThat(result.isSuccess).isTrue()
        verify(exactly = 0) { transaction.set(any(), any()) }
    }

    @Test
    fun `ascending board overwrites when the new score is lower`() = runTest {
        stubExisting(score = 100L)

        val result = submitter.submit("u1", 50L, leaderboardConfig("board") { sortDirection = SortDirection.Ascending }, emptyMap())

        assertThat(result.isSuccess).isTrue()
        verify { transaction.set(docRef, any<Map<String, Any?>>()) }
    }

    @Test
    fun `ascending board keeps the existing score when the new one is higher`() = runTest {
        stubExisting(score = 100L)

        val result = submitter.submit("u1", 150L, leaderboardConfig("board") { sortDirection = SortDirection.Ascending }, emptyMap())

        assertThat(result.isSuccess).isTrue()
        verify(exactly = 0) { transaction.set(any(), any()) }
    }

    @Test
    fun `metadata displayName and avatarId override the existing stored profile fields`() = runTest {
        stubExisting(score = 100L, displayName = "OldName", avatarId = "avatar_old")

        submitter.submit("u1", 150L, leaderboardConfig("board") {}, mapOf("displayName" to "NewName", "avatarId" to "avatar_new"))

        assertThat(written.captured["displayName"]).isEqualTo("NewName")
        assertThat(written.captured["avatarId"]).isEqualTo("avatar_new")
    }

    @Test
    fun `missing metadata falls back to the existing stored profile fields`() = runTest {
        stubExisting(score = 100L, displayName = "OldName", avatarId = "avatar_old")

        submitter.submit("u1", 150L, leaderboardConfig("board") {}, emptyMap())

        assertThat(written.captured["displayName"]).isEqualTo("OldName")
        assertThat(written.captured["avatarId"]).isEqualTo("avatar_old")
    }

    @Test
    fun `brand new entry with no metadata and no existing profile falls back to defaults`() = runTest {
        stubExisting(score = null, displayName = null, avatarId = null)

        submitter.submit("u1", 500L, leaderboardConfig("board") {}, emptyMap())

        assertThat(written.captured["displayName"]).isEqualTo("")
        assertThat(written.captured["avatarId"]).isEqualTo(AvatarDefaults.DEFAULT_AVATAR_ID)
    }

    @Test
    fun `displayName and avatarId are stripped out of the persisted metadata map`() = runTest {
        stubExisting(score = null)

        submitter.submit("u1", 500L, leaderboardConfig("board") {}, mapOf("displayName" to "Alice", "avatarId" to "avatar_01", "combo" to 3))

        @Suppress("UNCHECKED_CAST")
        val metadata = written.captured["metadata"] as Map<String, Any?>
        assertThat(metadata).containsExactly("combo", 3)
    }
}
