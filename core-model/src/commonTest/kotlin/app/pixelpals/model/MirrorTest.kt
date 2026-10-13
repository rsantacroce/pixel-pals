package app.pixelpals.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MirrorTest {
    @Test
    fun noMirrorReturnsPoint() {
        assertEquals(listOf(Point(1, 2)), MirrorMode().map(Point(1, 2), 8, 8))
    }

    @Test
    fun horizontalMirrorsAcrossVerticalAxis() {
        assertEquals(setOf(Point(1, 2), Point(6, 2)), MirrorMode(horizontal = true).map(Point(1, 2), 8, 8).toSet())
    }

    @Test
    fun verticalMirrorsAcrossHorizontalAxis() {
        assertEquals(setOf(Point(1, 2), Point(1, 4)), MirrorMode(vertical = true).map(Point(1, 2), 8, 7).toSet())
    }

    @Test
    fun bothGivesFourPointsAndDedupesCenter() {
        assertEquals(4, MirrorMode(true, true).map(Point(0, 0), 8, 8).size)
        assertEquals(1, MirrorMode(true, true).map(Point(2, 2), 5, 5).size)
    }

    @Test
    fun pencilStrokeIsMirroredLive() {
        val s = editor(8).run(EditorAction.ToggleMirrorHorizontal).tap(1, 1)
        assertEquals(RED, s.pixel(1, 1))
        assertEquals(RED, s.pixel(6, 1))
        assertEquals(2, s.painted())
    }
}
