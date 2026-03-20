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
        val emptyPadding = when {
            numStacks <= 8 -> maxOf(4, numStacks)
            numStacks <= 25 -> numStacks / 2
            else -> maxOf(2, numStacks / 5)
        }
        val boardSize = numStacks + emptyPadding
        val directions = HexDirection.entries

        repeat(MAX_BOARD_ATTEMPTS) {
            val allCells = generateBoardShape(boardSize, random)
            val cellList = allCells.toList()
            if (cellList.size < numStacks + 1) return@repeat

            repeat(MAX_STACK_ATTEMPTS) {
                val shuffled = cellList.shuffled(random)
                val stackCells = shuffled.take(numStacks)
                val remaining = shuffled.drop(numStacks)

                val stacks = mutableMapOf<HexCell, HexStack>()
                for (cell in stackCells) {
                    val dir = directions[random.nextInt(directions.size)]
                    stacks[cell] = HexStack(cell = cell, color = dir.color, direction = dir, height = 1)
                }

                // For large puzzles, break cycles by fixing minimum stacks
                if (numStacks > 20) {
                    breakCyclesMinimal(stacks, allCells, random)
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

    /**
     * Async generation with progress reporting for large puzzles.
     */
    suspend fun generateAsync(
        tier: DifficultyTier,
        random: Random = Random,
        onProgress: (attempt: Int, maxAttempts: Int) -> Unit = { _, _ -> }
    ): Level {
        val numStacks = random.nextInt(tier.stackRange.first, tier.stackRange.last + 1)

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val level = generateWithStackCount(numStacks, random)
            onProgress(1, 1)
            level
        }
    }

    /**
     * Break dependency cycles by re-pointing the minimum number of stacks.
     *
     * Iteratively finds cycles, picks one random stack from each cycle,
     * and re-points it to a direction that exits the board (shortest path).
     * Repeats until no cycles remain. This preserves most random directions
     * while guaranteeing solvability.
     */
    private fun breakCyclesMinimal(
        stacks: MutableMap<HexCell, HexStack>,
        boardCells: Set<HexCell>,
        random: Random
    ) {
        val directions = HexDirection.entries
        var maxIterations = 20 // safety limit

        while (maxIterations-- > 0) {
            val cycleCells = findCycleCells(stacks, boardCells)
            if (cycleCells.isEmpty()) return // no cycles, done

            // Find strongly connected components (individual cycles) via simple grouping:
            // walk the dependency chain from each cycle cell to find its cycle group
            val visited = mutableSetOf<HexCell>()
            for (cell in cycleCells) {
                if (cell in visited) continue

                // Find this cycle group by following blocking chains
                val group = mutableSetOf<HexCell>()
                val queue = ArrayDeque<HexCell>()
                queue.addLast(cell)
                while (queue.isNotEmpty()) {
                    val c = queue.removeFirst()
                    if (c !in cycleCells || !group.add(c)) continue
                    // Find what c blocks and what blocks c
                    val stack = stacks[c]!!
                    var current = c.neighbor(stack.direction)
                    while (current in boardCells) {
                        if (current in cycleCells) queue.addLast(current)
                        current = current.neighbor(stack.direction)
                    }
                }
                visited.addAll(group)

                // Pick one random stack from this group to fix
                val toFix = group.random(random)
                val shuffledDirs = directions.shuffled(random)
                val dir = shuffledDirs.minByOrNull { d -> stepsToExit(toFix, d, boardCells) }
                    ?: shuffledDirs.first()
                stacks[toFix] = HexStack(cell = toFix, color = dir.color, direction = dir, height = 1)
            }
        }
    }

    /**
     * Find all stack cells that participate in dependency cycles.
     */
    private fun findCycleCells(
        stacks: Map<HexCell, HexStack>,
        boardCells: Set<HexCell>
    ): Set<HexCell> {
        val stackCells = stacks.keys.toList()
        val n = stackCells.size
        val cellToIndex = HashMap<HexCell, Int>(n * 2)
        for (i in stackCells.indices) cellToIndex[stackCells[i]] = i

        val blockedByCount = IntArray(n)
        val blocks = Array(n) { mutableListOf<Int>() }

        for (i in stackCells.indices) {
            val cell = stackCells[i]
            val stack = stacks[cell]!!
            var current = cell.neighbor(stack.direction)
            while (current in boardCells) {
                val blockerIdx = cellToIndex[current]
                if (blockerIdx != null) {
                    blockedByCount[i]++
                    blocks[blockerIdx].add(i)
                }
                current = current.neighbor(stack.direction)
            }
        }

        val queue = ArrayDeque<Int>()
        for (i in 0 until n) {
            if (blockedByCount[i] == 0) queue.addLast(i)
        }

        val processed = BooleanArray(n)
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            processed[idx] = true
            for (unblocked in blocks[idx]) {
                blockedByCount[unblocked]--
                if (blockedByCount[unblocked] == 0) queue.addLast(unblocked)
            }
        }

        return stackCells.indices.filter { !processed[it] }.map { stackCells[it] }.toSet()
    }

    /**
     * Count how many steps it takes to exit the board from [cell] in [direction].
     * Fewer steps = closer to edge in that direction.
     */
    private fun stepsToExit(cell: HexCell, direction: HexDirection, boardCells: Set<HexCell>): Int {
        var current = cell.neighbor(direction)
        var steps = 1
        while (current in boardCells) {
            current = current.neighbor(direction)
            steps++
        }
        return steps
    }

    /**
     * Hex distance from origin (0,0) using cube coordinates.
     */
    private fun hexDistance(cell: HexCell): Int {
        val s = -cell.q - cell.r
        return maxOf(kotlin.math.abs(cell.q), kotlin.math.abs(cell.r), kotlin.math.abs(s))
    }

    /**
     * Generate a board shape biased toward the center.
     *
     * Uses a sorted frontier approach: all frontier cells are sorted by distance,
     * then a weighted random pick favors closer cells. The frontier is maintained
     * efficiently with a set to avoid duplicate processing.
     */
    private fun generateBoardShape(size: Int, random: Random): Set<HexCell> {
        val cells = mutableSetOf(HexCell(0, 0))
        val inFrontier = mutableSetOf<HexCell>()
        val frontier = mutableListOf<HexCell>()

        for (dir in HexDirection.entries) {
            val nb = HexCell(0, 0).neighbor(dir)
            if (inFrontier.add(nb)) frontier.add(nb)
        }

        while (cells.size < size && frontier.isNotEmpty()) {
            // For small frontiers, use weighted selection. For large ones, just pick
            // from the closest candidates to avoid O(n) weight computation.
            val idx = if (frontier.size <= 50) {
                weightedPick(frontier, random)
            } else {
                // Sort by distance, pick randomly from the closest third
                frontier.sortBy { hexDistance(it) }
                val pickRange = maxOf(1, frontier.size / 3)
                random.nextInt(pickRange)
            }

            val cell = frontier[idx]
            frontier[idx] = frontier.last()
            frontier.removeAt(frontier.lastIndex)
            inFrontier.remove(cell)

            if (cell in cells) continue
            cells.add(cell)

            for (dir in HexDirection.entries) {
                val nb = cell.neighbor(dir)
                if (nb !in cells && inFrontier.add(nb)) {
                    frontier.add(nb)
                }
            }
        }
        return cells
    }

    private fun weightedPick(frontier: List<HexCell>, random: Random): Int {
        val weights = DoubleArray(frontier.size) { i ->
            val d = hexDistance(frontier[i])
            1.0 / ((1 + d).toDouble() * (1 + d).toDouble())
        }
        var totalWeight = 0.0
        for (w in weights) totalWeight += w

        var roll = random.nextDouble() * totalWeight
        for (i in weights.indices) {
            roll -= weights[i]
            if (roll <= 0.0) return i
        }
        return weights.lastIndex
    }

    /**
     * Check solvability using dependency graph cycle detection.
     *
     * For each stack, walk its exit path to find which other stacks block it.
     * This builds a directed graph: edge A → B means "A blocks B's exit".
     * If the graph is a DAG (no cycles), a valid removal order exists and
     * par = number of stacks. If there's a cycle, the puzzle is unsolvable.
     *
     * O(n²) worst case — fast even for 100 stacks.
     */
    internal fun solve(initial: BoardState): Int? {
        if (initial.isComplete) return 0

        val stackCells = initial.stacks.keys.toList()
        val n = stackCells.size
        if (n == 0) return 0

        // Build adjacency: for each stack, which stacks block its exit?
        // Also track reverse edges for efficient in-degree updates.
        val blockedByCount = IntArray(n)
        val cellToIndex = HashMap<HexCell, Int>(n * 2)
        for (i in stackCells.indices) {
            cellToIndex[stackCells[i]] = i
        }

        // blocks[i] = list of indices that stack i blocks (reverse edges)
        val blocks = Array(n) { mutableListOf<Int>() }

        for (i in stackCells.indices) {
            val cell = stackCells[i]
            val stack = initial.stacks[cell]!!
            var current = cell.neighbor(stack.direction)
            while (current in initial.cells || current in initial.stacks) {
                val blockerIdx = cellToIndex[current]
                if (blockerIdx != null) {
                    // 'current' blocks 'cell' — cell can't move until current is removed
                    blockedByCount[i]++
                    blocks[blockerIdx].add(i)
                }
                current = current.neighbor(stack.direction)
            }
        }

        // Kahn's algorithm — topological sort
        val queue = ArrayDeque<Int>()
        for (i in 0 until n) {
            if (blockedByCount[i] == 0) queue.addLast(i)
        }

        var processed = 0
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            processed++
            for (unblocked in blocks[idx]) {
                blockedByCount[unblocked]--
                if (blockedByCount[unblocked] == 0) queue.addLast(unblocked)
            }
        }

        return if (processed == n) n else null
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
