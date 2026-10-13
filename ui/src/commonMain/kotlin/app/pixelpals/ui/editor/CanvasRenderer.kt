package app.pixelpals.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import app.pixelpals.model.Changed
import app.pixelpals.model.Drawing
import app.pixelpals.model.PixelBuffer

/**
 * Keeps an [ImageBitmap] in sync with a drawing's composite, repainting only the pixels a reduction
 * reports as changed. Full repaints draw one rectangle per horizontal run of equal colors.
 * [version] changes after every update so Compose redraws.
 */
class CanvasRenderer(val width: Int, val height: Int) {
    val image: ImageBitmap = ImageBitmap(width, height)
    private val canvas = Canvas(image)
    private val paint =
        Paint().apply {
            isAntiAlias = false
            blendMode = BlendMode.Src
        }

    var version: Int by mutableIntStateOf(0)
        private set

    /** Repaints according to [changed] for frame [frameIndex] of [drawing]. */
    fun update(drawing: Drawing, frameIndex: Int, changed: Changed) {
        when (changed) {
            Changed.None -> return
            Changed.All -> paintAll(drawing.composite(frameIndex))
            is Changed.Pixels -> {
                val buffers = drawing.visibleBuffers(frameIndex)
                for (i in changed.indices) {
                    paintRun(i % width, i / width, 1, Drawing.compositeAt(buffers, i))
                }
            }
        }
        version++
    }

    /** Repaints everything from an ARGB array. */
    fun paintAll(pixels: IntArray) {
        for (y in 0 until height) {
            var x = 0
            while (x < width) {
                val c = pixels[y * width + x]
                var run = 1
                while (x + run < width && pixels[y * width + x + run] == c) run++
                paintRun(x, y, run, c)
                x += run
            }
        }
        version++
    }

    private fun paintRun(x: Int, y: Int, length: Int, argb: Int) {
        paint.color = Color(argb)
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + length).toFloat(), (y + 1).toFloat(), paint)
    }

    companion object {
        /** A one-off bitmap of [buffer], for thumbnails. */
        fun bitmapOf(buffer: PixelBuffer): ImageBitmap =
            CanvasRenderer(buffer.width, buffer.height).also { it.paintAll(buffer.toIntArray()) }.image

        /** A one-off bitmap of raw pixels. */
        fun bitmapOf(width: Int, height: Int, pixels: IntArray): ImageBitmap =
            CanvasRenderer(width, height).also { it.paintAll(pixels) }.image
    }
}
