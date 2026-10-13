package app.pixelpals.model

/**
 * Symmetry settings. [horizontal] mirrors left↔right across the vertical center axis;
 * [vertical] mirrors top↕bottom across the horizontal center axis.
 */
public data class MirrorMode(val horizontal: Boolean = false, val vertical: Boolean = false) {
    /** True when any symmetry is active. */
    val enabled: Boolean get() = horizontal || vertical

    /** The point itself plus its mirror images on a `width × height` canvas (deduplicated). */
    public fun map(p: Point, width: Int, height: Int): List<Point> {
        if (!enabled) return listOf(p)
        val mx = width - 1 - p.x
        val my = height - 1 - p.y
        val out = LinkedHashSet<Point>(4)
        out += p
        if (horizontal) out += Point(mx, p.y)
        if (vertical) out += Point(p.x, my)
        if (horizontal && vertical) out += Point(mx, my)
        return out.toList()
    }

    /** Applies [map] to every point. */
    public fun mapAll(points: Iterable<Point>, width: Int, height: Int): List<Point> {
        if (!enabled) return points.toList()
        val out = LinkedHashSet<Point>()
        for (p in points) out += map(p, width, height)
        return out.toList()
    }
}
