package app.pixelpals.io

/** Minimal PNG writer: 8-bit RGBA, no filtering, streamed row by row through [ZlibCompressor]. */
public object PngEncoder {
    private val SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)

    /**
     * Encodes a `width × height` image whose rows are produced by [row]: it receives the row index
     * and must fill the provided `IntArray(width)` with straight ARGB colors.
     */
    public fun encode(width: Int, height: Int, row: (y: Int, out: IntArray) -> Unit): ByteArray {
        require(width > 0 && height > 0)
        val out = ByteSink(1024)
        out.bytes(SIGNATURE)
        val ihdr =
            ByteSink(13).apply {
                int(width)
                int(height)
                byte(8) // bit depth
                byte(6) // color type RGBA
                byte(0)
                byte(0)
                byte(0)
            }
        chunk(out, "IHDR", ihdr.toByteArray())
        val z = ZlibCompressor()
        val argb = IntArray(width)
        val line = ByteArray(1 + width * 4)
        for (y in 0 until height) {
            row(y, argb)
            line[0] = 0
            var p = 1
            for (c in argb) {
                line[p++] = (c shr 16).toByte()
                line[p++] = (c shr 8).toByte()
                line[p++] = c.toByte()
                line[p++] = (c ushr 24).toByte()
            }
            z.write(line)
        }
        chunk(out, "IDAT", z.finish())
        chunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    /** Encodes a plain ARGB array. */
    public fun encode(width: Int, height: Int, argb: IntArray): ByteArray =
        encode(width, height) { y, out -> argb.copyInto(out, 0, y * width, y * width + width) }

    private fun chunk(out: ByteSink, type: String, data: ByteArray) {
        out.int(data.size)
        val typed = type.encodeToByteArray() + data
        out.bytes(typed)
        out.int(Crc32.of(typed))
    }
}

/** CRC-32 (IEEE) as used by PNG. */
internal object Crc32 {
    private val table =
        IntArray(256) { n ->
            var c = n
            repeat(8) { c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else c ushr 1 }
            c
        }

    fun of(bytes: ByteArray): Int {
        var c = -1
        for (b in bytes) c = table[(c xor b.toInt()) and 0xFF] xor (c ushr 8)
        return c.inv()
    }
}
