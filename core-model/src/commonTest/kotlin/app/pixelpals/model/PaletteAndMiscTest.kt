package app.pixelpals.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaletteAndMiscTest {
    @Test
    fun defaultPaletteHasBrightColorsPlusNeutrals() {
        val c = Palettes.Bright.colors
        assertTrue(c.size in 16..30)
        assertTrue(Argb.WHITE in c)
        assertTrue(c.all { Argb.alpha(it) == 255 })
        assertEquals(c.size, c.distinct().size)
        assertTrue(Palettes.all.map { it.id }.containsAll(listOf("rainbow", "pastel", "forest", "ocean", "retro")))
        assertEquals(Palettes.Bright, Palettes.byId("nope"))
    }

    @Test
    fun customColorsDedupeAndCap() {
        var list = emptyList<Int>()
        repeat(20) { list = ColorMemory.addCustom(list, Argb.rgb(it)) }
        assertEquals(ColorMemory.MAX_CUSTOM, list.size)
        assertEquals(Argb.rgb(19), list.last())
        assertEquals(list, ColorMemory.addCustom(list, Argb.rgb(19)))
    }

    @Test
    fun recentColorsMoveToFront() {
        val l = ColorMemory.pushRecent(listOf(RED, BLUE), BLUE)
        assertEquals(listOf(BLUE, RED), l)
    }

    @Test
    fun hsvRoundTrip() {
        for (c in Palettes.Bright.colors) {
            val (h, s, v) = Argb.toHsv(c).toList()
            val back = Argb.fromHsv(h, s, v)
            assertTrue(Argb.distanceSq(c, back) <= 3, "color ${c.toString(16)}")
        }
    }

    @Test
    fun stampsAreValidAndUnique() {
        assertTrue(Stamps.all.size >= 8)
        assertEquals(Stamps.all.size, Stamps.all.map { it.id }.distinct().size)
        Stamps.all.forEach { assertFalse(it.pixels.isEmpty(), it.id) }
    }

    @Test
    fun coloringPagesHaveClosedRegions() {
        for (id in ColoringPages.ids) {
            val outline = ColoringPages.outline(id)
            assertFalse(outline.isEmpty())
            val outside = FloodFill.region(outline.toIntArray(), outline.width, outline.height, 0, 0).size
            assertTrue(outside < outline.size / 2 + outline.size / 4, "$id: outline encloses something")
        }
    }

    @Test
    fun challengeMatching() {
        val ch = Challenges.byId("heart")!!
        val d = Drawing.blank(ch.target.width, ch.target.height, "L")
        assertFalse(Challenges.isMatched(d, 0, ch.target))
        val done = d.withCel(0, 1, ch.target)
        assertTrue(Challenges.isMatched(done, 0, ch.target))
        assertEquals(1f, Challenges.progress(done, 0, ch.target))
        assertTrue(ch.palette.isNotEmpty())
    }

    @Test
    fun pixelateKeepsAspectAndSnapsToPalette() {
        val src = IntArray(40 * 20) { if (it % 40 < 20) RED else BLUE }
        val out = Pixelate.toCanvas(src, 40, 20, 8, 8, listOf(RED, BLUE, Argb.WHITE))
        assertEquals(0, out[0])
        assertEquals(RED, out[2 * 8 + 0])
        assertEquals(BLUE, out[2 * 8 + 7])
        assertTrue(out.all { it == 0 || it == RED || it == BLUE })
    }

    @Test
    fun shiftedDropsOffCanvasPixels() {
        val b = PixelBuffer.of(2, 1, intArrayOf(RED, BLUE))
        assertEquals(listOf(0, RED), b.shifted(1, 0).toIntArray().toList())
    }
}
