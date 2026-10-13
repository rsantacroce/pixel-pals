package app.pixelpals.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RasterTest {
    @Test
    fun lineIncludesBothEndsAndIsConnected() {
        val pts = Raster.line(0, 0, 7, 3)
        assertEquals(Point(0, 0), pts.first())
        assertEquals(Point(7, 3), pts.last())
        assertEquals(8, pts.size)
        pts.zipWithNext().forEach { (a, b) -> assertTrue(abs(a.x - b.x) <= 1 && abs(a.y - b.y) <= 1) }
    }

    @Test
    fun lineHandlesAllDirectionsAndSinglePoint() {
        assertEquals(listOf(Point(3, 3)), Raster.line(3, 3, 3, 3))
        assertEquals(5, Raster.line(4, 0, 0, 0).size)
        assertEquals(5, Raster.line(0, 4, 0, 0).size)
        assertEquals(Raster.line(0, 0, 5, 5).toSet(), Raster.line(5, 5, 0, 0).toSet())
    }

    @Test
    fun rectangleOutlineAndFilled() {
        assertEquals(12, Raster.rectangle(0, 0, 3, 3, filled = false).size)
        assertEquals(16, Raster.rectangle(3, 3, 0, 0, filled = true).size)
    }

    @Test
    fun ellipseIsSymmetricAndWithinBounds() {
        for ((w, h) in listOf(1 to 1, 2 to 2, 5 to 3, 8 to 8, 9 to 4, 1 to 6)) {
            val pts = Raster.ellipse(0, 0, w - 1, h - 1, filled = false).toSet()
            assertTrue(pts.all { it.x in 0 until w && it.y in 0 until h }, "bounds ${w}x$h")
            assertTrue(pts.all { Point(w - 1 - it.x, it.y) in pts }, "h-symmetry ${w}x$h")
            assertTrue(pts.all { Point(it.x, h - 1 - it.y) in pts }, "v-symmetry ${w}x$h")
            assertTrue(Point(0, (h - 1) / 2) in pts && Point((w - 1) / 2, 0) in pts, "touches edges ${w}x$h")
        }
    }

    @Test
    fun filledEllipseContainsOutline() {
        val outline = Raster.ellipse(0, 0, 9, 6, filled = false).toSet()
        val filled = Raster.ellipse(0, 0, 9, 6, filled = true).toSet()
        assertTrue(filled.containsAll(outline))
        assertTrue(Point(4, 3) in filled)
    }

    @Test
    fun brushSizes() {
        assertEquals(1, Raster.brush(5, 5, 1).size)
        assertEquals(4, Raster.brush(5, 5, 2).size)
        assertEquals(9, Raster.brush(5, 5, 3).size)
    }
}
