package app.pixelpals.model

/** Every drawing tool. */
public enum class Tool {
    PENCIL,
    ERASER,
    FILL,
    PICKER,
    LINE,
    RECTANGLE,
    ELLIPSE,
    MOVE,
    STAMP,
    DITHER,
    REPLACE,
    ;

    /** True for tools whose result depends on the start and current pointer (live preview). */
    public val isShape: Boolean get() = this == LINE || this == RECTANGLE || this == ELLIPSE

    /** True for tools that use the brush size. */
    public val usesBrush: Boolean get() = this == PENCIL || this == ERASER || this == DITHER || this == LINE
}

/** Current tool configuration. */
public data class ToolSettings(
    val tool: Tool = Tool.PENCIL,
    /** Brush size 1..3. */
    val brushSize: Int = 1,
    /** Rectangles and ellipses are filled when true. */
    val shapeFilled: Boolean = false,
    val mirror: MirrorMode = MirrorMode(),
    val stampId: String = Stamps.all.first().id,
    /** Tool to return to after a one-shot pick with the eyedropper. */
    val previousTool: Tool = Tool.PENCIL,
) {
    public companion object {
        /** Allowed brush sizes. */
        public val BRUSH_SIZES: IntRange = 1..3
    }
}

/** Shifts [source] by ([dx], [dy]) pixels; pixels moved off the canvas are dropped. */
public fun PixelBuffer.shifted(dx: Int, dy: Int): PixelBuffer {
    if (dx == 0 && dy == 0) return this
    val out = IntArray(size)
    for (y in 0 until height) {
        val sy = y - dy
        if (sy !in 0 until height) continue
        for (x in 0 until width) {
            val sx = x - dx
            if (sx in 0 until width) out[y * width + x] = this[sy * width + sx]
        }
    }
    return PixelBuffer.wrap(width, height, out)
}
