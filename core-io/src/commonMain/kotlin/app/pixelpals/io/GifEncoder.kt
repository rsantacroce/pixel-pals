package app.pixelpals.io

import app.pixelpals.model.Argb

/**
 * Small animated GIF89a encoder written for this app (no third-party code). Frames share one global
 * color table of up to 255 colors plus a transparent index 0. Pixels with alpha < 128 are transparent.
 */
public object GifEncoder {
    /**
     * Encodes [frames] (each `width × height` straight ARGB) looping forever, showing each frame for [delayMs].
     * When there are more than 255 distinct colors, the least frequent ones are mapped to their nearest kept color.
     */
    public fun encode(width: Int, height: Int, frames: List<IntArray>, delayMs: Int): ByteArray {
        require(frames.isNotEmpty() && width in 1..0xFFFF && height in 1..0xFFFF)
        val opaque = frames.map { f -> IntArray(f.size) { i -> if (Argb.alpha(f[i]) < 128) 0 else f[i] or 0xFF000000.toInt() } }
        val palette = buildPalette(opaque)
        val indexOf = HashMap<Int, Int>()
        palette.forEachIndexed { i, c -> indexOf[c] = i + 1 }
        val tableBits = bitsFor(palette.size + 1).coerceAtLeast(2)
        val tableSize = 1 shl tableBits

        val out = ByteSink(4096)
        out.ascii("GIF89a")
        out.shortLE(width)
        out.shortLE(height)
        out.byte(0x80 or ((tableBits - 1) shl 4) or (tableBits - 1)) // global table, color resolution, size
        out.byte(0) // background index (transparent)
        out.byte(0) // aspect
        for (i in 0 until tableSize) {
            val c = if (i in 1..palette.size) palette[i - 1] else 0
            out.byte(Argb.red(c))
            out.byte(Argb.green(c))
            out.byte(Argb.blue(c))
        }
        if (frames.size > 1) {
            // NETSCAPE2.0 application extension: loop forever.
            out.bytes(byteArrayOf(0x21, 0xFF.toByte(), 11))
            out.ascii("NETSCAPE2.0")
            out.bytes(byteArrayOf(3, 1, 0, 0, 0))
        }
        val delayCs = (delayMs / 10).coerceIn(2, 0xFFFF)
        for (frame in opaque) {
            out.bytes(byteArrayOf(0x21, 0xF9.toByte(), 4, (2 shl 2 or 1).toByte())) // dispose to background, transparent
            out.shortLE(delayCs)
            out.byte(0) // transparent index
            out.byte(0)
            out.byte(0x2C)
            out.shortLE(0)
            out.shortLE(0)
            out.shortLE(width)
            out.shortLE(height)
            out.byte(0) // no local table
            val indices =
                IntArray(frame.size) { i ->
                    val c = frame[i]
                    if (c == 0) 0 else indexOf[c] ?: (indexOf[Argb.nearest(c, palette)] ?: 0)
                }
            lzw(out, indices, tableBits)
        }
        out.byte(0x3B)
        return out.toByteArray()
    }

    private fun buildPalette(frames: List<IntArray>): List<Int> {
        val counts = HashMap<Int, Int>()
        for (f in frames) for (c in f) if (c != 0) counts[c] = (counts[c] ?: 0) + 1
        return counts.entries.sortedByDescending { it.value }.take(MAX_COLORS).map { it.key }
    }

    private fun bitsFor(n: Int): Int {
        var bits = 1
        while ((1 shl bits) < n) bits++
        return bits
    }

    /** GIF-flavored LZW with variable code size, written as data sub-blocks. */
    private fun lzw(out: ByteSink, indices: IntArray, minCodeSize: Int) {
        out.byte(minCodeSize)
        val clear = 1 shl minCodeSize
        val end = clear + 1
        val blocks = ByteSink(indices.size / 2 + 16)
        var bitBuf = 0
        var bitCount = 0
        var codeSize = minCodeSize + 1

        fun emit(code: Int) {
            bitBuf = bitBuf or (code shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                blocks.byte(bitBuf and 0xFF)
                bitBuf = bitBuf ushr 8
                bitCount -= 8
            }
        }
        val dict = HashMap<Int, Int>()
        var next = end + 1

        // Same timing as the classic compress/GIF encoders: widen after writing once the table reaches the limit.
        fun grow() {
            if (codeSize < 12 && next >= (1 shl codeSize)) codeSize++
        }
        emit(clear)
        var prefix = indices[0]
        for (i in 1 until indices.size) {
            val k = indices[i]
            val key = (prefix shl 8) or k
            val found = dict[key]
            if (found != null) {
                prefix = found
                continue
            }
            emit(prefix)
            grow()
            if (next < MAX_CODE) {
                dict[key] = next++
            } else {
                emit(clear)
                dict.clear()
                next = end + 1
                codeSize = minCodeSize + 1
            }
            prefix = k
        }
        emit(prefix)
        grow()
        emit(end)
        if (bitCount > 0) blocks.byte(bitBuf and 0xFF)
        val data = blocks.toByteArray()
        var p = 0
        while (p < data.size) {
            val n = minOf(255, data.size - p)
            out.byte(n)
            out.bytes(data, p, n)
            p += n
        }
        out.byte(0)
    }

    private const val MAX_COLORS = 255
    private const val MAX_CODE = 4096
}
