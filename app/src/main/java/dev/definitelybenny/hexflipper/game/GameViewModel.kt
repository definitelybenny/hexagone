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
 * Wraps [GameState] and exposes reactive [StateFlow]s that the UI
 * can collect. Animations are coordinated through [animatingStack]:
 * a successful move sets the animating stack first, and the board
 * state is only updated once [onAnimationComplete] is called.
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

    private val _animatingStack = MutableStateFlow<AnimatingStack?>(null)
    val animatingStack: StateFlow<AnimatingStack?> = _animatingStack.asStateFlow()

    private val _currentLevelNumber = MutableStateFlow(1)
    val currentLevelNumber: StateFlow<Int> = _currentLevelNumber.asStateFlow()

    /** The result of the pending move, applied after animation completes. */
    private var pendingMoveResult: MoveResult.Success? = null

    /** Snapshot taken before a pending move so we can defer the actual removal. */
    private var stateBeforePendingMove: BoardState? = null

    // ── Public API ───────────────────────────────────────────

    /** Load (or reload) a level, resetting all state. */
    fun loadLevel(level: Level) {
        _animatingStack.value = null
        pendingMoveResult = null
        stateBeforePendingMove = null
        gameState.reset(level)
        _currentLevelNumber.value = level.number
        publishState()
    }

    /**
     * Called when the player taps a hex cell.
     *
     * If a stack is already animating the tap is ignored.
     * On a successful move the animation is started but the board state
     * is not yet updated -- that happens in [onAnimationComplete].
     */
    fun onCellTapped(cell: HexCell) {
        // Ignore taps while an animation is in progress.
        if (_animatingStack.value != null) return

        val stack = gameState.boardState.stackAt(cell) ?: return

        // Compute path *before* mutating state (needed for slide animation).
        val path = gameState.boardState.slidePath(cell)

        // Perform the move inside GameState (updates move count for both success and blocked).
        val result = gameState.tapCell(cell)

        when (result) {
            is MoveResult.Blocked -> {
                // Show a shake animation and update move count.
                _moveCount.value = gameState.moveCount
                _animatingStack.value = AnimatingStack(
                    cell = cell,
                    stack = stack,
                    path = listOf(cell),
                    type = AnimationType.SHAKE
                )
            }
            is MoveResult.Success -> {
                // We want the UI to keep showing the stack while it animates out,
                // so we defer the board state update until onAnimationComplete.
                stateBeforePendingMove = gameState.boardState
                pendingMoveResult = result

                // Publish updated move count / undo availability immediately,
                // but keep the old board so the stack is still visible.
                _moveCount.value = gameState.moveCount
                _canUndo.value = gameState.canUndo

                _animatingStack.value = AnimatingStack(
                    cell = cell,
                    stack = result.stack,
                    path = path,
                    type = AnimationType.SLIDE_OFF
                )
            }
            is MoveResult.Empty -> { /* shouldn't reach here since we checked stackAt above */ }
        }
    }

    /** Undo the last move. */
    fun onUndo() {
        // Cancel any in-progress animation.
        if (_animatingStack.value != null) {
            _animatingStack.value = null
            pendingMoveResult = null
            stateBeforePendingMove = null
        }
        gameState.undo()
        publishState()
    }

    /**
     * Highlight a cell whose stack can be moved.
     *
     * Triggers a shake animation on the movable stack
     * to draw the player's eye.
     */
    fun onHint() {
        if (_animatingStack.value != null) return
        val hintCell = gameState.getHint() ?: return
        val stack = gameState.boardState.stackAt(hintCell) ?: return
        _animatingStack.value = AnimatingStack(
            cell = hintCell,
            stack = stack,
            path = listOf(hintCell),
            type = AnimationType.SHAKE
        )
    }

    /**
     * Called by the UI once the current animation finishes.
     *
     * If the animation was a slide-off, the board state is now updated
     * (removing the stack) and completion is checked.
     */
    fun onAnimationComplete() {
        val pending = pendingMoveResult
        _animatingStack.value = null

        if (pending != null) {
            pendingMoveResult = null
            stateBeforePendingMove = null
            // Now publish the real (post-removal) board state.
            publishState()
        }
    }

    // ── Internals ────────────────────────────────────────────

    /** Push all derived values from [gameState] into the flows. */
    private fun publishState() {
        _boardState.value = gameState.boardState
        _moveCount.value = gameState.moveCount
        _isComplete.value = gameState.isComplete
        _starRating.value = gameState.starRating
        _canUndo.value = gameState.canUndo
    }
}
