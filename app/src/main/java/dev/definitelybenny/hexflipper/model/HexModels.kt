package dev.definitelybenny.hexflipper.model

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Six directions for pointy-top hexagons.
 * Each direction has an axial coordinate offset (dq, dr).
 *
 * Pointy-top layout:
 *       UP
 *  UL  /  \  UR
 *     |    |
 *  DL  \  /  DR
 *      DOWN
 */
enum class HexDirection(val dq: Int, val dr: Int, val label: String) {
    UP(0, -1, "↑"),
    UP_RIGHT(1, -1, "↗"),
    DOWN_RIGHT(1, 0, "↘"),
    DOWN(0, 1, "↓"),
    DOWN_LEFT(-1, 1, "↙"),
    UP_LEFT(-1, 0, "↖");

    /**
     * Angle in radians for drawing the arrow, matching the actual screen-space
     * direction of movement for pointy-top hexes.
     *
     * Pointy-top hex neighbor screen directions:
     *   DOWN_RIGHT(1,0) → 0° (straight right)
     *   DOWN(0,1)       → 60° (down-right)
     *   DOWN_LEFT(-1,1) → 120° (down-left)
     *   UP_LEFT(-1,0)   → 180° (straight left)
     *   UP(0,-1)        → 240° / -120° (up-left)
     *   UP_RIGHT(1,-1)  → 300° / -60° (up-right)
     */
    val angleRadians: Float
        get() = when (this) {
            UP -> -2f * Math.PI.toFloat() / 3f          // -120° (up-left)
            UP_RIGHT -> -Math.PI.toFloat() / 3f         // -60° (up-right)
            DOWN_RIGHT -> 0f                             //  0° (straight right)
            DOWN -> Math.PI.toFloat() / 3f               //  60° (down-right)
            DOWN_LEFT -> 2f * Math.PI.toFloat() / 3f     //  120° (down-left)
            UP_LEFT -> Math.PI.toFloat()                 //  180° (straight left)
        }

    /** Fixed color for this direction — every stack moving the same way shares a color. */
    val color: PieceColor
        get() = when (this) {
            UP -> PieceColor.RED
            UP_RIGHT -> PieceColor.ORANGE
            DOWN_RIGHT -> PieceColor.YELLOW
            DOWN -> PieceColor.GREEN
            DOWN_LEFT -> PieceColor.BLUE
            UP_LEFT -> PieceColor.PURPLE
        }
}

/** Axial hex coordinate */
data class HexCell(val q: Int, val r: Int) {
    /** Cube coordinate s (derived) */
    val s: Int get() = -q - r

    fun neighbor(direction: HexDirection): HexCell =
        HexCell(q + direction.dq, r + direction.dr)

    /**
     * Convert axial coordinates to pixel position (pointy-top hexagon).
     * Returns (x, y) center of the hex in pixels.
     */
    fun toPixel(hexSize: Float): Pair<Float, Float> {
        val x = hexSize * (sqrt(3f) * q + sqrt(3f) / 2f * r)
        val y = hexSize * (3f / 2f * r)
        return Pair(x, y)
    }

    companion object {
        /**
         * Convert pixel position to nearest axial hex coordinate (pointy-top).
         */
        fun fromPixel(px: Float, py: Float, hexSize: Float): HexCell {
            val q = (sqrt(3f) / 3f * px - 1f / 3f * py) / hexSize
            val r = (2f / 3f * py) / hexSize
            return cubeRound(q, r)
        }

        private fun cubeRound(qf: Float, rf: Float): HexCell {
            val sf = -qf - rf
            var q = qf.roundToInt()
            var r = rf.roundToInt()
            val s = sf.roundToInt()

            val qDiff = abs(q - qf)
            val rDiff = abs(r - rf)
            val sDiff = abs(s - sf)

            if (qDiff > rDiff && qDiff > sDiff) {
                q = -r - s
            } else if (rDiff > sDiff) {
                r = -q - s
            }
            return HexCell(q, r)
        }
    }
}

/** Colors for hex pieces — each has a gradient start and end color */
enum class PieceColor(val start: Color, val end: Color) {
    RED(Color(0xFFFF6B6B), Color(0xFFEE5A24)),
    BLUE(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    GREEN(Color(0xFF43E97B), Color(0xFF38F9D7)),
    PURPLE(Color(0xFFF093FB), Color(0xFFF5576C)),
    ORANGE(Color(0xFFFA8231), Color(0xFFF7B731)),
    YELLOW(Color(0xFFFDD663), Color(0xFFFBD786));
}

/** Star rating based on moves vs par */
enum class StarRating(val stars: Int) {
    NONE(0), ONE(1), TWO(2), THREE(3);

    companion object {
        fun fromMoves(moves: Int, par: Int): StarRating = when {
            moves <= par -> THREE
            moves <= par + 2 -> TWO
            else -> ONE
        }
    }
}

/**
 * A stack of identical hex tiles at a board position.
 * All tiles in a stack share the same color and direction.
 */
data class HexStack(
    val cell: HexCell,
    val color: PieceColor,
    val direction: HexDirection,
    val height: Int  // number of tiles (1-4)
)

/**
 * Cell types on the board.
 */
enum class CellType {
    EMPTY,  // gray passable cell, no stack
    WALL    // black cell, blocks movement, can't be removed
}

/**
 * Board state for the hex sliding puzzle.
 *
 * The board consists of typed cells (empty or wall) and colored stacks.
 * Tapping a stack slides the entire stack off the board in its direction,
 * provided the path is clear. The puzzle is complete when all stacks are gone.
 */
data class BoardState(
    val cells: Map<HexCell, CellType>,  // all board cells (empty + wall)
    val stacks: Map<HexCell, HexStack>  // colored stacks on the board
) {
    /** All cells that are part of the board (cells + stacks). */
    val boardCells: Set<HexCell> get() = cells.keys + stacks.keys

    fun stackAt(cell: HexCell): HexStack? = stacks[cell]

    /**
     * Check if the stack at [cell] can slide off the board.
     *
     * A stack can move if its entire path in its direction is clear:
     * no other stacks AND no walls blocking it (regardless of height).
     * Empty cells are passable. The stack must eventually exit the board.
     */
    fun canMove(cell: HexCell): Boolean {
        val stack = stacks[cell] ?: return false
        var current = cell.neighbor(stack.direction)
        while (true) {
            if (current !in cells && current !in stacks) {
                // Off the board — the stack can exit
                return true
            }
            if (stacks.containsKey(current)) return false  // blocked by another stack
            if (cells[current] == CellType.WALL) return false  // blocked by wall
            // CellType.EMPTY — passable, continue
            current = current.neighbor(stack.direction)
        }
    }

    /**
     * Remove the entire stack from the board at [cell].
     * The cell becomes an empty passable cell.
     */
    fun removeStack(cell: HexCell): BoardState {
        return copy(
            cells = cells + (cell to CellType.EMPTY),
            stacks = stacks - cell
        )
    }

    /**
     * Get the path a stack takes when sliding off (for animation).
     * Returns list of cells from current position through empty cells
     * to one cell beyond the board edge for the exit animation.
     */
    fun slidePath(cell: HexCell): List<HexCell> {
        val stack = stacks[cell] ?: return emptyList()
        val path = mutableListOf(cell)
        var current = cell.neighbor(stack.direction)
        while (current in cells || current in stacks) {
            path.add(current)
            current = current.neighbor(stack.direction)
        }
        path.add(current)  // one beyond for exit animation
        return path
    }

    /** True when no stacks remain on the board (level complete). */
    val isComplete: Boolean get() = stacks.isEmpty()

    /** Total number of stacks remaining on the board. */
    val totalStacks: Int get() = stacks.size
}

/** A level definition with hex stacks and board cells */
data class Level(
    val number: Int,
    val cells: Map<HexCell, CellType>,  // empty + wall cells
    val stacks: Map<HexCell, HexStack>,  // colored stacks
    val par: Int
) {
    fun initialBoardState(): BoardState = BoardState(cells, stacks)
}

/** Difficulty tiers for Random Mode. */
enum class DifficultyTier(
    val label: String,
    val stackRange: IntRange,
    val unlockRequirement: Int
) {
    EASY("Easy", 3..5, 0),
    MEDIUM("Medium", 6..8, 5),
    HARD("Hard", 9..14, 5),
    EXPERT("Expert", 15..25, 5),
    MASTER("Master", 26..50, 5),
    INSANE("Insane", 51..100, 5);

    /** The tier that must be completed to unlock this one, or null if always unlocked. */
    val prerequisiteTier: DifficultyTier?
        get() = when (this) {
            EASY -> null
            MEDIUM -> EASY
            HARD -> MEDIUM
            EXPERT -> HARD
            MASTER -> EXPERT
            INSANE -> MASTER
        }
}

/** Stats for a single difficulty tier in Random Mode. */
data class TierStats(
    val solved: Int = 0,
    val bestMoves: Int? = null,
    val totalStars: Int = 0
)
