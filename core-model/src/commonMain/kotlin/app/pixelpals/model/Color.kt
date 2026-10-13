package app.pixelpals.model

import kotlin.math.abs
import kotlin.math.roundToInt

/** Helpers for colors stored as straight (non-premultiplied) ARGB `Int`s. */
public object Argb {
    /** Fully transparent. Index value 0 in every pixel buffer. */
    public const val TRANSPARENT: Int = 0

    /** Opaque black. */
    public const val BLACK: Int = 0xFF000000.toInt()

    /** Opaque white. */
    public const val WHITE: Int = 0xFFFFFFFF.toInt()

    /** Builds a color from 0..255 channel values. */
    public fun of(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** Builds an opaque color from a 0xRRGGBB value. */
    public fun rgb(rgb: Int): Int = rgb or 0xFF000000.toInt()

    /** Alpha channel 0..255. */
    public fun alpha(c: Int): Int = (c ushr 24) and 0xFF

    /** Red channel 0..255. */
    public fun red(c: Int): Int = (c shr 16) and 0xFF

    /** Green channel 0..255. */
    public fun green(c: Int): Int = (c shr 8) and 0xFF

    /** Blue channel 0..255. */
    public fun blue(c: Int): Int = c and 0xFF

    /** Composites [top] over [bottom] using straight alpha ("source over"). */
    public fun over(top: Int, bottom: Int): Int {
        val ta = alpha(top)
        if (ta == 255) return top
        if (ta == 0) return bottom
        val ba = alpha(bottom)
        if (ba == 0) return top
        val outA = ta + ba * (255 - ta) / 255
        if (outA == 0) return TRANSPARENT

        fun ch(t: Int, b: Int) = (t * ta + b * ba * (255 - ta) / 255) / outA
        return of(outA, ch(red(top), red(bottom)), ch(green(top), green(bottom)), ch(blue(top), blue(bottom)))
    }

    /** Squared RGB distance between two colors, ignoring alpha. */
    public fun distanceSq(a: Int, b: Int): Int {
        val dr = red(a) - red(b)
        val dg = green(a) - green(b)
        val db = blue(a) - blue(b)
        return dr * dr + dg * dg + db * db
    }

    /** Returns the entry of [palette] closest to [color]; [color] itself when the palette is empty. */
    public fun nearest(color: Int, palette: List<Int>): Int = palette.minByOrNull { distanceSq(color, it) } ?: color

    /**
     * Converts HSV to an opaque color.
     * @param hue degrees 0..360, @param saturation 0..1, @param value 0..1.
     */
    public fun fromHsv(hue: Float, saturation: Float, value: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f / 60f
        val c = value * saturation
        val x = c * (1 - abs(h % 2 - 1))
        val m = value - c
        val (r, g, b) =
            when (h.toInt()) {
                0 -> Triple(c, x, 0f)
                1 -> Triple(x, c, 0f)
                2 -> Triple(0f, c, x)
                3 -> Triple(0f, x, c)
                4 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
        return of(255, ((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    /** Converts a color to `[hue 0..360, saturation 0..1, value 0..1]`. */
    public fun toHsv(color: Int): FloatArray {
        val r = red(color) / 255f
        val g = green(color) / 255f
        val b = blue(color) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        val h =
            when {
                d == 0f -> 0f
                max == r -> 60f * (((g - b) / d) % 6f)
                max == g -> 60f * (((b - r) / d) + 2f)
                else -> 60f * (((r - g) / d) + 4f)
            }
        return floatArrayOf((h + 360f) % 360f, if (max == 0f) 0f else d / max, max)
    }
}
