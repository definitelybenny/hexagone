package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.BoardState
import dev.definitelybenny.hexflipper.model.HexCell
import dev.definitelybenny.hexflipper.model.HexStack
import dev.definitelybenny.hexflipper.model.Level
import dev.definitelybenny.hexflipper.model.StarRating

/** Result of attempting to tap a cell. */
sealed class MoveResult {
    /** The entire stack was successfully removed; includes the cell, the removed stack, and the slide path. */
    data class Success(val cell: HexCell, val stack: HexStack, val path: List<HexCell>) : MoveResult()

    /** A stack exists but is blocked and cannot slide off. */
    data class Blocked(val cell: HexCell) : MoveResult()

    /** No stack at the tapped cell. */
    data object Empty : MoveResult()
}

/**
 * Core game state manager for Hex Flipper.
 *
 * Holds the current board with hex stacks, tracks moves, supports undo,
 * and computes completion / star rating.
 */
class GameState {

    var boardState: BoardState = BoardState(emptyMap(), emptyMap())
        private set

    var moveCount: Int = 0
        private set

    var par: Int = 0
        private set

    private val undoStack = mutableListOf<BoardState>()

    // ── Queries ──────────────────────────────────────────────

    val isComplete: Boolean
        get() = boardState.isComplete

    val starRating: StarRating
        get() = if (isComplete) StarRating.fromMoves(moveCount, par) else StarRating.NONE

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    // ── Actions ──────────────────────────────────────────────

    /**
     * Attempt to remove the entire stack at [cell].
     *
     * If a movable stack is found its previous board state is pushed onto
     * the undo stack, the stack is removed, and the move counter increments.
     */
    fun tapCell(cell: HexCell): MoveResult {
        val stack = boardState.stackAt(cell) ?: return MoveResult.Empty

        if (!boardState.canMove(cell)) return MoveResult.Blocked(cell)

        val path = boardState.slidePath(cell)

        // Snapshot before mutation
        undoStack.add(boardState)

        boardState = boardState.removeStack(cell)
        moveCount++

        return MoveResult.Success(cell, stack, path)
    }

    /**
     * Undo the last move. Returns `true` if an undo was performed.
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        boardState = undoStack.removeAt(undoStack.lastIndex)
        moveCount--
        return true
    }

    /**
     * Simple hint: return the first cell with a movable stack, or `null`.
     */
    fun getHint(): HexCell? =
        boardState.stacks.keys.firstOrNull { cell ->
            boardState.canMove(cell)
        }

    /**
     * Reset the game to the initial state of [level].
     */
    fun reset(level: Level) {
        boardState = level.initialBoardState()
        moveCount = 0
        par = level.par
        undoStack.clear()
    }
}
