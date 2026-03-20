package dev.definitelybenny.hexflipper.model

import org.junit.Assert.*
import org.junit.Test

class StarRatingTest {

    @Test
    fun `three stars when moves equal par`() {
        assertEquals(StarRating.THREE, StarRating.fromMoves(5, 5))
    }

    @Test
    fun `three stars when moves less than par`() {
        assertEquals(StarRating.THREE, StarRating.fromMoves(3, 5))
    }

    @Test
    fun `two stars when moves equal par plus 1`() {
        assertEquals(StarRating.TWO, StarRating.fromMoves(6, 5))
    }

    @Test
    fun `two stars when moves equal par plus 2`() {
        assertEquals(StarRating.TWO, StarRating.fromMoves(7, 5))
    }

    @Test
    fun `one star when moves exceed par plus 2`() {
        assertEquals(StarRating.ONE, StarRating.fromMoves(8, 5))
    }

    @Test
    fun `one star for large move count`() {
        assertEquals(StarRating.ONE, StarRating.fromMoves(100, 5))
    }

    @Test
    fun `star values are correct`() {
        assertEquals(0, StarRating.NONE.stars)
        assertEquals(1, StarRating.ONE.stars)
        assertEquals(2, StarRating.TWO.stars)
        assertEquals(3, StarRating.THREE.stars)
    }
}
