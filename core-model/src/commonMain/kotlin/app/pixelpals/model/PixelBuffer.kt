package app.pixelpals.model

/**
 * An immutable `width × height` grid of ARGB pixels stored row-major in a flat [IntArray].
 * Index `0` (the value, not the position) means fully transparent.
 *
 * Mutating operations return a new buffer; the original is never changed, so buffers can be
 * shared freely between drawing snapshots and undo history.
 */
public class PixelBuffer private constructor(
    public val width: Int,
    public val height: Int,
    private val data: IntArray,
) {
    init {
        require(width > 0 && height > 0) { "Size must be positive" }
        require(data.size == width * height) { "Pixel count does not match size" }
    }

    /** Number of pixels. */
    public val size: Int get() = data.size

    /** Color at linear [index]. */
    public operator fun get(index: Int): Int = data[index]

    /** Color at ([x], [y]); [Argb.TRANSPARENT] when out of bounds. */
    public fun at(x: Int, y: Int): Int = if (contains(x, y)) data[y * width + x] else Argb.TRANSPARENT

    /** True when ([x], [y]) lies inside the buffer. */
    public fun contains(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    /** Returns a defensive copy of the pixels. */
    public fun toIntArray(): IntArray = data.copyOf()

    /** True when every pixel is transparent. */
    public fun isEmpty(): Boolean = data.all { it == Argb.TRANSPARENT }

    /** Returns a new buffer with `values[i]` written at `indices[i]`. */
    public fun withChanges(indices: IntArray, values: IntArray): PixelBuffer {
        if (indices.isEmpty()) return this
        val copy = data.copyOf()
        for (i in indices.indices) copy[indices[i]] = values[i]
        return PixelBuffer(width, height, copy)
    }

    /** Returns a new buffer produced by editing a copy of the pixels in [block]. */
    public inline fun edit(block: (IntArray) -> Unit): PixelBuffer {
        val copy = toIntArray()
        block(copy)
        return wrap(width, height, copy)
    }

    override fun equals(other: Any?): Boolean =
        other is PixelBuffer && other.width == width && other.height == height && other.data.contentEquals(data)

    override fun hashCode(): Int = 31 * (31 * width + height) + data.contentHashCode()

    override fun toString(): String = "PixelBuffer(${width}x$height)"

    public companion object {
        /** A fully transparent buffer. */
        public fun blank(width: Int, height: Int): PixelBuffer = PixelBuffer(width, height, IntArray(width * height))

        /** A buffer holding a copy of [pixels]. */
        public fun of(width: Int, height: Int, pixels: IntArray): PixelBuffer = PixelBuffer(width, height, pixels.copyOf())

        /**
         * Wraps [pixels] without copying. The caller must not modify the array afterwards.
         */
        public fun wrap(width: Int, height: Int, pixels: IntArray): PixelBuffer = PixelBuffer(width, height, pixels)
    }
}
