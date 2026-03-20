package dev.definitelybenny.hexflipper.model

import org.junit.Assert.*
import org.junit.Test

class HexCellTest {

    @Test
    fun `cube coordinate s is derived correctly`() {
        val cell = HexCell(2, 3)
        assertEquals(-5, cell.s)
    }

    @Test
    fun `origin has all zero coordinates`() {
        val cell = HexCell(0, 0)
        assertEquals(0, cell.q)
        assertEquals(0, cell.r)
        assertEquals(0, cell.s)
    }

    @Test
    fun `neighbor in each direction produces correct offset`() {
        val origin = HexCell(0, 0)
        assertEquals(HexCell(0, -1), origin.neighbor(HexDirection.UP))
        assertEquals(HexCell(1, -1), origin.neighbor(HexDirection.UP_RIGHT))
        assertEquals(HexCell(1, 0), origin.neighbor(HexDirection.DOWN_RIGHT))
        assertEquals(HexCell(0, 1), origin.neighbor(HexDirection.DOWN))
        assertEquals(HexCell(-1, 1), origin.neighbor(HexDirection.DOWN_LEFT))
        assertEquals(HexCell(-1, 0), origin.neighbor(HexDirection.UP_LEFT))
    }

    @Test
    fun `neighbor from non-origin cell`() {
        val cell = HexCell(3, -2)
        assertEquals(HexCell(3, -3), cell.neighbor(HexDirection.UP))
        assertEquals(HexCell(4, -2), cell.neighbor(HexDirection.DOWN_RIGHT))
    }

    @Test
    fun `opposite neighbors cancel out`() {
        val origin = HexCell(0, 0)
        for (dir in HexDirection.entries) {
            val opposite = when (dir) {
                HexDirection.UP -> HexDirection.DOWN
                HexDirection.UP_RIGHT -> HexDirection.DOWN_LEFT
                HexDirection.DOWN_RIGHT -> HexDirection.UP_LEFT
                HexDirection.DOWN -> HexDirection.UP
                HexDirection.DOWN_LEFT -> HexDirection.UP_RIGHT
                HexDirection.UP_LEFT -> HexDirection.DOWN_RIGHT
            }
            val result = origin.neighbor(dir).neighbor(opposite)
            assertEquals("Going $dir then $opposite should return to origin", origin, result)
        }
    }

    @Test
    fun `six neighbors of origin are all distinct`() {
        val origin = HexCell(0, 0)
        val neighbors = HexDirection.entries.map { origin.neighbor(it) }.toSet()
        assertEquals(6, neighbors.size)
    }

    @Test
    fun `toPixel produces different positions for different cells`() {
        val a = HexCell(0, 0).toPixel(10f)
        val b = HexCell(1, 0).toPixel(10f)
        assertNotEquals(a, b)
    }

    @Test
    fun `equality and hashCode work correctly`() {
        val a = HexCell(1, 2)
        val b = HexCell(1, 2)
        val c = HexCell(2, 1)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }
}
