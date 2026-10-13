package app.pixelpals.io

import app.pixelpals.model.PixelBuffer
import kotlin.io.encoding.Base64

/**
 * Run-length encoding for pixel buffers: a sequence of `(varint runLength, 4-byte big-endian ARGB)`
 * records, Base64-encoded for JSON. Pixel art compresses extremely well this way.
 */
public object PixelCodec {
    /** Encodes [pixels] to a Base64 RLE string. */
    public fun encode(pixels: IntArray): String {
        val out = ByteSink()
        var i = 0
        while (i < pixels.size) {
            val c = pixels[i]
            var run = 1
            while (i + run < pixels.size && pixels[i + run] == c) run++
            out.varint(run)
            out.int(c)
            i += run
        }
        return Base64.encode(out.toByteArray())
    }

    /** Encodes a buffer. */
    public fun encode(buffer: PixelBuffer): String = encode(buffer.toIntArray())

    /** Decodes exactly [count] pixels; throws [IllegalArgumentException] on malformed input. */
    public fun decode(text: String, count: Int): IntArray {
        val bytes = Base64.decode(text)
        val out = IntArray(count)
        var pos = 0
        var o = 0
        while (pos < bytes.size) {
            var run = 0
            var shift = 0
            while (true) {
                require(pos < bytes.size && shift <= 28) { "Bad run length" }
                val b = bytes[pos++].toInt() and 0xFF
                run = run or ((b and 0x7F) shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            require(pos + 4 <= bytes.size) { "Truncated color" }
            val c =
                ((bytes[pos].toInt() and 0xFF) shl 24) or ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[pos + 2].toInt() and 0xFF) shl 8) or (bytes[pos + 3].toInt() and 0xFF)
            pos += 4
            require(run > 0 && o + run <= count) { "Too many pixels" }
            out.fill(c, o, o + run)
            o += run
        }
        require(o == count) { "Expected $count pixels, got $o" }
        return out
    }
}

/** Growable byte buffer. */
internal class ByteSink(capacity: Int = 256) {
    private var data = ByteArray(capacity)
    var size: Int = 0
        private set

    private fun ensure(extra: Int) {
        if (size + extra > data.size) data = data.copyOf(maxOf(data.size * 2, size + extra))
    }

    fun byte(v: Int) {
        ensure(1)
        data[size++] = v.toByte()
    }

    fun bytes(b: ByteArray, off: Int = 0, len: Int = b.size) {
        ensure(len)
        b.copyInto(data, size, off, off + len)
        size += len
    }

    fun int(v: Int) {
        byte(v ushr 24)
        byte(v ushr 16)
        byte(v ushr 8)
        byte(v)
    }

    fun shortLE(v: Int) {
        byte(v)
        byte(v ushr 8)
    }

    fun varint(value: Int) {
        var v = value
        while (v >= 0x80) {
            byte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        byte(v)
    }

    fun ascii(s: String) = s.forEach { byte(it.code) }

    fun toByteArray(): ByteArray = data.copyOf(size)
}
