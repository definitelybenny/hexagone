package dev.definitelybenny.hexflipper.model

import org.junit.Assert.*
import org.junit.Test

class DifficultyTierTest {

    @Test
    fun `EASY has no prerequisite`() {
        assertNull(DifficultyTier.EASY.prerequisiteTier)
    }

    @Test
    fun `each tier except EASY has a prerequisite`() {
        for (tier in DifficultyTier.entries) {
            if (tier == DifficultyTier.EASY) continue
            assertNotNull("${tier.name} should have a prerequisite", tier.prerequisiteTier)
        }
    }

    @Test
    fun `prerequisite chain is correct`() {
        assertEquals(DifficultyTier.EASY, DifficultyTier.MEDIUM.prerequisiteTier)
        assertEquals(DifficultyTier.MEDIUM, DifficultyTier.HARD.prerequisiteTier)
        assertEquals(DifficultyTier.HARD, DifficultyTier.EXPERT.prerequisiteTier)
        assertEquals(DifficultyTier.EXPERT, DifficultyTier.MASTER.prerequisiteTier)
        assertEquals(DifficultyTier.MASTER, DifficultyTier.INSANE.prerequisiteTier)
    }

    @Test
    fun `stack ranges do not overlap`() {
        val allValues = DifficultyTier.entries.flatMap { it.stackRange.toList() }
        assertEquals("no duplicates across tiers", allValues.size, allValues.toSet().size)
    }

    @Test
    fun `stack ranges cover 3 to 100`() {
        val allValues = DifficultyTier.entries.flatMap { it.stackRange.toList() }
        assertEquals(3, allValues.min())
        assertEquals(100, allValues.max())
    }

    @Test
    fun `EASY unlock requirement is 0`() {
        assertEquals(0, DifficultyTier.EASY.unlockRequirement)
    }

    @Test
    fun `all other tiers require 5 solves`() {
        for (tier in DifficultyTier.entries) {
            if (tier == DifficultyTier.EASY) continue
            assertEquals("${tier.name} should require 5", 5, tier.unlockRequirement)
        }
    }

    @Test
    fun `labels are non-empty`() {
        for (tier in DifficultyTier.entries) {
            assertTrue("${tier.name} label should not be empty", tier.label.isNotEmpty())
        }
    }

    @Test
    fun `stack ranges are contiguous across tiers`() {
        val sorted = DifficultyTier.entries.sortedBy { it.stackRange.first }
        for (i in 0 until sorted.size - 1) {
            assertEquals(
                "${sorted[i].name} end + 1 should equal ${sorted[i + 1].name} start",
                sorted[i].stackRange.last + 1,
                sorted[i + 1].stackRange.first
            )
        }
    }
}
