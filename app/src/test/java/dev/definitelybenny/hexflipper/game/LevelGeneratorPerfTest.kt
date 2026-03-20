package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.DifficultyTier
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class LevelGeneratorPerfTest {

    private val iterations = 10_000
    private val heavyIterations = 100  // for Master/Insane with large stack counts

    @Test
    fun `EASY generation 10k iterations`() {
        val elapsed = measureTimeMillis {
            repeat(iterations) {
                LevelGenerator.generate(DifficultyTier.EASY, Random(it))
            }
        }
        val avg = elapsed.toDouble() / iterations
        println("EASY: $iterations levels in ${elapsed}ms (%.3fms avg)".format(avg))
    }

    @Test
    fun `MEDIUM generation 10k iterations`() {
        val elapsed = measureTimeMillis {
            repeat(iterations) {
                LevelGenerator.generate(DifficultyTier.MEDIUM, Random(it))
            }
        }
        val avg = elapsed.toDouble() / iterations
        println("MEDIUM: $iterations levels in ${elapsed}ms (%.3fms avg)".format(avg))
    }

    @Test
    fun `HARD generation 10k iterations`() {
        val elapsed = measureTimeMillis {
            repeat(iterations) {
                LevelGenerator.generate(DifficultyTier.HARD, Random(it))
            }
        }
        val avg = elapsed.toDouble() / iterations
        println("HARD: $iterations levels in ${elapsed}ms (%.3fms avg)".format(avg))
    }

    @Test
    fun `EXPERT generation 10k iterations`() {
        val elapsed = measureTimeMillis {
            repeat(iterations) {
                LevelGenerator.generate(DifficultyTier.EXPERT, Random(it))
            }
        }
        val avg = elapsed.toDouble() / iterations
        println("EXPERT: $iterations levels in ${elapsed}ms (%.3fms avg)".format(avg))
    }

    @Test
    fun `MASTER generation 100 iterations`() {
        val elapsed = measureTimeMillis {
            repeat(heavyIterations) {
                LevelGenerator.generate(DifficultyTier.MASTER, Random(it))
            }
        }
        val avg = elapsed.toDouble() / heavyIterations
        println("MASTER: $heavyIterations levels in ${elapsed}ms (%.3fms avg)".format(avg))
    }

    @Test
    fun `INSANE generation 100 iterations`() {
        val elapsed = measureTimeMillis {
            repeat(heavyIterations) {
                LevelGenerator.generate(DifficultyTier.INSANE, Random(it))
            }
        }
        val avg = elapsed.toDouble() / heavyIterations
        println("INSANE: $heavyIterations levels in ${elapsed}ms (%.3fms avg)".format(avg))
    }

    @Test
    fun `BFS solver 10k iterations per tier`() {
        for (tier in DifficultyTier.entries) {
            val level = LevelGenerator.generate(tier, Random(42))
            val board = level.initialBoardState()
            val elapsed = measureTimeMillis {
                repeat(iterations) {
                    LevelGenerator.solve(board)
                }
            }
            val avg = elapsed.toDouble() / iterations
            println("${tier.name} solve (${level.stacks.size} stacks): $iterations runs in ${elapsed}ms (%.3fms avg)".format(avg))
        }
    }
}
