package app.pixelpals.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class FloodFillTest {
    @Test
    fun fillsWholeEmptyCanvas() {
        assertEquals(64, FloodFill.region(IntArray(64), 8, 8, 3, 3).size)
    }

    @Test
    fun outOfBoundsReturnsEmpty() {
        assertEquals(0, FloodFill.region(IntArray(64), 8, 8, -1, 3).size)
        assertEquals(0, FloodFill.region(IntArray(64), 8, 8, 8, 0).size)
    }

    @Test
    fun stopsAtWallsAndDoesNotLeakDiagonally() {
        val w = 8
        val px = IntArray(w * w)
        // Diagonal line from (0,7) to (7,0) splits the canvas; 4-way fill must not cross it.
        for (i in 0 until w) px[(w - 1 - i) * w + i] = RED
        val region = FloodFill.region(px, w, w, 0, 0)
        assertEquals(28, region.size)
        assertTrue(region.all { px[it] == 0 })
    }

    @Test
    fun fillsOnlyMatchingColor() {
        val px = intArrayOf(RED, RED, 0, RED)
        assertEquals(setOf(0, 1, 3), FloodFill.region(px, 2, 2, 0, 0).toSet())
    }

    @Test
    fun handlesMaximumCanvasWithSpiralWithoutOverflow() {
        val n = Drawing.MAX_SIZE
        val px = IntArray(n * n)
        // A maze of vertical walls with alternating gaps forces a long serpentine region.
        for (x in 1 until n step 2) {
            val gapY = if ((x / 2) % 2 == 0) n - 1 else 0
            for (y in 0 until n) if (y != gapY) px[y * n + x] = RED
        }
        val region = FloodFill.region(px, n, n, 0, 0)
        assertEquals(px.count { it == 0 }, region.size)
    }

    @Test
    fun largeFillIsFast() {
        val n = Drawing.MAX_SIZE
        val px = IntArray(n * n)
        FloodFill.region(px, n, n, 0, 0) // warm-up
        val mark = TimeSource.Monotonic.markNow()
        repeat(5) { FloodFill.region(px, n, n, n / 2, n / 2) }
        val perFill = mark.elapsedNow().inWholeMilliseconds / 5
        assertTrue(perFill < 50, "fill took $perFill ms")
    }
}
