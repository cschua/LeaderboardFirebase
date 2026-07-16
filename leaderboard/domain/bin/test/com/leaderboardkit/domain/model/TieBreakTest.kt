package com.leaderboardkit.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TieBreakTest {

    @Test
    fun `EarliestAchievedFirst preserves metadataKey and has a default`() {
        val tb = TieBreak.EarliestAchievedFirst()
        assertThat(tb.metadataKey).isEqualTo("achievedAt")

        val custom = TieBreak.EarliestAchievedFirst("timestamp")
        assertThat(custom.metadataKey).isEqualTo("timestamp")
    }

    @Test
    fun `LatestAchievedFirst preserves metadataKey and has a default`() {
        val tb = TieBreak.LatestAchievedFirst()
        assertThat(tb.metadataKey).isEqualTo("achievedAt")

        val custom = TieBreak.LatestAchievedFirst("timestamp")
        assertThat(custom.metadataKey).isEqualTo("timestamp")
    }

    @Test
    fun `None is a data object`() {
        assertThat(TieBreak.None).isNotNull()
    }
}
