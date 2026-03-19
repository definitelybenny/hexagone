package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.BoardState
import dev.definitelybenny.hexflipper.model.CellType
import dev.definitelybenny.hexflipper.model.DifficultyTier
import dev.definitelybenny.hexflipper.model.HexCell
import dev.definitelybenny.hexflipper.model.HexDirection
import dev.definitelybenny.hexflipper.model.HexStack
import dev.definitelybenny.hexflipper.model.Level
import kotlin.random.Random

/**
 * Procedural level generator for random / endless mode.
 * Generates solvable hex puzzles based on difficulty tier.
 */
object LevelGenerator {

    private const val MAX_BOARD_ATTEMPTS = 50
    private const val MAX_STACK_ATTEMPTS = 200

    /**
     * Generate a solvable level for the given difficulty tier.
     * Stack count is randomly chosen within the tier's range.
     * Board size auto-scales to fit the stacks with movement room.
     */
    fun generate(tier: DifficultyTier, random: Random = Random): Level {
        val numStacks = random.nextInt(tier.stackRange.first, tier.stackRange.last + 1)
        return generateWithStackCount(numStacks, random)
    }

    private fun generateWithStackCount(numStacks: Int, random: Random): Level {
        val boardSize = numStacks + maxOf(6, numStacks * 2 / 3)
        val directions = HexDirection.entries

        repeat(MAX_BOARD_ATTEMPTS) {
            val allCells = generateBoardShape(boardSize, random)
            val cellList = allCells.toList()
            if (cellList.size < numStacks + 2) return@repeat

            repeat(MAX_STACK_ATTEMPTS) {
                val shuffled = cellList.shuffled(random)
                val stackCells = shuffled.take(numStacks)
                val remaining = shuffled.drop(numStacks)

                val stacks = mutableMapOf<HexCell, HexStack>()
                for (cell in stackCells) {
                    val dir = directions[random.nextInt(directions.size)]
                    stacks[cell] = HexStack(
                        cell = cell,
                        color = dir.color,
                        direction = dir,
                        height = 1
                    )
                }

                val cells = mutableMapOf<HexCell, CellType>()
                for (cell in remaining) {
                    cells[cell] = CellType.EMPTY
                }

                val state = BoardState(cells, stacks)
                val par = solve(state)

                if (par != null && par > 1) {
                    return Level(number = 0, cells = cells, stacks = stacks, par = par)
                }
            }
        }

        return createFallbackLevel()
    }

    private fun generateBoardShape(size: Int, random: Random): Set<HexCell> {
        val cells = mutableSetOf(HexCell(0, 0))
        val frontier = mutableListOf<HexCell>()
        for (dir in HexDirection.entries) {
            frontier.add(HexCell(0, 0).neighbor(dir))
        }
        while (cells.size < size && frontier.isNotEmpty()) {
            val idx = random.nextInt(frontier.size)
            val cell = frontier[idx]
            frontier[idx] = frontier.last()
            frontier.removeAt(frontier.lastIndex)
            if (cell in cells) continue
            cells.add(cell)
            for (dir in HexDirection.entries) {
                val nb = cell.neighbor(dir)
                if (nb !in cells) frontier.add(nb)
            }
        }
        return cells
    }

    /**
     * BFS solver. Each move removes one stack. Returns min moves or null.
     */
    internal fun solve(initial: BoardState): Int? {
        if (initial.isComplete) return 0

        fun stateKey(state: BoardState): List<Long> =
            state.stacks.keys.map { cell ->
                ((cell.q.toLong() and 0xFFFF) shl 16) or (cell.r.toLong() and 0xFFFF)
            }.sorted()

        val visited = HashSet<List<Long>>(512)
        visited.add(stateKey(initial))

        var frontier = listOf(initial)
        var depth = 0

        while (frontier.isNotEmpty()) {
            val nextFrontier = mutableListOf<BoardState>()

            for (state in frontier) {
                for (cell in state.stacks.keys) {
                    if (!state.canMove(cell)) continue
                    val next = state.removeStack(cell)
                    if (next.isComplete) return depth + 1
                    val key = stateKey(next)
                    if (visited.add(key)) {
                        nextFrontier.add(next)
                    }
                }
            }

            depth++
            frontier = nextFrontier
            if (visited.size > 500_000) return null
        }

        return null
    }

    private fun createFallbackLevel(): Level {
        val c0 = HexCell(0, 0)
        val c1 = HexCell(1, 0)
        val c2 = HexCell(2, 0)
        return Level(
            number = 0,
            cells = mapOf(c2 to CellType.EMPTY),
            stacks = mapOf(
                c0 to HexStack(c0, HexDirection.DOWN_RIGHT.color, HexDirection.DOWN_RIGHT, 1),
                c1 to HexStack(c1, HexDirection.DOWN_RIGHT.color, HexDirection.DOWN_RIGHT, 1)
            ),
            par = 2
        )
    }
}
