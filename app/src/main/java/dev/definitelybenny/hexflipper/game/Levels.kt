package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.*
import dev.definitelybenny.hexflipper.model.CellType.*
import dev.definitelybenny.hexflipper.model.HexDirection.*
// PieceColor is now derived from direction via HexDirection.color

/**
 * Hand-crafted campaign levels for Hex Flipper.
 *
 * Each stack is uniform (same color and direction). Tapping removes the entire stack.
 * A stack can slide if no other stack or wall blocks its path.
 * Empty cells are passable. Par = number of stacks.
 */
object CampaignLevels {

    private fun c(q: Int, r: Int) = HexCell(q, r)

    /** Color is derived from direction — all stacks moving the same way share a color. */
    private fun stack(q: Int, r: Int, dir: HexDirection, height: Int = 1): Pair<HexCell, HexStack> {
        val cell = c(q, r)
        return cell to HexStack(cell, dir.color, dir, height)
    }

    private fun empty(q: Int, r: Int) = c(q, r) to EMPTY
    private fun wall(q: Int, r: Int) = c(q, r) to WALL

    private fun level(
        number: Int,
        cells: Map<HexCell, CellType>,
        stacks: Map<HexCell, HexStack>,
        par: Int = stacks.size
    ) = Level(number, cells, stacks, par)

    private fun level(
        number: Int,
        emptyCells: List<Pair<HexCell, CellType>>,
        stackList: List<Pair<HexCell, HexStack>>,
        par: Int = stackList.size
    ) = Level(number, emptyCells.toMap(), stackList.toMap(), par)

    // ── Campaign ────────────────────────────────────────────────────────

    val levels: List<Level> = listOf(

        // ═══ TUTORIAL (1-5) ═══

        // Level 1 — Two stacks, both point right. (1,0) blocks (0,0).
        // Solution: (1,0) then (0,0)
        level(1,
            listOf(empty(2, 0)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT)
            )
        ),

        // Level 2 — Three stacks in a line.
        // Solution: (2,0) → (1,0) → (0,0)
        level(2,
            listOf(empty(3, 0), empty(-1, 0)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT),
                stack(2, 0, DOWN_RIGHT)
            )
        ),

        // Level 3 — Introduces UP direction.
        // (0,1) goes UP, blocked by (0,0). (0,0) goes DOWN_RIGHT, clear.
        // Solution: (0,0) → (0,1)
        level(3,
            listOf(empty(1, 0), empty(0, -1)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(0, 1, UP)
            )
        ),

        // Level 4 — Four stacks, two directions.
        // Solution: (1,-1) → (1,0) → (0,0) → (0,1)
        level(4,
            listOf(empty(2, 0), empty(2, -1), empty(-1, 0), empty(-1, 1), empty(0, -1), empty(1, 1)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT),
                stack(0, 1, UP),
                stack(1, -1, DOWN_RIGHT)
            )
        ),

        // Level 5 — Five stacks, mixed directions.
        // Solution: (2,0) → (1,0) → (0,0) → (-1,1) → (0,1)
        level(5,
            listOf(empty(3, 0), empty(-1, 0), empty(-2, 1), empty(-2, 2), empty(0, -1), empty(1, 1)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT),
                stack(2, 0, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT),
                stack(0, 1, UP)
            )
        ),

        // ═══ EASY (6-10) ═══

        // Level 6 — Introduces height (tall stack = 2)
        level(6,
            listOf(empty(2, 0), empty(-1, 0), empty(0, -1), empty(1, 1), empty(-1, 1)),
            listOf(
                stack(0, 0, DOWN_RIGHT, 2),
                stack(1, 0, DOWN_RIGHT),
                stack(0, 1, UP),
                stack(1, -1, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT)
            )
        ),

        // Level 7 — Wider board
        level(7,
            listOf(empty(3, 0), empty(-1, 0), empty(0, -1), empty(2, -1), empty(-1, 1), empty(1, 1), empty(2, 1)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT),
                stack(2, 0, DOWN_RIGHT),
                stack(0, 1, DOWN_RIGHT),
                stack(1, -1, DOWN),
                stack(-1, 1, DOWN_LEFT)
            )
        ),

        // Level 8 — Cross pattern
        level(8,
            listOf(empty(2, 0), empty(-2, 0), empty(0, 2), empty(0, -2),
                empty(1, 1), empty(-1, -1), empty(1, -1), empty(-1, 1)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT),
                stack(0, 1, DOWN),
                stack(0, -1, UP),
                stack(1, -1, UP_RIGHT),
                stack(-1, 1, DOWN_LEFT)
            )
        ),

        // Level 9 — L-shape with blocking
        level(9,
            listOf(empty(3, 0), empty(4, 0), empty(-1, 0), empty(0, -1),
                empty(0, 2), empty(0, 3), empty(1, 1), empty(1, 2)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT),
                stack(2, 0, DOWN_RIGHT),
                stack(0, 1, DOWN, 2),
                stack(-1, 1, DOWN_LEFT),
                stack(1, -1, UP_RIGHT)
            )
        ),

        // Level 10 — First walls! Wall blocks path.
        level(10,
            listOf(empty(2, 0), empty(-1, 0), empty(0, -1), empty(1, 1),
                empty(-1, 1), empty(0, 2), empty(1, -1), wall(3, 0)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(0, 1, UP),
                stack(-1, 0, UP_LEFT),
                stack(2, 0, DOWN_RIGHT),  // blocked by wall at (3,0) — must go UP instead
                stack(0, -1, UP)
            )
        ),

        // ═══ MEDIUM (11-15) ═══

        // Level 11
        level(11,
            listOf(empty(3, 0), empty(-2, 0), empty(0, -1), empty(2, -1),
                empty(-1, -1), empty(-1, 2), empty(1, 1), empty(2, 1),
                empty(-2, 1), empty(0, 2)),
            listOf(
                stack(0, 0, DOWN_RIGHT, 2),
                stack(1, 0, DOWN_RIGHT),
                stack(2, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT),
                stack(0, 1, DOWN),
                stack(-1, 1, DOWN_LEFT),
                stack(1, -1, UP_RIGHT),
                stack(-1, 0, UP_LEFT)
            )
        ),

        // Level 12 — Diamond with walls
        level(12,
            listOf(empty(2, 0), empty(-2, 0), empty(0, 2), empty(0, -2),
                empty(2, -1), empty(-2, 1), empty(1, 1), empty(-1, -1),
                wall(1, -1), wall(-1, 1)),
            listOf(
                stack(0, 0, DOWN, 2),
                stack(1, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT),
                stack(0, 1, DOWN_LEFT),
                stack(0, -1, UP_RIGHT),
                stack(2, -1, UP_RIGHT),
                stack(-2, 1, DOWN_LEFT),
                stack(-1, -1, UP)
            )
        ),

        // Level 13
        level(13,
            listOf(empty(3, 0), empty(3, -1), empty(-2, 0), empty(-2, 1),
                empty(0, -2), empty(0, 3), empty(1, 2), empty(2, 1),
                empty(-1, -1), empty(-1, 2)),
            listOf(
                stack(0, 0, DOWN_RIGHT),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(2, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT),
                stack(0, 1, DOWN, 2),
                stack(1, 1, DOWN_RIGHT),
                stack(0, -1, UP),
                stack(-1, 1, DOWN_LEFT),
                stack(2, -1, UP_RIGHT)
            )
        ),

        // Level 14
        level(14,
            listOf(empty(3, 0), empty(-2, 0), empty(0, -1), empty(2, -1),
                empty(-1, 2), empty(1, 2), empty(2, 1), empty(-2, 1),
                empty(3, -1), empty(-1, -1), wall(0, 2)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT),
                stack(2, 0, DOWN_RIGHT, 2),
                stack(-1, 0, UP_LEFT),
                stack(0, 1, DOWN_LEFT),
                stack(1, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT),
                stack(1, -1, UP_RIGHT, 2),
                stack(-1, 0, UP_LEFT)
            )
        ),

        // Level 15 — Multiple walls
        level(15,
            listOf(empty(3, 0), empty(-2, 0), empty(0, -2), empty(0, 3),
                empty(2, 1), empty(-2, 2), empty(1, 2), empty(-1, -1),
                empty(3, -1), empty(-1, 2), wall(2, -1), wall(-1, 1)),
            listOf(
                stack(0, 0, DOWN_RIGHT, 2),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(2, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT),
                stack(0, 1, DOWN),
                stack(1, 1, DOWN_RIGHT),
                stack(0, -1, UP, 2),
                stack(1, -1, UP_RIGHT),
                stack(-2, 1, DOWN_LEFT),
                stack(-1, 2, DOWN_LEFT)
            )
        ),

        // ═══ MEDIUM-HARD (16-20) ═══

        // Level 16
        level(16,
            listOf(empty(3, 0), empty(4, 0), empty(-2, 0), empty(-3, 0),
                empty(0, -2), empty(0, 3), empty(2, 1), empty(-2, 2),
                empty(3, -1), empty(-1, -1), empty(1, 2), empty(-1, 2),
                wall(0, 2)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(2, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 2),
                stack(0, 1, DOWN_LEFT),
                stack(1, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT),
                stack(0, -1, UP),
                stack(1, -1, UP_RIGHT, 2),
                stack(2, -1, UP_RIGHT),
                stack(-2, 1, DOWN_LEFT)
            )
        ),

        // Level 17
        level(17,
            listOf(empty(3, 0), empty(-3, 0), empty(0, -2), empty(0, 3),
                empty(2, 1), empty(-2, 2), empty(3, -1), empty(-3, 1),
                empty(1, 2), empty(-1, -1), empty(-1, 2), empty(2, -1),
                wall(-2, 1), wall(2, 0)),
            listOf(
                stack(0, 0, DOWN_RIGHT, 2),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(-1, 0, UP_LEFT),
                stack(0, 1, DOWN, 3),
                stack(1, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT),
                stack(0, -1, UP, 2),
                stack(1, -1, UP_RIGHT),
                stack(-2, 0, UP_LEFT),
                stack(-1, -1, UP),
                stack(1, 2, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT)
            )
        ),

        // Level 18
        level(18,
            listOf(empty(3, 0), empty(-2, 0), empty(0, -2), empty(0, 3),
                empty(2, 1), empty(-2, 2), empty(3, -1), empty(-1, -1),
                empty(1, 2), empty(-1, 2), empty(2, -1), empty(-2, 1),
                wall(1, -1)),
            listOf(
                stack(0, 0, DOWN, 2),
                stack(1, 0, DOWN_RIGHT, 3),
                stack(2, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 2),
                stack(0, 1, DOWN_LEFT, 2),
                stack(1, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT),
                stack(0, -1, UP),
                stack(-2, 0, UP_LEFT),
                stack(-1, 0, UP_LEFT),
                stack(2, -1, UP_RIGHT, 2)
            )
        ),

        // Level 19
        level(19,
            listOf(empty(4, 0), empty(-3, 0), empty(0, -2), empty(0, 3),
                empty(3, 1), empty(-3, 2), empty(4, -1), empty(-2, -1),
                empty(2, 2), empty(-2, 3), empty(3, -1), empty(-1, -1),
                wall(0, 2), wall(2, -1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(2, 0, DOWN_RIGHT, 2),
                stack(3, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT),
                stack(-2, 0, UP_LEFT),
                stack(0, 1, DOWN_LEFT, 2),
                stack(1, 1, DOWN_RIGHT),
                stack(2, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT),
                stack(-2, 1, DOWN_LEFT),
                stack(0, -1, UP, 2),
                stack(1, -1, UP_RIGHT)
            )
        ),

        // Level 20
        level(20,
            listOf(empty(4, 0), empty(-3, 0), empty(0, -2), empty(0, 3),
                empty(3, 1), empty(-3, 2), empty(4, -1), empty(-2, -1),
                empty(2, 2), empty(-2, 3), empty(1, 2), empty(-1, 2),
                wall(2, -1), wall(-1, -1), wall(3, -1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(2, 0, DOWN_RIGHT),
                stack(3, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 2),
                stack(-2, 0, UP_LEFT),
                stack(0, 1, DOWN, 2),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(-1, 1, DOWN_LEFT),
                stack(-2, 1, DOWN_LEFT),
                stack(0, -1, UP, 2),
                stack(1, -1, UP_RIGHT),
                stack(2, 1, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT)
            )
        ),

        // ═══ HARD (21-25) ═══
        // Larger boards, more stacks, more walls

        level(21,
            listOf(empty(4, 0), empty(-3, 0), empty(0, -2), empty(0, 4),
                empty(3, 1), empty(-3, 2), empty(4, -1), empty(-2, -1),
                empty(2, 2), empty(-2, 3), empty(1, 3), empty(-1, 3),
                empty(3, -1), empty(-3, 1), wall(0, 3), wall(2, -1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(2, 0, DOWN_RIGHT),
                stack(3, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 2),
                stack(-2, 0, UP_LEFT),
                stack(0, 1, DOWN_LEFT, 2),
                stack(1, 1, DOWN_RIGHT),
                stack(2, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT),
                stack(-2, 1, DOWN_LEFT),
                stack(0, 2, DOWN, 2),
                stack(1, 2, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT),
                stack(0, -1, UP, 2)
            )
        ),

        level(22,
            listOf(empty(4, 0), empty(-3, 0), empty(0, -2), empty(0, 4),
                empty(3, 1), empty(-3, 2), empty(4, -1), empty(-2, -1),
                empty(2, 2), empty(-2, 3), empty(1, 3), empty(-1, 3),
                wall(3, -1), wall(-3, 1), wall(0, 3)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(2, 0, DOWN_RIGHT, 2),
                stack(3, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 2),
                stack(-2, 0, UP_LEFT),
                stack(0, 1, DOWN, 2),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(2, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT, 2),
                stack(-2, 1, DOWN_LEFT),
                stack(0, 2, DOWN_LEFT, 2),
                stack(1, 2, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT),
                stack(0, -1, UP, 2),
                stack(1, -1, UP_RIGHT)
            )
        ),

        level(23,
            listOf(empty(4, 0), empty(-4, 0), empty(0, -2), empty(0, 4),
                empty(3, 1), empty(-3, 2), empty(4, -1), empty(-2, -1),
                empty(2, 2), empty(-2, 3), empty(1, 3), empty(-1, 3),
                empty(3, -1), empty(-3, 1), wall(0, 3), wall(-1, -1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 3),
                stack(2, 0, DOWN_RIGHT, 2),
                stack(3, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 2),
                stack(-2, 0, UP_LEFT, 2),
                stack(-3, 0, UP_LEFT),
                stack(0, 1, DOWN_LEFT, 2),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(-1, 1, DOWN_LEFT),
                stack(0, 2, DOWN, 2),
                stack(-1, 2, DOWN_LEFT),
                stack(1, 2, DOWN_RIGHT),
                stack(0, -1, UP, 2),
                stack(1, -1, UP_RIGHT)
            )
        ),

        level(24,
            listOf(empty(4, 0), empty(-4, 0), empty(0, -3), empty(0, 4),
                empty(3, 1), empty(-3, 2), empty(4, -1), empty(-4, 1),
                empty(2, 2), empty(-2, 3), empty(1, 3), empty(-1, 3),
                empty(3, -1), empty(-3, 1), empty(2, -2), empty(-2, -1),
                wall(0, 3), wall(-1, -1), wall(3, -1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 2),
                stack(2, 0, DOWN_RIGHT, 2),
                stack(3, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 3),
                stack(-2, 0, UP_LEFT, 2),
                stack(-3, 0, UP_LEFT),
                stack(0, 1, DOWN, 2),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(2, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT, 2),
                stack(-2, 1, DOWN_LEFT),
                stack(0, 2, DOWN_LEFT, 2),
                stack(1, 2, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT),
                stack(0, -1, UP, 2),
                stack(1, -1, UP_RIGHT)
            )
        ),

        level(25,
            listOf(empty(4, 0), empty(-4, 0), empty(0, -3), empty(0, 4),
                empty(3, 1), empty(-3, 2), empty(4, -1), empty(-4, 1),
                empty(2, 2), empty(-2, 3), empty(1, 3), empty(-1, 3),
                empty(3, -1), empty(-3, 1), empty(2, -2), empty(-2, -1),
                empty(3, -2), empty(-3, 0),
                wall(0, 3), wall(-1, -1), wall(2, -1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 3),
                stack(2, 0, DOWN_RIGHT, 2),
                stack(3, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 3),
                stack(-2, 0, UP_LEFT, 2),
                stack(0, 1, DOWN, 2),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(2, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT, 2),
                stack(-2, 1, DOWN_LEFT),
                stack(0, 2, DOWN_LEFT, 2),
                stack(1, 2, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT),
                stack(0, -1, UP, 3),
                stack(1, -1, UP_RIGHT, 2),
                stack(-2, 2, DOWN_LEFT),
                stack(2, -1, UP_RIGHT)
            )
        ),

        // ═══ EXPERT (26-30) ═══

        level(26,
            listOf(empty(4, 0), empty(-4, 0), empty(0, -3), empty(0, 4),
                empty(3, 1), empty(-3, 2), empty(4, -1), empty(-4, 1),
                empty(2, 2), empty(-2, 3), empty(1, 3), empty(-1, 3),
                empty(3, -1), empty(-3, 1), empty(2, -2), empty(-2, -1),
                wall(0, 3), wall(-1, -1), wall(3, -1), wall(-3, 0)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 3),
                stack(2, 0, DOWN_RIGHT, 2),
                stack(3, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 3),
                stack(-2, 0, UP_LEFT, 2),
                stack(0, 1, DOWN_LEFT, 3),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(2, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT, 2),
                stack(-2, 1, DOWN_LEFT),
                stack(0, 2, DOWN, 2),
                stack(1, 2, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT, 2),
                stack(0, -1, UP, 3),
                stack(1, -1, UP_RIGHT, 2),
                stack(0, -2, UP, 2),
                stack(-2, 2, DOWN_LEFT)
            )
        ),

        level(27,
            listOf(empty(5, 0), empty(-4, 0), empty(0, -3), empty(0, 5),
                empty(4, 1), empty(-4, 2), empty(5, -1), empty(-4, 1),
                empty(3, 2), empty(-3, 3), empty(2, 3), empty(-2, 4),
                empty(4, -1), empty(-4, 1), empty(3, -2), empty(-3, -1),
                wall(0, 4), wall(-2, -1), wall(4, -1), wall(-1, -1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 3),
                stack(2, 0, DOWN_RIGHT, 2),
                stack(3, 0, DOWN_RIGHT, 2),
                stack(4, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 3),
                stack(-2, 0, UP_LEFT, 2),
                stack(-3, 0, UP_LEFT),
                stack(0, 1, DOWN, 2),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(2, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT, 2),
                stack(-2, 1, DOWN_LEFT),
                stack(0, 2, DOWN_LEFT, 2),
                stack(1, 2, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT),
                stack(0, -1, UP, 3),
                stack(1, -1, UP_RIGHT, 2),
                stack(-1, -1, UP_LEFT),
                stack(2, -1, UP_RIGHT)
            )
        ),

        level(28,
            listOf(empty(5, 0), empty(-4, 0), empty(0, -3), empty(0, 5),
                empty(4, 1), empty(-4, 2), empty(5, -1), empty(-3, -1),
                empty(3, 2), empty(-3, 3), empty(2, 3), empty(-2, 4),
                empty(4, -1), empty(-4, 1), empty(3, -2), empty(-2, -1),
                wall(0, 4), wall(-1, -1), wall(4, -1), wall(-3, 1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 3),
                stack(2, 0, DOWN_RIGHT, 3),
                stack(3, 0, DOWN_RIGHT, 2),
                stack(4, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 3),
                stack(-2, 0, UP_LEFT, 2),
                stack(-3, 0, UP_LEFT),
                stack(0, 1, DOWN, 3),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(2, 1, DOWN_RIGHT, 2),
                stack(3, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT, 2),
                stack(-2, 1, DOWN_LEFT),
                stack(0, 2, DOWN_LEFT, 2),
                stack(1, 2, DOWN_RIGHT),
                stack(-1, 2, DOWN_LEFT, 2),
                stack(0, -1, UP, 3),
                stack(1, -1, UP_RIGHT, 2),
                stack(2, -1, UP_RIGHT)
            )
        ),

        level(29,
            listOf(empty(5, 0), empty(-5, 0), empty(0, -3), empty(0, 5),
                empty(4, 1), empty(-4, 2), empty(5, -1), empty(-5, 1),
                empty(3, 2), empty(-3, 3), empty(2, 3), empty(-2, 4),
                empty(4, -1), empty(-4, 1), empty(3, -2), empty(-3, -1),
                empty(4, -2), empty(-4, 0),
                wall(0, 4), wall(-2, -1), wall(4, -1), wall(-1, -1), wall(3, -1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 3),
                stack(2, 0, DOWN_RIGHT, 3),
                stack(3, 0, DOWN_RIGHT, 2),
                stack(4, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 3),
                stack(-2, 0, UP_LEFT, 2),
                stack(-3, 0, UP_LEFT, 2),
                stack(0, 1, DOWN, 3),
                stack(1, 1, DOWN_RIGHT, 2),
                stack(2, 1, DOWN_RIGHT, 2),
                stack(3, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT, 2),
                stack(-2, 1, DOWN_LEFT, 2),
                stack(-3, 1, DOWN_LEFT),
                stack(0, 2, DOWN, 2),
                stack(1, 2, DOWN_RIGHT, 2),
                stack(-1, 2, DOWN_LEFT),
                stack(0, -1, UP, 3),
                stack(1, -1, UP_RIGHT, 2),
                stack(2, -1, UP_RIGHT),
                stack(0, -2, UP, 2)
            )
        ),

        level(30,
            listOf(empty(5, 0), empty(-5, 0), empty(0, -4), empty(0, 5),
                empty(4, 1), empty(-4, 2), empty(5, -1), empty(-5, 1),
                empty(3, 2), empty(-3, 3), empty(2, 3), empty(-2, 4),
                empty(4, -1), empty(-4, 1), empty(3, -2), empty(-3, -1),
                empty(4, -2), empty(-4, 0), empty(2, -2), empty(-2, -1),
                wall(0, 4), wall(-1, -2), wall(4, -1), wall(-1, -1),
                wall(3, -1), wall(-3, 1)),
            listOf(
                stack(0, 0, DOWN, 3),
                stack(1, 0, DOWN_RIGHT, 3),
                stack(2, 0, DOWN_RIGHT, 3),
                stack(3, 0, DOWN_RIGHT, 2),
                stack(4, 0, DOWN_RIGHT),
                stack(-1, 0, UP_LEFT, 3),
                stack(-2, 0, UP_LEFT, 3),
                stack(-3, 0, UP_LEFT, 2),
                stack(-4, 0, UP_LEFT),
                stack(0, 1, DOWN, 3),
                stack(1, 1, DOWN_RIGHT, 3),
                stack(2, 1, DOWN_RIGHT, 2),
                stack(3, 1, DOWN_RIGHT),
                stack(-1, 1, DOWN_LEFT, 2),
                stack(-2, 1, DOWN_LEFT, 2),
                stack(-3, 2, DOWN_LEFT),
                stack(0, 2, DOWN, 2),
                stack(1, 2, DOWN_RIGHT, 2),
                stack(-1, 2, DOWN_LEFT),
                stack(0, -1, UP, 3),
                stack(1, -1, UP_RIGHT, 2),
                stack(2, -1, UP_RIGHT, 2),
                stack(0, -2, UP, 2),
                stack(0, -3, UP)
            )
        )
    )
}
