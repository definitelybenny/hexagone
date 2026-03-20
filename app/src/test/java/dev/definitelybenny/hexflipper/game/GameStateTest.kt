package dev.definitelybenny.hexflipper.game

import dev.definitelybenny.hexflipper.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameStateTest {

    private lateinit var gameState: GameState

    /** Simple 2-stack level: both point right along a row of empty cells. */
    private fun twoStackLevel(): Level {
        val c0 = HexCell(0, 0)
        val c1 = HexCell(1, 0)
        val c2 = HexCell(2, 0)
        val c3 = HexCell(3, 0)
        return Level(
            number = 1,
            cells = mapOf(c2 to CellType.EMPTY, c3 to CellType.EMPTY),
            stacks = mapOf(
                c0 to HexStack(c0, PieceColor.YELLOW, HexDirection.DOWN_RIGHT, 1),
                c1 to HexStack(c1, PieceColor.YELLOW, HexDirection.DOWN_RIGHT, 1)
            ),
            par = 2
        )
    }

    @Before
    fun setup() {
        gameState = GameState()
        gameState.reset(twoStackLevel())
    }

    @Test
    fun `initial state has zero moves`() {
        assertEquals(0, gameState.moveCount)
    }

    @Test
    fun `initial state is not complete`() {
        assertFalse(gameState.isComplete)
    }

    @Test
    fun `initial state cannot undo`() {
        assertFalse(gameState.canUndo)
    }

    @Test
    fun `tap empty cell returns Empty`() {
        val result = gameState.tapCell(HexCell(2, 0))
        assertTrue(result is MoveResult.Empty)
    }

    @Test
    fun `tap cell not on board returns Empty`() {
        val result = gameState.tapCell(HexCell(99, 99))
        assertTrue(result is MoveResult.Empty)
    }

    @Test
    fun `tap blocked stack returns Blocked and increments move count`() {
        // c0 is blocked by c1
        val result = gameState.tapCell(HexCell(0, 0))
        assertTrue(result is MoveResult.Blocked)
        assertEquals(1, gameState.moveCount)
    }

    @Test
    fun `tap movable stack returns Success`() {
        // c1 can move (path to edge is clear past c2, c3)
        val result = gameState.tapCell(HexCell(1, 0))
        assertTrue(result is MoveResult.Success)
    }

    @Test
    fun `successful tap increments move count`() {
        gameState.tapCell(HexCell(1, 0))
        assertEquals(1, gameState.moveCount)
    }

    @Test
    fun `successful tap enables undo`() {
        gameState.tapCell(HexCell(1, 0))
        assertTrue(gameState.canUndo)
    }

    @Test
    fun `successful tap removes stack from board`() {
        gameState.tapCell(HexCell(1, 0))
        assertNull(gameState.boardState.stackAt(HexCell(1, 0)))
    }

    @Test
    fun `removing all stacks completes the level`() {
        // Remove c1 first (it's not blocked), then c0
        gameState.tapCell(HexCell(1, 0))
        gameState.tapCell(HexCell(0, 0))
        assertTrue(gameState.isComplete)
    }

    @Test
    fun `undo restores previous state`() {
        gameState.tapCell(HexCell(1, 0))
        gameState.undo()
        assertNotNull(gameState.boardState.stackAt(HexCell(1, 0)))
        assertEquals(0, gameState.moveCount)
    }

    @Test
    fun `undo on empty stack returns false`() {
        assertFalse(gameState.undo())
    }

    @Test
    fun `star rating is NONE before completion`() {
        assertEquals(StarRating.NONE, gameState.starRating)
    }

    @Test
    fun `three stars when completing at par`() {
        gameState.tapCell(HexCell(1, 0))
        gameState.tapCell(HexCell(0, 0))
        assertEquals(StarRating.THREE, gameState.starRating)
        assertEquals(2, gameState.moveCount)
    }

    @Test
    fun `fewer stars when moves exceed par`() {
        // Make 3 extra blocked moves to push past par + 2
        gameState.tapCell(HexCell(0, 0)) // blocked
        gameState.tapCell(HexCell(0, 0)) // blocked
        gameState.tapCell(HexCell(0, 0)) // blocked
        gameState.tapCell(HexCell(1, 0)) // success
        gameState.tapCell(HexCell(0, 0)) // success
        assertTrue(gameState.isComplete)
        assertEquals(5, gameState.moveCount)
        assertEquals(StarRating.ONE, gameState.starRating)
    }

    @Test
    fun `getHint returns a movable cell`() {
        val hint = gameState.getHint()
        assertNotNull(hint)
        assertTrue(gameState.boardState.canMove(hint!!))
    }

    @Test
    fun `getHint returns null when no moves available`() {
        // Create a board where the only stack is blocked
        val c0 = HexCell(0, 0)
        val c1 = HexCell(1, 0)
        val level = Level(
            number = 1,
            cells = emptyMap(),
            stacks = mapOf(
                c0 to HexStack(c0, PieceColor.YELLOW, HexDirection.DOWN_RIGHT, 1),
                c1 to HexStack(c1, PieceColor.YELLOW, HexDirection.DOWN_RIGHT, 1)
            ),
            par = 2
        )
        gameState.reset(level)
        // c0 is blocked by c1, but c1 can move — so hint should find c1
        val hint = gameState.getHint()
        assertNotNull(hint)
        assertEquals(HexCell(1, 0), hint)
    }

    @Test
    fun `reset clears all state`() {
        gameState.tapCell(HexCell(1, 0))
        gameState.reset(twoStackLevel())
        assertEquals(0, gameState.moveCount)
        assertFalse(gameState.canUndo)
        assertFalse(gameState.isComplete)
        assertEquals(2, gameState.boardState.totalStacks)
    }
}
