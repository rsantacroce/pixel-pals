package app.pixelpals.io

import app.pixelpals.model.Argb
import app.pixelpals.model.Drawing

/** Renders drawings to crisp, nearest-neighbor-scaled image files. */
public object PngExport {
    /** Offered export scales; 8× is the default so pictures are not tiny. */
    public val SCALES: List<Int> = listOf(1, 4, 8, 16)

    /** Default export scale. */
    public const val DEFAULT_SCALE: Int = 8

    /** Output pixel size of an export at [scale]. */
    public fun outputSize(drawing: Drawing, scale: Int): Pair<Int, Int> = drawing.width * scale to drawing.height * scale

    /** PNG of frame [frameIndex] at integer [scale], optionally on a white background. */
    public fun render(drawing: Drawing, frameIndex: Int, scale: Int, whiteBackground: Boolean): ByteArray {
        require(scale >= 1)
        val src = drawing.composite(frameIndex).also { if (whiteBackground) onWhite(it) }
        val (w, h) = outputSize(drawing, scale)
        return PngEncoder.encode(w, h) { y, out ->
            val sy = y / scale
            for (x in 0 until w) out[x] = src[sy * drawing.width + x / scale]
        }
    }

    /** All frames side by side in one PNG (a sprite sheet). */
    public fun spriteSheet(drawing: Drawing, scale: Int, whiteBackground: Boolean): ByteArray {
        val frames = drawing.frames.indices.map { i -> drawing.composite(i).also { if (whiteBackground) onWhite(it) } }
        val fw = drawing.width * scale
        return PngEncoder.encode(fw * frames.size, drawing.height * scale) { y, out ->
            val sy = y / scale
            for (x in out.indices) {
                val f = frames[x / fw]
                out[x] = f[sy * drawing.width + (x % fw) / scale]
            }
        }
    }

    /** Animated GIF of every frame at [scale] and the drawing's fps. */
    public fun gif(drawing: Drawing, scale: Int, whiteBackground: Boolean): ByteArray {
        val w = drawing.width * scale
        val h = drawing.height * scale
        val frames =
            drawing.frames.indices.map { i ->
                val src = drawing.composite(i).also { if (whiteBackground) onWhite(it) }
                IntArray(w * h) { p -> src[(p / w / scale) * drawing.width + (p % w) / scale] }
            }
        return GifEncoder.encode(w, h, frames, 1000 / drawing.fps)
    }

    private fun onWhite(px: IntArray) {
        for (i in px.indices) px[i] = Argb.over(px[i], Argb.WHITE)
    }
}
