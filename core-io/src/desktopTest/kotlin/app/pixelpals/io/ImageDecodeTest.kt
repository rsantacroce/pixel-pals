package app.pixelpals.io

import app.pixelpals.model.Argb
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Verifies our encoders against an independent decoder (javax.imageio). */
class ImageDecodeTest {
    @Test
    fun pngDecodesToSamePixels() {
        val d = sampleDrawing()
        val png = PngExport.render(d, 0, 4, whiteBackground = false)
        val img = ImageIO.read(ByteArrayInputStream(png))
        assertEquals(d.width * 4, img.width)
        val src = d.composite(0)
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val expected = src[(y / 4) * d.width + x / 4]
                assertEquals(expected, img.getRGB(x, y).let { if (Argb.alpha(it) == 0) 0 else it }, "($x,$y)")
            }
        }
        val decoded = assertNotNull(decodeImage(png))
        assertEquals(img.height, decoded.height)
    }

    @Test
    fun gifDecodesAllFramesExactly() {
        val rnd = Random(7)
        val colors = List(300) { Argb.rgb(rnd.nextInt(0xFFFFFF)) }
        // Noisy frames with many colors stress the LZW code-size transitions and table resets.
        val frames = List(3) { f -> IntArray(120 * 90) { i -> if ((i + f) % 11 == 0) 0 else colors[(i * 31 + f * 7) % 200] } }
        val gif = GifEncoder.encode(120, 90, frames, 100)
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.input = ImageIO.createImageInputStream(ByteArrayInputStream(gif))
        assertEquals(3, reader.getNumImages(true))
        for (f in 0 until 3) {
            val img = reader.read(f)
            val px = img.getRGB(0, 0, 120, 90, null, 0, 120).map { if (Argb.alpha(it) == 0) 0 else it }.toIntArray()
            assertContentEquals(frames[f], px, "frame $f")
        }
    }

    @Test
    fun jvmFileStoreIsAtomicAndListsOnlyFiles() {
        val dir = Files.createTempDirectory("pp").toFile()
        val fs = JvmFileStore(dir)
        fs.writeAtomic("projects/a.pixelpals", "1".encodeToByteArray())
        fs.writeAtomic("projects/a.pixelpals", "2".encodeToByteArray())
        assertEquals("2", fs.read("projects/a.pixelpals")!!.decodeToString())
        assertEquals(listOf("a.pixelpals"), fs.list("projects"))
        fs.move("projects/a.pixelpals", "trash/a.pixelpals")
        assertEquals(listOf("a.pixelpals"), fs.list("trash"))
        assertEquals(emptyList(), fs.list("nope"))
        File(dir, "x").deleteRecursively()
        dir.deleteRecursively()
    }
}
