package dev.definitelybenny.hexflipper.game

import androidx.lifecycle.ViewModel
import dev.definitelybenny.hexflipper.model.BoardState
import dev.definitelybenny.hexflipper.model.HexCell
import dev.definitelybenny.hexflipper.model.Level
import dev.definitelybenny.hexflipper.model.StarRating
import dev.definitelybenny.hexflipper.ui.components.AnimatingStack
import dev.definitelybenny.hexflipper.ui.components.AnimationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Jetpack Compose ViewModel for Hex Flipper.
 *
 * Supports concurrent animations — multiple stacks can animate
 * out simultaneously for fast, responsive gameplay.
 */
class GameViewModel : ViewModel() {

    private val gameState = GameState()

    // ── Exposed state flows ──────────────────────────────────

    private val _boardState = MutableStateFlow(gameState.boardState)
    val boardState: StateFlow<BoardState> = _boardState.asStateFlow()

    private val _moveCount = MutableStateFlow(0)
    val moveCount: StateFlow<Int> = _moveCount.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    private val _starRating = MutableStateFlow(StarRating.NONE)
    val starRating: StateFlow<StarRating> = _starRating.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _animatingStacks = MutableStateFlow<List<AnimatingStack>>(emptyList())
    val animatingStacks: StateFlow<List<AnimatingStack>> = _animatingStacks.asStateFlow()

    private val _currentLevelNumber = MutableStateFlow(1)
    val currentLevelNumber: StateFlow<Int> = _currentLevelNumber.asStateFlow()

    /** Cells currently animating out — treated as already removed for tap logic. */
    private val concurrentPendingCells = mutableSetOf<HexCell>()

    // ── Public API ───────────────────────────────────────────

    /** Load (or reload) a level, resetting all state. */
    fun loadLevel(level: Level) {
        _animatingStacks.value = emptyList()
        concurrentPendingCells.clear()
        gameState.reset(level)
        _currentLevelNumber.value = level.number
        publishState()
    }

    /**
     * Called when the player taps a hex cell.
     * Allows concurrent animations — tap as fast as you want.
     */
    fun onCellTapped(cell: HexCell) {
        // Skip if this cell is already animating out
        if (cell in concurrentPendingCells) return

        val stack = gameState.boardState.stackAt(cell) ?: return

        if (!canMoveConcurrent(cell)) {
            // Blocked — count as move and shake
            gameState.tapCell(cell)
            _moveCount.value = gameState.moveCount

            val shakeAnim = AnimatingStack(
                cell = cell,
                stack = stack,
                path = listOf(cell),
                type = AnimationType.SHAKE
            )
            _animatingStacks.value = _animatingStacks.value + shakeAnim
            return
        }

        // Compute path before mutating
        val path = gameState.boardState.slidePath(cell)

        // Perform the move in GameState
        val result = gameState.tapCell(cell)
        if (result is MoveResult.Success) {
            concurrentPendingCells.add(cell)
            _moveCount.value = gameState.moveCount
            _canUndo.value = gameState.canUndo

            val slideAnim = AnimatingStack(
                cell = cell,
                stack = result.stack,
                path = path,
                type = AnimationType.SLIDE_OFF
            )
            _animatingStacks.value = _animatingStacks.value + slideAnim
        }
    }

    /** Undo the last move. */
    fun onUndo() {
        _animatingStacks.value = emptyList()
        concurrentPendingCells.clear()
        gameState.undo()
        publishState()
    }

    /** Highlight a cell whose stack can be moved. */
    fun onHint() {
        val hintCell = gameState.getHint() ?: return
        if (hintCell in concurrentPendingCells) return
        val stack = gameState.boardState.stackAt(hintCell) ?: return
        val shakeAnim = AnimatingStack(
            cell = hintCell,
            stack = stack,
            path = listOf(hintCell),
            type = AnimationType.SHAKE
        )
        _animatingStacks.value = _animatingStacks.value + shakeAnim
    }

    /** Called by the UI when a specific animation finishes. */
    fun onAnimationComplete(cell: HexCell) {
        concurrentPendingCells.remove(cell)
        _animatingStacks.value = _animatingStacks.value.filter { it.cell != cell }
        publishState()
    }

    // ── Internals ────────────────────────────────────────────

    /**
     * Check if a stack can move, treating concurrently-animating stacks as already gone.
     */
    private fun canMoveConcurrent(cell: HexCell): Boolean {
        val stack = gameState.boardState.stackAt(cell) ?: return false
        var current = cell.neighbor(stack.direction)
        while (true) {
            val boardState = gameState.boardState
            if (current !in boardState.cells && current !in boardState.stacks) {
                return true
            }
            if (boardState.stacks.containsKey(current) && current !in concurrentPendingCells) {
                return false
            }
            if (boardState.cells[current] == dev.definitelybenny.hexflipper.model.CellType.WALL) {
                return false
            }
            current = current.neighbor(stack.direction)
        }
    }

    private fun publishState() {
        _boardState.value = gameState.boardState
        _moveCount.value = gameState.moveCount
        _isComplete.value = gameState.isComplete
        _starRating.value = gameState.starRating
        _canUndo.value = gameState.canUndo
    }
}
