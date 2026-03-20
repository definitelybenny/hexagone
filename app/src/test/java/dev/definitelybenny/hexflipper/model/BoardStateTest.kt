package dev.definitelybenny.hexflipper.model

import org.junit.Assert.*
import org.junit.Test

class BoardStateTest {

    private fun simpleBoard(): BoardState {
        // 3 cells in a row: (0,0) stack pointing right, (1,0) empty, (2,0) empty
        val c0 = HexCell(0, 0)
        val c1 = HexCell(1, 0)
        val c2 = HexCell(2, 0)
        val stack = HexStack(c0, PieceColor.YELLOW, HexDirection.DOWN_RIGHT, 1)
        return BoardState(
            cells = mapOf(c1 to CellType.EMPTY, c2 to CellType.EMPTY),
            stacks = mapOf(c0 to stack)
        )
    }

    @Test
    fun `boardCells includes both cells and stacks`() {
        val board = simpleBoard()
        assertEquals(3, board.boardCells.size)
        assertTrue(board.boardCells.contains(HexCell(0, 0)))
        assertTrue(board.boardCells.contains(HexCell(1, 0)))
        assertTrue(board.boardCells.contains(HexCell(2, 0)))
    }

    @Test
    fun `stackAt returns stack when present`() {
        val board = simpleBoard()
        assertNotNull(board.stackAt(HexCell(0, 0)))
    }

    @Test
    fun `stackAt returns null for empty cell`() {
        val board = simpleBoard()
        assertNull(board.stackAt(HexCell(1, 0)))
    }

    @Test
    fun `stackAt returns null for cell not on board`() {
        val board = simpleBoard()
        assertNull(board.stackAt(HexCell(99, 99)))
    }

    @Test
    fun `canMove returns true when path to edge is clear`() {
        val board = simpleBoard()
        // Stack at (0,0) pointing DOWN_RIGHT, path goes (1,0) empty, (2,0) empty, then off board
        assertTrue(board.canMove(HexCell(0, 0)))
    }

    @Test
    fun `canMove returns false when blocked by another stack`() {
        val c0 = HexCell(0, 0)
        val c1 = HexCell(1, 0)
        val stack0 = HexStack(c0, PieceColor.YELLOW, HexDirection.DOWN_RIGHT, 1)
        val stack1 = HexStack(c1, PieceColor.RED, HexDirection.UP, 1)
        val board = BoardState(cells = emptyMap(), stacks = mapOf(c0 to stack0, c1 to stack1))
        assertFalse(board.canMove(c0))
    }

    @Test
    fun `canMove returns false for cell with no stack`() {
        val board = simpleBoard()
        assertFalse(board.canMove(HexCell(1, 0)))
    }

    @Test
    fun `removeStack removes the stack and adds empty cell`() {
        val board = simpleBoard()
        val after = board.removeStack(HexCell(0, 0))
        assertNull(after.stackAt(HexCell(0, 0)))
        assertEquals(CellType.EMPTY, after.cells[HexCell(0, 0)])
        assertEquals(0, after.stacks.size)
    }

    @Test
    fun `isComplete is true when no stacks remain`() {
        val board = simpleBoard()
        val after = board.removeStack(HexCell(0, 0))
        assertTrue(after.isComplete)
    }

    @Test
    fun `isComplete is false when stacks remain`() {
        val board = simpleBoard()
        assertFalse(board.isComplete)
    }

    @Test
    fun `totalStacks returns correct count`() {
        val board = simpleBoard()
        assertEquals(1, board.totalStacks)
    }

    @Test
    fun `slidePath includes start cell and exit cell`() {
        val board = simpleBoard()
        val path = board.slidePath(HexCell(0, 0))
        assertTrue(path.isNotEmpty())
        assertEquals(HexCell(0, 0), path.first())
        // Last cell should be off the board
        assertFalse(board.boardCells.contains(path.last()))
    }

    @Test
    fun `slidePath returns empty for cell with no stack`() {
        val board = simpleBoard()
        val path = board.slidePath(HexCell(1, 0))
        assertTrue(path.isEmpty())
    }
}
