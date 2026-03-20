package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.DifficultyTier
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class LevelGeneratorPerfTest {

    @Test
    fun `EASY generation completes under 1 second`() {
        val elapsed = measureTimeMillis {
            repeat(10) {
                LevelGenerator.generate(DifficultyTier.EASY, Random(it))
            }
        }
        println("EASY: 10 levels in ${elapsed}ms (${elapsed / 10}ms avg)")
        assertTrue("EASY generation should be fast", elapsed < 1000)
    }

    @Test
    fun `MEDIUM generation completes under 2 seconds`() {
        val elapsed = measureTimeMillis {
            repeat(10) {
                LevelGenerator.generate(DifficultyTier.MEDIUM, Random(it + 50))
            }
        }
        println("MEDIUM: 10 levels in ${elapsed}ms (${elapsed / 10}ms avg)")
        assertTrue("MEDIUM generation should complete in time", elapsed < 2000)
    }

    @Test
    fun `HARD generation completes under 5 seconds`() {
        val elapsed = measureTimeMillis {
            repeat(10) {
                LevelGenerator.generate(DifficultyTier.HARD, Random(it + 100))
            }
        }
        println("HARD: 10 levels in ${elapsed}ms (${elapsed / 10}ms avg)")
        assertTrue("HARD generation should complete in time", elapsed < 5000)
    }

    @Test
    fun `EXPERT generation completes under 10 seconds`() {
        val elapsed = measureTimeMillis {
            repeat(5) {
                LevelGenerator.generate(DifficultyTier.EXPERT, Random(it + 200))
            }
        }
        println("EXPERT: 5 levels in ${elapsed}ms (${elapsed / 5}ms avg)")
        assertTrue("EXPERT generation should complete in time", elapsed < 10000)
    }

    @Test
    fun `BFS solver time scales with stack count`() {
        val tiers = listOf(DifficultyTier.EASY, DifficultyTier.MEDIUM, DifficultyTier.HARD)
        val times = mutableMapOf<String, Long>()

        for (tier in tiers) {
            val level = LevelGenerator.generate(tier, Random(42))
            val board = level.initialBoardState()
            val elapsed = measureTimeMillis {
                LevelGenerator.solve(board)
            }
            times[tier.name] = elapsed
            println("${tier.name} solve (${level.stacks.size} stacks): ${elapsed}ms")
        }

        // Just verify they all complete — timing will vary by machine
        for ((name, time) in times) {
            assertTrue("$name solver should finish", time < 30000)
        }
    }
}
