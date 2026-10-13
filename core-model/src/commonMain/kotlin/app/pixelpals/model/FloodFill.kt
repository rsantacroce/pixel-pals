package app.pixelpals.model

/** Iterative 4-way scanline flood fill. Never recurses, so it is safe on the largest canvases. */
public object FloodFill {
    /**
     * Returns the linear indices of the region connected to ([x], [y]) whose color in [source]
     * equals the color at the start. [source] is a `width × height` row-major ARGB array.
     * Returns an empty array when the start is out of bounds.
     */
    public fun region(source: IntArray, width: Int, height: Int, x: Int, y: Int): IntArray {
        if (x !in 0 until width || y !in 0 until height) return IntArray(0)
        val target = source[y * width + x]
        val visited = BooleanArray(width * height)
        val result = IntList()
        val stack = IntList()
        stack.add(y * width + x)
        while (stack.size > 0) {
            val seed = stack.removeLast()
            if (visited[seed]) continue
            val sy = seed / width
            val row = sy * width
            var left = seed - row
            while (left > 0 && !visited[row + left - 1] && source[row + left - 1] == target) left--
            var right = seed - row
            while (right < width - 1 && !visited[row + right + 1] && source[row + right + 1] == target) right++
            for (sx in left..right) {
                val i = row + sx
                visited[i] = true
                result.add(i)
            }
            pushSpans(source, visited, stack, width, sy - 1, left, right, target, height)
            pushSpans(source, visited, stack, width, sy + 1, left, right, target, height)
        }
        return result.toIntArray()
    }

    private fun pushSpans(
        source: IntArray,
        visited: BooleanArray,
        stack: IntList,
        width: Int,
        y: Int,
        left: Int,
        right: Int,
        target: Int,
        height: Int,
    ) {
        if (y !in 0 until height) return
        val row = y * width
        var inSpan = false
        for (x in left..right) {
            val i = row + x
            val match = !visited[i] && source[i] == target
            if (match && !inSpan) stack.add(i)
            inSpan = match
        }
    }
}

/** Minimal growable int list to avoid boxing in hot loops. */
internal class IntList(capacity: Int = 64) {
    private var data = IntArray(capacity)
    var size: Int = 0
        private set

    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = v
    }

    fun removeLast(): Int = data[--size]

    fun toIntArray(): IntArray = data.copyOf(size)
}
