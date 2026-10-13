package app.pixelpals.model

/** Turns a photo into canvas-sized pixel art. */
public object Pixelate {
    /**
     * Downscales [source] (`srcWidth × srcHeight` ARGB) to fit inside `width × height`, keeping the aspect ratio
     * and centering it on a transparent background. Each output pixel is the average of its source box,
     * then snapped to the nearest [palette] color when a palette is given. Mostly-transparent boxes stay transparent.
     */
    public fun toCanvas(source: IntArray, srcWidth: Int, srcHeight: Int, width: Int, height: Int, palette: List<Int>?): IntArray {
        require(source.size == srcWidth * srcHeight) { "Source size mismatch" }
        val out = IntArray(width * height)
        if (srcWidth == 0 || srcHeight == 0) return out
        val scale = minOf(width.toDouble() / srcWidth, height.toDouble() / srcHeight)
        val dw = (srcWidth * scale).toInt().coerceIn(1, width)
        val dh = (srcHeight * scale).toInt().coerceIn(1, height)
        val ox = (width - dw) / 2
        val oy = (height - dh) / 2
        for (y in 0 until dh) {
            val sy0 = y * srcHeight / dh
            val sy1 = maxOf(sy0 + 1, (y + 1) * srcHeight / dh)
            for (x in 0 until dw) {
                val sx0 = x * srcWidth / dw
                val sx1 = maxOf(sx0 + 1, (x + 1) * srcWidth / dw)
                var a = 0L
                var r = 0L
                var g = 0L
                var b = 0L
                var n = 0
                for (sy in sy0 until sy1) {
                    for (sx in sx0 until sx1) {
                        val c = source[sy * srcWidth + sx]
                        val ca = Argb.alpha(c)
                        a += ca
                        r += Argb.red(c) * ca
                        g += Argb.green(c) * ca
                        b += Argb.blue(c) * ca
                        n++
                    }
                }
                if (a * 2 < 255L * n) continue
                val avg = Argb.of(255, (r / a).toInt(), (g / a).toInt(), (b / a).toInt())
                out[(oy + y) * width + ox + x] = if (palette.isNullOrEmpty()) avg else Argb.nearest(avg, palette)
            }
        }
        return out
    }
}
