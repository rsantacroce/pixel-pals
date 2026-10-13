package app.pixelpals.model

/** Built-in line-art templates: a locked outline layer above an empty coloring layer. */
public object ColoringPages {
    /** Template canvas size. */
    public const val SIZE: Int = 32

    private const val INK: Int = 0xFF222034.toInt()

    /** Template ids in display order. */
    public val ids: List<String> = listOf("house", "fish", "dinosaur", "robot", "flower")

    /** Outline pixels for template [id]. */
    public fun outline(id: String): PixelBuffer {
        val pts =
            when (id) {
                "house" -> house()
                "fish" -> fish()
                "dinosaur" -> dinosaur()
                "robot" -> robot()
                else -> flower()
            }
        val px = IntArray(SIZE * SIZE)
        for (p in pts) if (p.x in 0 until SIZE && p.y in 0 until SIZE) px[p.y * SIZE + p.x] = INK
        return PixelBuffer.wrap(SIZE, SIZE, px)
    }

    /**
     * Builds a drawing for template [id]: a coloring layer named [colorLayerName] below
     * a locked outline layer named [outlineLayerName]. The coloring layer is selected by default.
     */
    public fun drawing(id: String, colorLayerName: String, outlineLayerName: String): Drawing {
        val blank = PixelBuffer.blank(SIZE, SIZE)
        return Drawing(
            width = SIZE,
            height = SIZE,
            layers = listOf(LayerInfo(1, colorLayerName), LayerInfo(2, outlineLayerName, locked = true)),
            frames = listOf(Frame(3, mapOf(1 to blank, 2 to outline(id)))),
            nextId = 4,
        )
    }

    private fun rect(x0: Int, y0: Int, x1: Int, y1: Int) = Raster.rectangle(x0, y0, x1, y1, filled = false)

    private fun oval(x0: Int, y0: Int, x1: Int, y1: Int) = Raster.ellipse(x0, y0, x1, y1, filled = false)

    private fun poly(vararg xy: Int): List<Point> =
        (0 until xy.size / 2 - 1).flatMap { i -> Raster.line(xy[2 * i], xy[2 * i + 1], xy[2 * i + 2], xy[2 * i + 3]) }

    private fun house() =
        rect(6, 15, 25, 29) + poly(3, 16, 15, 4, 16, 4, 28, 16, 3, 16) +
            rect(20, 6, 23, 11) + rect(13, 21, 18, 29) + rect(8, 18, 11, 21) + rect(20, 18, 23, 21) + rect(0, 30, 31, 31)

    private fun fish() =
        oval(4, 9, 22, 23) + poly(22, 16, 29, 9, 29, 23, 22, 16) +
            oval(8, 13, 11, 16) + poly(12, 9, 16, 5, 19, 10) + poly(14, 19, 13, 21, 17, 21) +
            oval(25, 2, 27, 4) + oval(28, 5, 29, 6)

    private fun dinosaur() =
        oval(5, 14, 21, 25) + poly(17, 16, 20, 7, 22, 4, 28, 4, 30, 7, 29, 10, 23, 10, 21, 18) +
            oval(24, 5, 25, 6) + poly(6, 20, 0, 27, 4, 27, 8, 24) + rect(8, 23, 11, 29) + rect(15, 23, 18, 29) +
            poly(7, 15, 8, 12, 10, 14, 12, 11, 14, 14, 16, 12, 17, 15)

    private fun robot() =
        rect(10, 4, 21, 12) + rect(12, 6, 14, 8) + rect(17, 6, 19, 8) + rect(13, 10, 18, 10) +
            poly(15, 4, 15, 1) + rect(14, 0, 16, 1) + rect(8, 14, 23, 25) + rect(12, 17, 19, 21) +
            rect(4, 15, 7, 23) + rect(24, 15, 27, 23) + rect(10, 26, 13, 30) + rect(18, 26, 21, 30) + poly(13, 12, 13, 14) +
            poly(18, 12, 18, 14)

    private fun flower() =
        oval(12, 7, 19, 14) + oval(12, 1, 19, 7) + oval(12, 14, 19, 20) + oval(6, 7, 12, 14) +
            oval(19, 7, 25, 14) + poly(15, 20, 15, 31) + oval(7, 22, 14, 26) + oval(16, 24, 23, 28) + rect(0, 31, 31, 31)
}
