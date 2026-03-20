package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.CellType
import dev.definitelybenny.hexflipper.model.DifficultyTier
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LevelGeneratorTest {

    @Test
    fun `EASY generates 3-5 stacks`() {
        repeat(10) {
            val level = LevelGenerator.generate(DifficultyTier.EASY, Random(it))
            assertTrue(
                "EASY should have 3-5 stacks, got ${level.stacks.size}",
                level.stacks.size in 3..5
            )
        }
    }

    @Test
    fun `MEDIUM generates 6-8 stacks`() {
        repeat(10) {
            val level = LevelGenerator.generate(DifficultyTier.MEDIUM, Random(it + 50))
            assertTrue(
                "MEDIUM should have 6-8 stacks, got ${level.stacks.size}",
                level.stacks.size in 6..8
            )
        }
    }

    @Test
    fun `HARD generates 9-12 stacks`() {
        repeat(10) {
            val level = LevelGenerator.generate(DifficultyTier.HARD, Random(it + 100))
            assertTrue(
                "HARD should have 9-12 stacks, got ${level.stacks.size}",
                level.stacks.size in 9..12
            )
        }
    }

    @Test
    fun `all stacks have height 1`() {
        for (tier in listOf(DifficultyTier.EASY, DifficultyTier.MEDIUM, DifficultyTier.HARD)) {
            val level = LevelGenerator.generate(tier, Random(42))
            for ((_, stack) in level.stacks) {
                assertEquals("All stacks should have height 1", 1, stack.height)
            }
        }
    }

    @Test
    fun `no walls are generated`() {
        for (tier in listOf(DifficultyTier.EASY, DifficultyTier.MEDIUM, DifficultyTier.HARD)) {
            val level = LevelGenerator.generate(tier, Random(42))
            for ((_, cellType) in level.cells) {
                assertEquals("No walls should be generated", CellType.EMPTY, cellType)
            }
        }
    }

    @Test
    fun `board has enough cells for stacks plus movement room`() {
        for (tier in listOf(DifficultyTier.EASY, DifficultyTier.MEDIUM, DifficultyTier.HARD)) {
            val level = LevelGenerator.generate(tier, Random(42))
            val totalCells = level.cells.size + level.stacks.size
            assertTrue(
                "Board should have at least stacks + 5 cells",
                totalCells >= level.stacks.size + 5
            )
        }
    }

    @Test
    fun `generated level has positive par`() {
        val level = LevelGenerator.generate(DifficultyTier.EASY, Random(42))
        assertTrue("Par should be positive", level.par > 0)
    }

    @Test
    fun `seeded random produces deterministic results`() {
        val level1 = LevelGenerator.generate(DifficultyTier.MEDIUM, Random(99))
        val level2 = LevelGenerator.generate(DifficultyTier.MEDIUM, Random(99))
        assertEquals(level1.stacks.size, level2.stacks.size)
        assertEquals(level1.cells.size, level2.cells.size)
        assertEquals(level1.par, level2.par)
    }

    @Test
    fun `generated level is solvable`() {
        repeat(5) {
            val level = LevelGenerator.generate(DifficultyTier.EASY, Random(it))
            val par = LevelGenerator.solve(level.initialBoardState())
            assertNotNull("Generated level should be solvable", par)
        }
    }

    @Test
    fun `level number is always 0 for generated levels`() {
        val level = LevelGenerator.generate(DifficultyTier.EASY, Random(42))
        assertEquals(0, level.number)
    }
}
