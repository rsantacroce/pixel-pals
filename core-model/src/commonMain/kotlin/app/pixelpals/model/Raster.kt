package app.pixelpals.model

import kotlin.math.abs

/** A pixel coordinate. */
public data class Point(val x: Int, val y: Int)

/** Pure rasterization helpers. All functions return points that may lie outside any canvas; callers clip. */
public object Raster {
    /** Bresenham line from ([x0], [y0]) to ([x1], [y1]), both ends included, without duplicates. */
    public fun line(x0: Int, y0: Int, x1: Int, y1: Int): List<Point> {
        val out = ArrayList<Point>(maxOf(abs(x1 - x0), abs(y1 - y0)) + 1)
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            out += Point(x, y)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x += sx
            }
            if (e2 <= dx) {
                err += dx
                y += sy
            }
        }
        return out
    }

    /** Rectangle spanning the two corners (in any order), outline or [filled]. */
    public fun rectangle(x0: Int, y0: Int, x1: Int, y1: Int, filled: Boolean): List<Point> {
        val left = minOf(x0, x1)
        val right = maxOf(x0, x1)
        val top = minOf(y0, y1)
        val bottom = maxOf(y0, y1)
        val out = ArrayList<Point>()
        for (y in top..bottom) {
            for (x in left..right) {
                if (filled || y == top || y == bottom || x == left || x == right) out += Point(x, y)
            }
        }
        return out
    }

    /**
     * Ellipse inscribed in the bounding box spanned by the two corners, outline or [filled].
     * Uses the integer midpoint algorithm, which handles even and odd box sizes.
     */
    public fun ellipse(x0: Int, y0: Int, x1: Int, y1: Int, filled: Boolean): List<Point> {
        var left = minOf(x0, x1)
        var right = maxOf(x0, x1)
        var top = minOf(y0, y1)
        var bottom = maxOf(y0, y1)
        // Ellipses two pixels thin or less are just their bounding box.
        if (right - left < 2 || bottom - top < 2) return rectangle(left, top, right, bottom, filled = true)
        val set = LinkedHashSet<Point>()

        fun plot(x: Int, y: Int) {
            set += Point(x, y)
        }

        fun span(xa: Int, xb: Int, y: Int) {
            for (x in xa..xb) plot(x, y)
        }
        // Algorithm by Alois Zingl ("A Rasterizing Algorithm for Drawing Curves").
        var a = (right - left).toLong()
        val b = (bottom - top).toLong()
        var b1 = b and 1L
        var dx = 4 * (1 - a) * b * b
        var dy = 4 * (b1 + 1) * a * a
        var err = dx + dy + b1 * a * a
        top += ((b + 1) / 2).toInt()
        bottom = top - b1.toInt()
        a *= 8 * a
        b1 = 8 * b * b
        do {
            if (filled) {
                span(left, right, top)
                span(left, right, bottom)
            } else {
                plot(right, top)
                plot(left, top)
                plot(left, bottom)
                plot(right, bottom)
            }
            val e2 = 2 * err
            if (e2 <= dy) {
                top++
                bottom--
                dy += a
                err += dy
            }
            if (e2 >= dx || 2 * err > dy) {
                left++
                right--
                dx += b1
                err += dx
            }
        } while (left <= right)
        while (top - bottom < b) {
            plot(left - 1, top)
            plot(right + 1, top)
            top++
            plot(left - 1, bottom)
            plot(right + 1, bottom)
            bottom--
        }
        return set.toList()
    }

    /**
     * Square brush footprint of [size] (1..3) at ([x], [y]).
     * Size 2 extends right/down; size 3 is centered.
     */
    public fun brush(x: Int, y: Int, size: Int): List<Point> =
        when (size) {
            1 -> listOf(Point(x, y))
            2 -> listOf(Point(x, y), Point(x + 1, y), Point(x, y + 1), Point(x + 1, y + 1))
            else -> buildList { for (dy in -1..1) for (dx in -1..1) add(Point(x + dx, y + dy)) }
        }
}
