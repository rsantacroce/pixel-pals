package app.pixelpals.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun ByteArray.u32(at: Int): Int =
    ((this[at].toInt() and 0xFF) shl 24) or ((this[at + 1].toInt() and 0xFF) shl 16) or
        ((this[at + 2].toInt() and 0xFF) shl 8) or (this[at + 3].toInt() and 0xFF)

class ExportTest {
    @Test
    fun pngHasExpectedDimensionsForEveryScale() {
        val d = sampleDrawing()
        for (scale in PngExport.SCALES) {
            val png = PngExport.render(d, 0, scale, whiteBackground = false)
            assertEquals(0x89.toByte(), png[0])
            assertEquals("IHDR", png.copyOfRange(12, 16).decodeToString())
            assertEquals(d.width * scale, png.u32(16))
            assertEquals(d.height * scale, png.u32(20))
            assertEquals("IEND", png.copyOfRange(png.size - 8, png.size - 4).decodeToString())
        }
    }

    @Test
    fun spriteSheetIsFramesWide() {
        val d = sampleDrawing()
        val png = PngExport.spriteSheet(d, 2, whiteBackground = true)
        assertEquals(d.width * 2 * d.frames.size, png.u32(16))
    }

    @Test
    fun gifHasHeaderAndTrailer() {
        val gif = PngExport.gif(sampleDrawing(), 1, whiteBackground = false)
        assertEquals("GIF89a", gif.copyOfRange(0, 6).decodeToString())
        assertEquals(0x3B.toByte(), gif.last())
        assertTrue(gif.size > 100)
    }
}
