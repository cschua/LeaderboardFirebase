package com.leaderboardkit.data.common

import com.google.common.truth.Truth.assertThat
import com.leaderboardkit.domain.model.LeaderboardEntry
import com.leaderboardkit.domain.model.SortDirection
import org.junit.Test

class RankWindowTest {

    private fun entry(userId: String, score: Long) =
        LeaderboardEntry(userId, displayName = userId, avatarId = "avatar_01", score = score, rank = null)

    // -- assignRanks ------------------------------------------------------

    @Test
    fun `assignRanks stamps consecutive ranks starting at startRank`() {
        val entries = listOf(entry("a", 100), entry("b", 90), entry("c", 80))

        val ranked = assignRanks(entries, startRank = 5)

        assertThat(ranked.map { it.rank }).containsExactly(5, 6, 7).inOrder()
        assertThat(ranked.map { it.userId }).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun `assignRanks on an empty list returns an empty list`() {
        assertThat(assignRanks(emptyList(), startRank = 1)).isEmpty()
    }

    // -- rankFromAscendingIndex --------------------------------------------

    @Test
    fun `ascending boards rank best-first from the start of the stored order`() {
        assertThat(rankFromAscendingIndex(index = 0, size = 10, SortDirection.Ascending)).isEqualTo(1)
        assertThat(rankFromAscendingIndex(index = 4, size = 10, SortDirection.Ascending)).isEqualTo(5)
        assertThat(rankFromAscendingIndex(index = 9, size = 10, SortDirection.Ascending)).isEqualTo(10)
    }

    @Test
    fun `descending boards rank best-first from the end of the stored order`() {
        assertThat(rankFromAscendingIndex(index = 9, size = 10, SortDirection.Descending)).isEqualTo(1)
        assertThat(rankFromAscendingIndex(index = 0, size = 10, SortDirection.Descending)).isEqualTo(10)
        assertThat(rankFromAscendingIndex(index = 4, size = 10, SortDirection.Descending)).isEqualTo(6)
    }

    // -- surroundingWindow --------------------------------------------------

    private val tenAscending = (1..10).map { "s$it" } // s1 = lowest score ... s10 = highest score

    @Test
    fun `ascending board window reads forward with no reversal`() {
        val window = surroundingWindow(tenAscending, anchorIndex = 4, radius = 2, SortDirection.Ascending)

        assertThat(window.items).containsExactly("s3", "s4", "s5", "s6", "s7").inOrder()
        assertThat(window.startRank).isEqualTo(3)
    }

    @Test
    fun `descending board window reverses so the highest score displays first`() {
        val window = surroundingWindow(tenAscending, anchorIndex = 4, radius = 2, SortDirection.Descending)

        // s5 is the 5th-lowest of 10 scores, i.e. rank 6 under "highest score wins".
        // A radius of 2 around rank 6 covers ranks 4..8: s7, s6, s5, s4, s3.
        assertThat(window.items).containsExactly("s7", "s6", "s5", "s4", "s3").inOrder()
        assertThat(window.startRank).isEqualTo(4)
    }

    @Test
    fun `window near the start of an ascending board clamps instead of going negative`() {
        val window = surroundingWindow(tenAscending, anchorIndex = 1, radius = 3, SortDirection.Ascending)

        assertThat(window.items).containsExactly("s1", "s2", "s3", "s4", "s5").inOrder()
        assertThat(window.startRank).isEqualTo(1)
    }

    @Test
    fun `window near the top of a descending board clamps instead of overrunning the list`() {
        val window = surroundingWindow(tenAscending, anchorIndex = 8, radius = 3, SortDirection.Descending)

        assertThat(window.items).containsExactly("s10", "s9", "s8", "s7", "s6").inOrder()
        assertThat(window.startRank).isEqualTo(1)
    }

    @Test
    fun `zero radius returns only the anchor itself`() {
        val window = surroundingWindow(tenAscending, anchorIndex = 4, radius = 0, SortDirection.Ascending)

        assertThat(window.items).containsExactly("s5")
        assertThat(window.startRank).isEqualTo(5)
    }

    @Test
    fun `single-element list never indexes out of bounds`() {
        val window = surroundingWindow(listOf("only"), anchorIndex = 0, radius = 5, SortDirection.Descending)

        assertThat(window.items).containsExactly("only")
        assertThat(window.startRank).isEqualTo(1)
    }

    // -- aboveWindowStartRank -------------------------------------------------

    @Test
    fun `aboveWindowStartRank is the anchor's rank minus how many entries beat it`() {
        assertThat(aboveWindowStartRank(anchorRank = 10, betterCount = 3)).isEqualTo(7)
    }

    @Test
    fun `aboveWindowStartRank never drops below rank 1`() {
        assertThat(aboveWindowStartRank(anchorRank = 2, betterCount = 5)).isEqualTo(1)
    }
}
